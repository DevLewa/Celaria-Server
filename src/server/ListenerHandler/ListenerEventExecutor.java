package server.ListenerHandler;

import java.util.List;
import java.util.Queue;


/**
 * 
 * TODO: Profile the whole multithreading concept.
 * The general idea is that events (which are fired by the server)
 * don't block the execution of the serverthreads while the serverevents are passed
 * to the serverlisteners (which registered themselves on the server.)
 * 
 * 
 * 
 *
 */

public class ListenerEventExecutor implements Runnable{


	ListenerHandler listenerHandler;

	private boolean running;
	//TODO: CONCURRENTLIST (Concurrency Issues!!!)
	public ListenerEventExecutor(ListenerHandler handler){
		this.listenerHandler = handler;

		running = true;
	}

	public void setRunning(boolean run){
		this.running = run;
	}


	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(running){
			Queue<ServerEvent> eventList = listenerHandler.getEventQueue();
			List<ServerListener> listenerList = listenerHandler.getListenersList();

			//process all events in the EventList

			listenerHandler.getEventListLock().writeLock().lock();

			while(eventList.size()>0){
				ServerEvent event = eventList.poll();

				listenerHandler.getListenerListLock().readLock().lock();;
				for(int i = 0;i<listenerList.size();i++){
					event.execute(listenerList.get(i));
				}
				listenerHandler.getListenerListLock().readLock().unlock();
			}

			listenerHandler.getEventListLock().writeLock().unlock();

			try {
				Thread.sleep(200);//0.2 second
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
