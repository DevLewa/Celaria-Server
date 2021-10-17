package server.ListenerHandler;

import java.util.Queue;

/**
 * Pushes the Serverevents from the tempList to the main list.
 * SHOULD BLOCK THE ACCESS TO THE -TEMPEVENTLIST- AS LITTLE AS POSSIBLE!!!!
 * 
 * 
 *
 */

public class ListenerEventTransporter implements Runnable{

	ListenerHandler listenerHandler;
	
	boolean running;
	public ListenerEventTransporter(ListenerHandler handler){
		listenerHandler = handler;
		
		running = true;
	}
	
	public void setRunning(boolean run){
		this.running = run;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (running){
		
			Queue<ServerEvent> eventList = listenerHandler.getEventQueue();
			Queue<ServerEvent> tempEventList = listenerHandler.getTempEventQueue();
			/**
			 * First check if the tempEvenList is > 0. (only readlock required)
			 * if it is > 0 then we can start the while statement which requires a writelock
			 * 
			 * The idea behind this is, that if the tempEventList.size() == 0, then we can 
			 * skip the if statement (and the writelock) altogether, which in turn
			 * leads to less unnessecary writeblocking calls.

			 */
			boolean tempEventShiftNessecary = false;
			listenerHandler.getTempEventListLock().readLock().lock();
			if(tempEventList.size()>0){
				tempEventShiftNessecary = true;
			}
			listenerHandler.getTempEventListLock().readLock().unlock();
			

			//only executed if the previous if statement set this variable to true.
			//this blocks the whole tempEventlist (which can block the serverthreads)
			
			//TODO: reduce the threadblocking!
			if(tempEventShiftNessecary == true){
				/*the eventlistlock happens before the tempeventlsit lock so that (if other threads are blocking the eventListLock)
				 * this thread waits for this lock to be opened, before taking the tempeventlistlock (which can affect serverperformance by a huge
				 * margin!)
				 * 
				 * The idea is to reduce the locking time of the temEventList as much as possible)
				 */
				listenerHandler.getEventListLock().writeLock().lock();//lock eventList before tempEventList
				listenerHandler.getTempEventListLock().writeLock().lock();
				if(tempEventList.size()>0){
					eventList.offer(tempEventList.poll());
				}
				listenerHandler.getTempEventListLock().writeLock().unlock();
				listenerHandler.getEventListLock().writeLock().unlock();
			}

			
			try {
				Thread.sleep(20);//20 ms
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
