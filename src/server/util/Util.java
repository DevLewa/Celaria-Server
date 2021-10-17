package server.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Random;

import server.leaderboard.LeaderboardEntry;

public class Util {

	private static final Random secureRandom = new SecureRandom();
	private static final String secureRandom_characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	private static DecimalFormat format = new DecimalFormat("00");

	public static class LeaderboardFindResult {
		private LeaderboardEntry entry;
		private int placement;// starting at index 0;

		public LeaderboardFindResult(LeaderboardEntry entry, int placement) {
			this.entry = entry;
			this.placement = placement;
		}

		public LeaderboardEntry getEntry() {
			return entry;
		}

		// starting at index 0
		public int getPlacement() {
			return placement;
		}

	}

	public static Util.LeaderboardFindResult findPlayerEntry(LeaderboardEntry[] leaderboard, int id) {
		for (int i = 0; i < leaderboard.length; i++) {
			if (leaderboard[i].getPlayerID() == id) {
				return new Util.LeaderboardFindResult(leaderboard[i], i);
			}
		}
		return null;

	}

	public static String sha1(String input) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] messageDigest = md.digest(input.getBytes("UTF-8"));
		String s = String.format("%040x", new BigInteger(1, messageDigest));
		return s;
	}

	public static String generateRandomString(int length) {
		StringBuilder randomStr = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			randomStr.append(secureRandom_characters.charAt(secureRandom.nextInt(secureRandom_characters.length())));
		}
		return randomStr.toString();
	}

	//time in ticks (where 100 ticks = 1 second)
	public static String timeFormat(long time) {
		float sec = 0;
		float minutes = 0;
		float millisec = 0;

		if (time >= 0) {
			sec = (float) (Math.floor(time / 100f) % 60f);
			minutes = (float) Math.floor(time / 6000);
			millisec = (float) Math.floor(time % 100);
		}

		String t = ((int) minutes) + ":" + format.format(sec) + ":" + format.format(millisec);

		return t;
	}

}
