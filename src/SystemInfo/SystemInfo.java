package SystemInfo;

public class SystemInfo {
	

	final static int mb = 1024*1024;
	
	public static int getCoreCount(){
	/* Total number of processors or cores available to the JVM */
        return Runtime.getRuntime().availableProcessors();
	}
	
	public static long getUsedMemoryBytes(){
		return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	}
	
	public static long getUsedMemoryMegaBytes(){
		return getUsedMemoryBytes()/mb;
	}
	
	public static long getFreeMemoryBytes(){
    /* Total amount of free memory available to the JVM (in bytes)*/
		return Runtime.getRuntime().freeMemory();
	}
	
	public static long getFreeMemoryMegaBytes(){
	    /* Total amount of free memory available to the JVM */
			return getFreeMemoryBytes()/mb;
		}

	public static long getMaxMemoryPossibleBytes(){
    /* This will return Long.MAX_VALUE if there is no preset limit */
		long maxMemory = Runtime.getRuntime().maxMemory();
    /* Maximum amount of memory the JVM will attempt to use */
		return maxMemory;
	}
	
	public static long getMaxMemoryPossibleMegaBytes(){
		return getMaxMemoryPossibleBytes()/mb;
	}
	
	public static long getMaxMemoryAvailableBytes(){
		/* Total memory currently available to the JVM */
        return Runtime.getRuntime().totalMemory();
	}
	public static long getMaxMemoryAvialableMegBytes(){
		return getMaxMemoryAvailableBytes()/mb;
	}

    

}
