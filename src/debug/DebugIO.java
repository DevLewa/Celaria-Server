package debug;

/**
 * 
 * Debug class which outputs messages in the console
 * 
 * 
 *
 */

public class DebugIO {
	
	public static void println(String s){
		//System.out.println("DEBUG: "+s);
	}
	
	public static void printRunning(String s){
		//System.out.println("Running-Thread: "+s+" "+Math.random());
	}
	
	public static void errPrintln(String s){
		System.err.println("DEBUG: "+s);
	}
	

	
}
