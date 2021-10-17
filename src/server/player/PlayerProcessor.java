package server.player;

import java.awt.Color;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import java.util.List;
import java.util.Queue;

import DataStructures.TcpMessage;
import DataStructures.ThreadedQueue;
import debug.DebugIO;
import server.ChatMessageColors;
import server.Server;
import server.Server.TCPDISCONNECTTYPE;
import server.ServerCore;
import server.ServerProcessor;
import server.connection.TcpPlayerWriter;
import server.leaderboard.LeaderboardEntry;
import server.leaderboard.LeaderboardState;
import server.player.PlayerServerState.LeaderboardRequest;
import server.player.data.PlayerRuntimeUdpData;
import server.player.data.PlayerSkin;
import server.util.BufferConverter;
import server.util.Util;
import server.util.Util.LeaderboardFindResult;

public class PlayerProcessor implements Runnable {

	ServerCore server;

	boolean run;

	// message which the client sends
	class TcpClientMessage {

	};

	// message which the server sends
	public class TcpServerMessage {
		public static final int LEADERBOARD = 120;
		public static final int NEW_MAPLOAD = 180;
		public static final int SET_MAPID = 182;
		public static final int ROUND_START = 183;
		public static final int ROUND_END = 184;
		public static final int PLAYER_INFO = 10;
		public static final int CHATMESSAGE = 200;
		public static final int UDP_CHECK_CONFIRM = 240;
		public static final int TCP_STILL_ALIVE = 210;
		public static final int SERVER_INFO = 1;
		public static final int PLAYER_VALIDATED_CONFIRMATION = 2;
		public static final int CLIENT_INFO_CONFIRM = 8;
		public static final int PASSWORD_CRYPT_DATA = 150;
		public static final int MAP_DATA_PACKET = 4;
		public static final int LAST_MAP_DATA_PACKET = 5;
		public static final int PLAYER_DISCONNECT = 11;
		public static final int ROUNDTIME_EXTEND = 20;
		public static final int LEADERBOARD_UPDATE = 121;
		public static final int SERVER_DISCONNECT = 250;// disconnect by the server (kick for example)
	};

	private static final String adminRequiredMessage = "Admin privileges required for this command!";

	ByteBuffer emptyBuffer;

	public PlayerProcessor(ServerCore server) {
		this.server = server;

		emptyBuffer = ByteBuffer.allocate(0);

		run = true;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (run == true) {

			Player[] playerArray = server.getPlayers().values().toArray(new Player[0]);

			for (int i = 0; i < playerArray.length; i++) {

				processPlayer(playerArray[i]);

			}

			try {
				Thread.sleep(1000 / ServerCore.TICKS_PER_SECOND);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	public void setRunning(boolean run) {
		this.run = run;
	}

	private void addLeaderboardEntryMessage(ByteBuffer message, int place, LeaderboardEntry entry) throws IOException {
		String playerName = entry.getPlayername();
		int playerID = entry.getPlayerID();

		BufferConverter.buffer_write_u8(message, place);// place
		BufferConverter.buffer_write_u8(message, playerID);// playerID
		BufferConverter.buffer_write_u8(message, playerName.length());// length (max 255)
		BufferConverter.buffer_write_chars_u8(message, playerName);// string

		BufferConverter.buffer_write_u8(message, entry.getBadge());

		if (entry.timeSet() == true) {
			BufferConverter.buffer_write_u8(message, 1);
			BufferConverter.buffer_write_u32(message, (int) entry.getTime());// time
			BufferConverter.buffer_write_u8(message, entry.getMedal());// medal

		} else {
			BufferConverter.buffer_write_u8(message, 0);
		}
	}

	public void processPlayer(Player player) {
		try {
			processTcpMessages(player);
			sendTcpStillAliveMessage(player);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			return;// breaks the execution
		}

		/**
		 * Process leaderboard request (if the client requested it)
		 */
		PlayerServerState.LeaderboardRequest leaderboardRequested = player.getPlayerServerData()
				.pollLeaderboardRequested();

		if (leaderboardRequested != null) {// if(player.getPlayerServerData().isLeaderboardRequested() == true){
			DebugIO.println("SENDING LEADERBOARD 1");
			if (player.getServer().getGameState().getHighscoreState().isSorted() == true) {
				DebugIO.println("SENDING LEADERBOARD 2");
				LeaderboardEntry[] leaderboard = player.getServer().getGameState().getHighscoreState()
						.getListArrayCopy();

				// player.getPlayerServerData().setLeaderboardRequest(false,0,0);

				int requestedPosition = leaderboardRequested.offset;
				LeaderboardFindResult individualEntry = Util.findPlayerEntry(leaderboard, player.getID());

				boolean additionalPlayerEntry = false;
				if (leaderboardRequested.addOwnEntry == true && individualEntry != null) {
					additionalPlayerEntry = true;
				}

				int iterateEntries = leaderboardRequested.size;// max 10 elements

				if (leaderboardRequested.requestType == 1) {// if requestType == 1 (end screen leaderboard) send the
															// leaderboard instead only the requested size.
					iterateEntries = leaderboard.length;
					requestedPosition = 0;
				}

				// first calculate total bytes of the message(is nessecary)
				int totalBytes = 4;

				int actualPlayerEntries = 0;// the value which holds the final amount of entries in the leaderboard list
											// (is calculated in the first for loop)

				for (int i = 0; i < iterateEntries; i++) {

					if (i + requestedPosition < leaderboard.length) {

						LeaderboardEntry entry = leaderboard[i + requestedPosition];

						totalBytes += 4;
						String playerName = entry.getPlayername();
						totalBytes += playerName.length();
						totalBytes++;
						if (entry.timeSet() == true) {
							totalBytes += 4 + 1;
						}

						actualPlayerEntries++;
					} else {
						break;
					}

				}

				if (additionalPlayerEntry == true) {// add an entry in the list with the data of the specific player

					totalBytes += 4;
					String playerName = individualEntry.getEntry().getPlayername();
					totalBytes += playerName.length();
					totalBytes++;
					if (individualEntry.getEntry().timeSet() == true) {
						totalBytes += 4 + 1;
					}
				}

				try {

					ByteBuffer message = ByteBuffer.allocate(totalBytes);
					message.position(0);

					BufferConverter.buffer_write_u8(message, leaderboardRequested.requestType);// LEADERBOARD TYPE

					BufferConverter.buffer_write_u8(message, leaderboard.length);// total number in leaderboard
					BufferConverter.buffer_write_u8(message, actualPlayerEntries + (additionalPlayerEntry ? 1 : 0));// number
																													// of
																													// player
																													// entries
					BufferConverter.buffer_write_u8(message, (additionalPlayerEntry ? 1 : 0));// is the last entry the
																								// players own record?
																								// (true/false)

					for (int i = 0; i < actualPlayerEntries; i++) {
						int place;
						LeaderboardEntry entry = null;

						entry = leaderboard[i + requestedPosition];
						place = i + 1;

						addLeaderboardEntryMessage(message, place, entry);
					}

					if (additionalPlayerEntry == true) {
						addLeaderboardEntryMessage(message, individualEntry.getPlacement() + 1,
								individualEntry.getEntry());
					}

					server.sendTcpMessageLocked(player, TcpServerMessage.LEADERBOARD, message);

				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					return;// breaks the execution
				}
			}
		}

		/**
		 * sends UDP confirmpacket via TCP
		 */
		if (player.getPlayerServerData().getUdpTestPacketRecieved() == true
				&& player.getPlayerServerData().getUdpConnectionEstablished() == true
				&& player.getPlayerServerData().getUdpConfirmPacketSend() == false) {

			// UDP CONFIRMATION PACKET
			ByteBuffer message = ByteBuffer.allocate(0);
			server.sendTcpMessageLocked(player, TcpServerMessage.UDP_CHECK_CONFIRM, message);
			DebugIO.println("UDP CONFIRM END");

			player.getPlayerServerData().setUdpConfirmPacketSend(true);
		}

		/**
		 * send map data to player (if requested)
		 */

		if (player.getPlayerServerData().getUdpTestPacketRecieved() == true
				&& player.getPlayerServerData().getUdpConnectionEstablished() == true
				&& player.getPlayerServerData().getUdpConfirmPacketSend() == true) {
			if (player.getPlayerServerData().requestedNewMapInit() == true) {

				// Now that the connection is established sent gameData to the server (NOTE: The
				// client will top the current session, start a loading screen and wait for the
				// mapdata from the server.)
				// send new gamemode data (initiates new map loading on the server!)

				ByteBuffer message = ByteBuffer.allocate(1);
				message.position(0);

				BufferConverter.buffer_write_u8(message, 1);// 0 = freeroam mode , 1 = time trial mode
				server.sendTcpMessageLocked(player, TcpServerMessage.NEW_MAPLOAD, message);

				// and now send mapData to the server once the map finished loading (currently
				// not used as originally intended, but it works.)
				message = ByteBuffer.allocate(2);
				BufferConverter.buffer_write_u16(message,
						player.getServer().getRessourceHandler().getMapHandler().getMapMagicID());// freeroam mode
				server.sendTcpMessageLocked(player, TcpServerMessage.SET_MAPID, message);
				// reset map transmitter
				player.getMapTransmitter().reset();
				player.getPlayerServerData().newMapInitDone();
			}

			if (player.getPlayerServerData().getPlayerReady()) {
				if (player.getPlayerServerData().getMapSended() == true) {

					// Send a "round started" message to tell the client that he can finally start
					// moving
					if (player.getPlayerServerData().getSendStartRoundMessage() == false
							&& player.getServer().getGameState().getRoundRunning() == true) {
						player.getPlayerServerData().setSendStartRoundMessage(true);

						ByteBuffer message = ByteBuffer.allocate(4);
						BufferConverter.buffer_write_u32(message, server.getGameState().getRemainingRoundTimeTicks());
						server.sendTcpMessageLocked(player, TcpServerMessage.ROUND_START, message);
					}

					// Round ended.
					if (player.getPlayerServerData().getSendEndRoundMessage() == false
							&& player.getServer().getGameState().getEndRoundTimerStarted()) {

						player.getPlayerServerData().setSendEndRoundMessage(true);

						ByteBuffer message = ByteBuffer.allocate(0);
						server.sendTcpMessageLocked(player, TcpServerMessage.ROUND_END, message);
					}
				}
			}

		}

		/**
		 * Check if the player requested information about another player and send it to
		 * him (if nessecary)
		 */

		// TODO: nicer playercheck

		if (player.getRequestedPlayerIDs().isEmptyConcurrent() == false) {

			// returns queue and clears internal one to avoid locks.
			Queue<Integer> requestedPlayerIDs = player.getRequestedPlayerIDs().returnAndClearConcurrent();

			while (requestedPlayerIDs.isEmpty() == false) {
				int requestedID = requestedPlayerIDs.poll();
				if (player.getServer().getPlayers().containsKey(requestedID)) {
					if (player.getServer().getPlayers().get(requestedID).getPlayerServerData().isSigned() == true) {

						Player p = player.getServer().getPlayers().get(requestedID);
						PlayerSkin ps = p.getPlayerSkinCopy();
						PlayerRuntimeUdpData data = p.getPlayerDataCopy();

						String name = p.getPlayerProfileData().getPlayerName();

						int namebytes = 1 + (name.length() * 2);// +1 as a leading byte is used to tell the client how
																// many characters of the name are following

						ByteBuffer message = ByteBuffer.allocate(2 + namebytes + 9 + 1);

						BufferConverter.buffer_write_u8(message, requestedID);
						BufferConverter.buffer_write_u8(message, 1);// 1= player exists

						BufferConverter.buffer_write_u8(message, name.length());// 1= player exists
						BufferConverter.buffer_write_chars_u16(message, name);

						BufferConverter.buffer_write_u8(message, ps.skinID);

						BufferConverter.buffer_write_u8(message, ps.skin_R);
						BufferConverter.buffer_write_u8(message, ps.skin_G);
						BufferConverter.buffer_write_u8(message, ps.skin_B);
						BufferConverter.buffer_write_u8(message, ps.armor_R);
						BufferConverter.buffer_write_u8(message, ps.armor_G);
						BufferConverter.buffer_write_u8(message, ps.armor_B);
						BufferConverter.buffer_write_u8(message, ps.eye_R);
						BufferConverter.buffer_write_u8(message, ps.eye_G);
						BufferConverter.buffer_write_u8(message, ps.eye_B);

						server.sendTcpMessageLocked(player, TcpServerMessage.PLAYER_INFO, message);

						DebugIO.println("SEND PLAYER INFO FROM " + requestedID + " to " + player.getID());

					}
				} else {
					// send information that player doesn't exist

					ByteBuffer message = ByteBuffer.allocate(2);

					// TODO: Send player information, lice name, color, etc...
					BufferConverter.buffer_write_u8(message, requestedID);
					BufferConverter.buffer_write_u8(message, 0);// 0 = player doesnt exist

					server.sendTcpMessageLocked(player, TcpServerMessage.PLAYER_INFO, message);
				}
			}
		}

		/**
		 * check UDP timeout (and disconnect if nessecary)
		 */

		// check udp timeout
		if (System.nanoTime() - player.getTimings()
				.getLastUdpTime() > (player.getServer().getServerConfig().getUdpTimeout() * 1000000000L)) {
			// UDP TIMEOUT!
			player.closePlayer(TCPDISCONNECTTYPE.UDPTIMEOUT);

			// UDP still alive packet is handled by the udp broadcaster!
		} else {

			if (System.nanoTime() - player.getTimings()
					.getLastTcpTime() > (player.getServer().getServerConfig().getTcpTimeout() * 1000000000L)) {
				// UDP TIMEOUT!
				player.closePlayer(TCPDISCONNECTTYPE.TCPTIMEOUT);

				// UDP still alive packet is handled by the udp broadcaster!
			}
		}

	}

	public void processTcpMessages(Player player) throws IOException {
		ThreadedQueue<TcpMessage> queue = player.getInputMessageQueue();

		TcpMessage message = null;
		message = queue.pollConcurrent();
		if (message != null) {

			processTcpMessage(player, message);
		}

	}

	void sendTcpStillAliveMessage(Player player) {

		// Send a TCP still alive message to the client
		if (player.getPlayerServerData().getLastTcpSendTimeMS()
				+ server.getServerConfig().getTcpStillAliveIntervallMS() < System.currentTimeMillis()) {
			server.sendTcpMessageLocked(player, TcpServerMessage.TCP_STILL_ALIVE, emptyBuffer);// 210 is the MessageID
																								// of a TCP stillalive
																								// refresh
			player.getPlayerServerData().resetLastTcpSendTime();
		}

	}

	private void processConsoleCommand(Player player, String input) {
		if (input.charAt(0) == '/') {
			input = input.substring(1);

			String[] segments = input.split("\\s+");// split by empty spaces (all empty spaces are removed. no need to
													// trim)

			if (segments.length > 0) {

				switch (segments[0]) {
				case "login":
					if (segments.length > 1) {
						if (server.getServerConfig().getAdminPassword() != null) {
							if (segments[1].equals(server.getServerConfig().getAdminPassword())) {
								this.server.sendTcpChatMessage(player, ChatMessageColors.serverNotificationColor,
										"Login successful! You are now an Admin.");

								// NOTE: Look into possible synchronisation locks

								player.getPlayerServerData().setAdmin(true);

							} else {
								this.server.sendTcpChatMessage(player, ChatMessageColors.errorColor,
										"Login failed: Incorrect password.");
							}
						} else {
							this.server.sendTcpChatMessage(player, ChatMessageColors.errorColor,
									"Admin account is not set up.");
						}
					} else {
						this.server.sendTcpChatMessage(player, ChatMessageColors.errorColor,
								"Login failed: Password not provided.");
					}
					break;

				case "logout":
					if (player.getPlayerServerData().isAdmin()) {
						player.getPlayerServerData().setAdmin(false);
						this.server.sendTcpChatMessage(player, ChatMessageColors.serverNotificationColor,
								"Logout successful.");
					} else {
						this.server.sendTcpChatMessage(player, ChatMessageColors.errorColor,
								"Logout failed: You were not logged in.");
					}
					break;

				case "nextmap":
					if (player.getPlayerServerData().isAdmin()) {
						if (this.server.getGameState().requestNextMapRotation()) {
							this.server.sendTcpBroadcastChatMessage(ChatMessageColors.gamestateColor,
									"Server will switch to the next map in a few seconds!");
						} else {
							this.server.sendTcpChatMessage(player, ChatMessageColors.errorColor,
									"The server is already in a map-transition. Wait until a new round started!");
						}
					} else {
						this.server.sendTcpChatMessage(player, ChatMessageColors.errorColor, adminRequiredMessage);
					}
					break;

				case "extend":
					if (player.getPlayerServerData().isAdmin()) {
						if (segments.length == 2) {
							int timeSecAdd = 0;
							boolean error = false;
							try {
								timeSecAdd = Integer.parseInt(segments[1]);// seconds to add to the timer
							} catch (NumberFormatException e) {
								timeSecAdd = 0;
								error = true;
							}
							if (error == false) {
								this.server.extendTime(timeSecAdd);
							} else {
								this.server.sendTcpChatMessage(player, ChatMessageColors.errorColor,
										"Time is not in number format!");
							}
						} else {
							// adminRequiredMessage
							this.server.sendTcpChatMessage(player, ChatMessageColors.errorColor,
									"Amount of time not specified.");
						}
					} else {
						this.server.sendTcpChatMessage(player, ChatMessageColors.errorColor, adminRequiredMessage);
					}
					break;

				default:
					this.server.sendTcpChatMessage(player, ChatMessageColors.errorColor, "unknown server-command.");
					break;
				}

			}
		}

	}

	public void processTcpMessage(Player player, TcpMessage message) throws IOException {
		int packetID = message.getID();

		ByteBuffer input = message.getBuffer();
		input.position(0);// reset position to 0

		switch (packetID) {

		case 1:
			int gameVersion = BufferConverter.buffer_read_u16(input);

			if (gameVersion == ServerCore.SERVER_PROTOCOL_VERSION) {

				// confirm

				int onlineMode = BufferConverter.buffer_read_u8(input);
				player.getPlayerServerData().setPlayerIsOnline(onlineMode == 1 ? true : false);

				if (onlineMode != 1) {
					player.closePlayer(TCPDISCONNECTTYPE.ONLINEMODE_REQUIRED);
				} else {

					String salt = "";
					int saltLen = 0;

					if (server.getServerConfig().hasPassword()) {
						salt = Util.generateRandomString(10);
						player.getPlayerServerData().setPasswordSalt(salt);
						saltLen = salt.length();

						System.out.println(salt);
					}

					ByteBuffer tcpMessage = ByteBuffer.allocate(3 + saltLen);
					BufferConverter.buffer_write_u8(tcpMessage, ServerCore.MODIFIED ? 1 : 0);

					BufferConverter.buffer_write_u8(tcpMessage, saltLen);
					if (saltLen > 0) {
						BufferConverter.buffer_write_chars_u8(tcpMessage, salt);
					}

					BufferConverter.buffer_write_u8(tcpMessage, server.getServerConfig().getClientUdpRefreshRate());

					server.sendTcpMessageLocked(player, TcpServerMessage.SERVER_INFO, tcpMessage);
				}

			} else {
				// if the version is not the right one, then send a player disconnect TCP
				// message and disconnect the player
				String m = "Protocol versions (Client: " + gameVersion + " / Server: "
						+ ServerCore.SERVER_PROTOCOL_VERSION + ")";

				if (gameVersion < ServerCore.SERVER_PROTOCOL_VERSION) {
					m += "#Please update your client to the latest version.";
				} else {
					m += "#The Server needs to be updated to be compatible with the client.";
				}

				player.closePlayer(TCPDISCONNECTTYPE.INCOMPATIBLESERVERVERSION, m);
			}

			break;

		case 2:
			// password packet

			int pwLen = BufferConverter.buffer_read_u8(input);
			String password = BufferConverter.buffer_read_chars_u8(input, pwLen);

			String comparePassword = "";
			try {
				comparePassword = Util
						.sha1(server.getServerConfig().getPassword() + player.getPlayerServerData().getPasswordSalt());
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				player.closePlayer(TCPDISCONNECTTYPE.PROCESSINGERROR);
				break;
			}

			if (password.equals(comparePassword)) {
				ByteBuffer tcpMessage = ByteBuffer.allocate(1);
				BufferConverter.buffer_write_u8(tcpMessage, 1);// 1 = ok, 0 = not ok
				server.sendTcpMessageLocked(player, TcpServerMessage.PLAYER_VALIDATED_CONFIRMATION, tcpMessage);

				player.getPlayerServerData().setServerPasswordPassed(true);
			} else {
				player.closePlayer(TCPDISCONNECTTYPE.WRONGPASSWORD);
			}

			break;

		case 210:// still-alive message (resets TCP counter). Doesn't have data.

			break;

		case 20:// player disconnected
			player.closePlayer();
			break;

		case 240:
			if (player.getPlayerServerData().getUdpTestPacketRecieved() == true) {
				player.getPlayerServerData().setUdpConnectionEstablished(true);
			}
			break;

		default: {

			if (server.getServerConfig().hasPassword() == false || (server.getServerConfig().hasPassword() == true
					&& player.getPlayerServerData().getServerPasswordPassed())) {

				switch (packetID) {

				case 3:// player information

					// Now read the playerdata from the packets and refresh the

					// read playername
					int nameLength = BufferConverter.buffer_read_u8(input);
					String name = BufferConverter.buffer_read_chars_u16(input, nameLength);

					// set Playername
					player.getPlayerProfileData().setPlayerName(name);

					// steamID
					if (player.getPlayerServerData().getPlayerIsOnline()) {
						long steamID = BufferConverter.buffer_read_u64(input);
						String steamIDstring = Long.toUnsignedString(steamID);

						player.getPlayerProfileData().setSteamID(steamIDstring);
					}

					// reads player info
					PlayerSkin skin = player.getPlayerSkinCopy();
					// color information

					skin.skinID = (byte) BufferConverter.buffer_read_u8(input);
					skin.skin_R = (byte) BufferConverter.buffer_read_u8(input);
					skin.skin_G = (byte) BufferConverter.buffer_read_u8(input);
					skin.skin_B = (byte) BufferConverter.buffer_read_u8(input);
					skin.armor_R = (byte) BufferConverter.buffer_read_u8(input);
					skin.armor_G = (byte) BufferConverter.buffer_read_u8(input);
					skin.armor_B = (byte) BufferConverter.buffer_read_u8(input);
					skin.eye_R = (byte) BufferConverter.buffer_read_u8(input);
					skin.eye_G = (byte) BufferConverter.buffer_read_u8(input);
					skin.eye_B = (byte) BufferConverter.buffer_read_u8(input);

					// apply player skin
					player.applyPlayerSkin(skin);

					// -------------------------------------------------------

					player.getPlayerServerData().setSigned(true);// player is now officially in the game

					// confirm

					ByteBuffer tcpOut = ByteBuffer.allocate(3);
					BufferConverter.buffer_write_u8(tcpOut, player.getID());
					BufferConverter.buffer_write_u16(tcpOut, player.getUdpKey());

					server.sendTcpMessageLocked(player, TcpServerMessage.CLIENT_INFO_CONFIRM, tcpOut);// send the
																										// package

					player.sendPlayerJoinedToListener();

					player.getServer().sendTcpBroadcastChatMessage(ChatMessageColors.serverNotificationColor,
							"Player " + '"' + name + '"' + "[" + player.getID() + "]" + " connected.");

					server.consolePrintln("Player " + '"' + name + '"' + "[" + player.getID() + "] connected");

					server.addLeaderboardEntry(
							new LeaderboardEntry(player.getID(), name, player.getPlayerProfileData().getBadgeID()));

					break;

				// player ranking status (badge, multiplayer experience, etc...)
				case 4:
					int multiplayer_experience = BufferConverter.buffer_read_u32(input);
					byte badge_index = (byte) BufferConverter.buffer_read_u8(input);

					player.getPlayerProfileData().setExperience(multiplayer_experience);
					player.getPlayerProfileData().setBadgeID(badge_index);

					player.getServer().addLeaderboardEntry(LeaderboardState.LEADERBOARDADDTYPE.OVERWRITE_BADGE_ONLY,
							new LeaderboardEntry(player.getID(), player.getPlayerProfileData().getPlayerName(),
									badge_index));

					break;

				case 6:
					// player finished loading the map
					DebugIO.println("PLAYER FINISHED MAP LOADING");
					player.getPlayerServerData().setPlayerReady(true);// Player is verified and is ready to play (client
																		// loaded the map and player can now move
																		// around)

					break;

				case 120:// current leaderboard requested
					int leaderboardRequestType = BufferConverter.buffer_read_u8(input);
					int leaderboardRequestOffset = BufferConverter.buffer_read_u8(input);
					int leaderboardRequestSize = BufferConverter.buffer_read_u8(input);
					int leaderBoardownEntry = BufferConverter.buffer_read_u8(input);
					boolean leaderboardAddOwnEntry = false;
					if (leaderBoardownEntry == 1) {
						leaderboardAddOwnEntry = true;
					}

					if (player.getPlayerServerData().getUdpTestPacketRecieved() == true
							&& player.getPlayerServerData().getUdpConnectionEstablished() == true) {
						player.getPlayerServerData()
								.addLeaderboardRequest(new LeaderboardRequest(leaderboardRequestType,
										leaderboardRequestOffset, leaderboardRequestSize, leaderboardAddOwnEntry));
					}

					break;

				default:

					if (player.getPlayerServerData().getUdpTestPacketRecieved() == true
							&& player.getPlayerServerData().getUdpConnectionEstablished() == true) {

						switch (packetID) {
						case 10:// player request
							int requestedPlayerID = BufferConverter.buffer_read_u8(input);
							player.getRequestedPlayerIDs().offerConcurrent(requestedPlayerID);
							break;

						case 150:
							int saltID = BufferConverter.buffer_read_u8(input);

							String salt = Util.generateRandomString(10);

							player.getPlayerServerData().setPasswordSalt(salt);

							var len = 2 + salt.length();

							ByteBuffer outbuffer = ByteBuffer.allocate(len);
							BufferConverter.buffer_write_u8(outbuffer, saltID);
							BufferConverter.buffer_write_u8(outbuffer, salt.length());

							BufferConverter.buffer_write_chars_u8(outbuffer, salt);

							server.sendTcpMessageLocked(player, TcpServerMessage.PASSWORD_CRYPT_DATA, outbuffer);

							break;

						case 151:

							saltID = BufferConverter.buffer_read_u8(input);// should be 0;
							int passwordLen = BufferConverter.buffer_read_u8(input);

							String cryptedPW = BufferConverter.buffer_read_chars_u8(input, passwordLen);

							String comparePW = "";
							String emptyPW = "";
							try {
								comparePW = Util.sha1(server.getServerConfig().getAdminPassword()
										+ player.getPlayerServerData().getPasswordSalt());
								emptyPW = Util.sha1("");

							} catch (NoSuchAlgorithmException e) {
								// TODO Auto-generated catch block
								player.closePlayer(TCPDISCONNECTTYPE.PROCESSINGERROR);
								break;
							}

							if (cryptedPW.equals(emptyPW)) {
								this.server.sendTcpChatMessage(player, ChatMessageColors.errorColor,
										"Login failed: Admin-mode not available.");
							} else {
								if (cryptedPW.equals(comparePW)) {
									this.server.sendTcpChatMessage(player, ChatMessageColors.serverNotificationColor,
											"Login successful! You are now an Admin.");

									// NOTE: Look into possible synchronisation locks

									player.getPlayerServerData().setAdmin(true);

								} else {
									this.server.sendTcpChatMessage(player, ChatMessageColors.errorColor,
											"Login failed: Incorrect password.");
								}
							}

							break;

						// further Mappackets were requested.
						case 199:

							int requestedMapOrderID = BufferConverter.buffer_read_u16(input);

							// sending map
							if (player.getServer().getRessourceHandler().getMapHandler().mapReady()) {
								if (requestedMapOrderID == player.getServer().getRessourceHandler().getMapHandler()
										.getMapMagicID()) {
									// It can happen that while the player requests a new packet from a map, the
									// server already loaded a new map (which makes this packet invalid)
									// The ID check prevents this from happening
									player.getMapTransmitter().sendPacket();
								}
							}

							break;

						case 200:// chatmessage

							String playerName = player.getPlayerProfileData().getPlayerName();

							String chatMessage = BufferConverter.buffer_read_chars_u8(input, input.capacity());
							// send TCP broadcast

							if (chatMessage.length() > 0) {
								if (chatMessage.charAt(0) == '/') {
									// possible console command
									processConsoleCommand(player, chatMessage);
								} else {
									// normal chat message (send to other clients as a broadcast)
									player.getServer().sendTcpBroadcastChatMessage(
											'"' + playerName + '"' + "[" + player.getID() + "]: " + chatMessage);
								}
							}

							break;

						case 201:
							// reset run
							player.getCheckpointQueue().clearConcurrent();

							break;

						case 202:// checkpoint activated

							// recieve player time
							int time = BufferConverter.buffer_read_u32(input);
							player.getCheckpointQueue().offerConcurrent(time);

							break;

						case 203:// goal activated

							long goalTime = BufferConverter.buffer_read_u32(input);
							// TODO: check if checkpoint count is valid
							// TODO: Better hashcode for player identification
							byte medal = server.getRessourceHandler().getMapHandler().getMedalIDfromTime(goalTime);

							player.getServer()
									.addLeaderboardEntry(new LeaderboardEntry(player.getID(),
											player.getPlayerProfileData().getPlayerName(), goalTime, medal,
											player.getPlayerProfileData().getBadgeID()));

							// clear checkpoint list
							player.getCheckpointQueue().clearConcurrent();

							ByteBuffer b = ByteBuffer.allocate(0);
							TcpMessage updateLeaderboardMessage = new TcpMessage(TcpServerMessage.LEADERBOARD_UPDATE,
									b);// leaderboard update
							player.getServer().sendTcpBroadcast(updateLeaderboardMessage);// Send leaderboard update
																							// message
							// NOTE: The checkpoint list is currently unused.
							/**
							 * The idea was to use the checkpointlist to validate the goaltime. This could
							 * be done (in order to avoid blocking this thread) by moving the calculation to
							 * the "playerRealtimeThread".
							 */

							break;

						}

					}

				}

			} else {
				player.closePlayer(TCPDISCONNECTTYPE.PROTOCOL_VIOLATION);
			}

		}

			break;

		}
	}

}
