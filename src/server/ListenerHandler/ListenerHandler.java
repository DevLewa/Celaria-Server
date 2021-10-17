package server.ListenerHandler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import server.ListenerHandler.ServerEvents.*;
import window.ListenerEventCalls;


/**
 * Class which handles ServerListeners and forwards server-messages to them
 * (like player joined player left, console Message, etc...)

 * 
 *
 */

public class ListenerHandler {
	
	//TODO: Use concurrentList!!!!!
	List<ServerListener> listeners;
	Queue<ServerEvent> serverEvents;
	Queue<ServerEvent> tempServerEvents;
	
	
	ListenerEventTransporter eventTransporter;
	Thread eventTransporterThread;
	
	ListenerEventExecutor eventExecutor;
	Thread eventExecutorThread;
	
	ListenerEventCalls eventCalls;
	
	
	//Locks for both of the lists. Is probably not the nicest/most efficient way of handling things.
	ReentrantReadWriteLock listenerListLock;
	ReentrantReadWriteLock eventListLock;
	ReentrantReadWriteLock tempEventListLock;
	
	public ListenerHandler(){
		listeners = new ArrayList<ServerListener>();
		tempServerEvents = new LinkedList<ServerEvent>();//temporary storage of events to reduce blocking times between threads!
		serverEvents = new LinkedList<ServerEvent>();
		

		listenerListLock = new ReentrantReadWriteLock();
		eventListLock = new ReentrantReadWriteLock();
		tempEventListLock = new ReentrantReadWriteLock();
		
		
		eventCalls = new ListenerEventCalls(this); 
		
		
		eventExecutor = new ListenerEventExecutor(this);//this processes all the events
		eventExecutorThread = new Thread(eventExecutor,"Listener Event Executor");
		eventExecutorThread.start();
		
		
		eventTransporter = new ListenerEventTransporter(this);
		eventTransporterThread = new Thread(eventTransporter,"Event Transporter");
		eventTransporterThread.start();
		
	}
	
	public ListenerEventCalls getEventCaller(){
		return eventCalls;
	}
	
	public ReentrantReadWriteLock getTempEventListLock(){
		return tempEventListLock;
	}
	
	
	public ReentrantReadWriteLock getListenerListLock(){
		return listenerListLock;
	}
	
	public ReentrantReadWriteLock getEventListLock(){
		return eventListLock;
	}
	
	public void clearServerEvents(){
		getTempEventListLock().writeLock().lock();
		this.tempServerEvents.clear();
		getTempEventListLock().writeLock().unlock();
		
		getEventListLock().writeLock().lock();
		this.serverEvents.clear();
		getEventListLock().writeLock().unlock();
	}
	
	public Queue<ServerEvent> getTempEventQueue(){
		return tempServerEvents;
	}
	
	public Queue<ServerEvent> getEventQueue(){
		return serverEvents;
	}
	
	public List<ServerListener> getListenersList(){
		return listeners;
	}
	
	public void addServerListener(ServerListener listener){
		listenerListLock.writeLock().lock();
		listeners.add(listener);
		listenerListLock.writeLock().unlock();
	}
	
	
	public void addEventToTempList(ServerEvent event){
		tempEventListLock.writeLock().lock();
		tempServerEvents.add(event);
		tempEventListLock.writeLock().unlock();
	}
}
