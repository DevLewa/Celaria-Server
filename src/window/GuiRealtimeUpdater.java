package window;

import SystemInfo.SystemInfo;

/**
 * Class which updates the GUI of the window in realtime.
 * Implements runnable! (Thread)
 *
 */

public class GuiRealtimeUpdater implements Runnable{
	
	Window window;
	
	boolean run;
	
	public GuiRealtimeUpdater(Window window){
		this.window = window;
		run = true;
	}

	@Override
	public void run() {
		while(run){
		// TODO Auto-generated method stub
			window.refreshPlayerCount();
				
			try {
				Thread.sleep(1000);//1 second
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
