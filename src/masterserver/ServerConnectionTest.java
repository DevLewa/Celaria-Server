package masterserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import masterserver.HttpConnection.Response;

/**
 * Handles connectiontest to validate if port is accessible from the outside
 * 
 * 
 *
 */

public class ServerConnectionTest {

	class NetOut implements Runnable {
		String ipAddress;
		int port;
		java.net.Socket socket;
		boolean error;
		AtomicBoolean finished;

		public NetOut(String ipAddress, int port) {
			this.ipAddress = ipAddress;
			this.port = port;
			this.finished = new AtomicBoolean(false);
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				this.socket = new java.net.Socket(ipAddress, port);
				this.finished.set(true);
			} catch (UnknownHostException e) {
				error = true;
				this.finished.set(true);
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				error = true;
				this.finished.set(true);
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void close() throws IOException {
			if (socket != null) {
				if (this.socket.isClosed() == false) {
					this.socket.close();
				}
			}
		}

		public boolean getFinished() {
			return this.finished.get();
		}
	}

	// opens socket and listens on it
	class NetIn implements Runnable {

		ServerSocketChannel serverChannel;
		Selector selector;
		SocketChannel client;

		int port;
		String ipAddress;

		AtomicBoolean finished;

		AtomicBoolean run;
		AtomicBoolean startedListening;

		public NetIn(String ipAddress, int port) throws IOException {
			// create socket in constructor to make sure that the socket is open BEFORE
			// NetOut.run is called (otherwise the response might not be detected)
			this.port = port;
			this.ipAddress = ipAddress;

			this.finished = new AtomicBoolean(false);
			this.run = new AtomicBoolean(true);
			this.startedListening = new AtomicBoolean(false);
		}

		public void setRun(boolean run) {
			this.run.set(run);
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				serverChannel = ServerSocketChannel.open();
				serverChannel.configureBlocking(false);
				System.out.println(ipAddress + " // " + port);
				// serverChannel.socket().setReuseAddress(true);
				serverChannel.socket().bind(new InetSocketAddress(port));
				selector = Selector.open();
				serverChannel.register(selector, SelectionKey.OP_ACCEPT);

				boolean stop = false;
				long timeOutTime = System.currentTimeMillis() + 5L * 1000L;

				this.startedListening.set(true);

				while (System.currentTimeMillis() < timeOutTime && stop == false && this.run.get() == true) {

					System.out.println(System.currentTimeMillis());
					try {
						selector.select();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						// e.printStackTrace();

					}

					/**
					 * If we are here, it is because an operation happened (or the TIMEOUT expired).
					 * We need to get the SelectionKeys from the selector to see what operations are
					 * available. We use an iterator for this.
					 */

					Iterator<SelectionKey> keys = null;
					try {
						keys = selector.selectedKeys().iterator();
					} catch (ClosedSelectorException e) {
						keys = null;
					}

					if (keys != null) {
						while (keys.hasNext()) {

							SelectionKey key = keys.next();
							// remove the key so that we don't process this OPERATION again.
							keys.remove();

							// key could be invalid if for example, the client closed the connection.
							if (!key.isValid()) {
								continue;
							}

							if (key.isAcceptable()) {

								// accept connection

								client = serverChannel.accept();

								stop = true;

								this.finished.set(true);
								System.out.println("ACCEPTED IN : " + this.finished);

							}
						}
					}

				}
				System.out.println("CLOSED THREAD");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public boolean getStartedListening() {
			return this.startedListening.get();
		}

		public boolean getFinished() {
			return finished.get();
		}

		public void close() {
			this.setRun(false);

			if (serverChannel != null) {
				if (serverChannel.isOpen() == true) {
					System.out.println("CLOSING SOCKET");
					try {
						serverChannel.socket().close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						serverChannel.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.out.println("SOCKETS CLOSED");
					System.out.println("SOCKETS CLOSED A");
				}
			}

			System.out.println("SOCKETS CLOSED D");
			if (selector != null) {

				// close all connections
				Iterator<SelectionKey> keys = null;
				try {
					keys = selector.selectedKeys().iterator();
				} catch (ClosedSelectorException e) {
					keys = null;
				}

				if (keys != null) {
					while (keys.hasNext()) {
						SelectionKey key = keys.next();

						SelectableChannel channel = key.channel();
						try {
							channel.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
				}

				if (selector.isOpen()) {
					try {
						selector.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			System.out.println("SOCKETS CLOSED G");
			if (client != null) {
				if (client.isOpen()) {
					System.out.println("CLOSING CLIENT");
					try {
						client.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			System.out.println("SOCKETS CLOSED F");
		}
	}

	String ipAddress;
	int port;

	public ServerConnectionTest(String ipAddress, int port) {
		this.port = port;
		this.ipAddress = ipAddress;
	}

	// returns false on error
	public boolean checkBlocked() {
		try {
			NetIn in = new NetIn(ipAddress, port);
			NetOut out = new NetOut(ipAddress, port);

			Thread tIn = new Thread(in);
			Thread tOut = new Thread(out);

			tIn.start();

			/*
			 * try { Thread.sleep(100); } catch (InterruptedException e) { // TODO
			 * Auto-generated catch block e.printStackTrace(); }
			 */
			while (in.getStartedListening() == false) {
				// wait as long as the thread didn't start listening
			}
			System.out.println("TEST B");
			tOut.start();

			long timeoutTime = System.currentTimeMillis() + 3 * 1000;// 3 second timeout

			// check for timeout
			boolean timeout = false;
			while (!(in.getFinished() == true && out.getFinished() == true)) {
				if (System.currentTimeMillis() >= timeoutTime) {
					System.out.println("TIMEOUT");
					System.out.println("TEST C");
					timeout = true;
					break;
				}
				
				if(out.error == true) {
					timeout = true;
					break;
				}
			}

			System.out.println("TEST D");
			in.close();
			System.out.println("TEST F");
			out.close();
			System.out.println("TEST G");
			
			/*
			 * try { tOut.join(); } catch (InterruptedException e) { // TODO Auto-generated
			 * catch block e.printStackTrace(); }
			 * 
			 * try { tIn.join(); } catch (InterruptedException e) { // TODO Auto-generated
			 * catch block e.printStackTrace(); }
			 */
			System.out.println(in.getFinished() + " ... " + out.getFinished() + ".... " + timeout);

			if (timeout == false) {
				return true;
			}
			return false;

		} catch (IOException e) {

			return false;
		}
	}
}
