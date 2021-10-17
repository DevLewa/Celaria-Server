package DataStructures;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * 
 * Custom multithreading queue implementation.
 * 
 * 
 *
 * @param <T>
 */

public class ThreadedQueue<T>{

	ReentrantReadWriteLock lock;
	Queue<T> queue;
	
	
	public ThreadedQueue() {
		queue = new LinkedList<T>();
		lock = new ReentrantReadWriteLock();
	}
	
	public boolean isEmptyConcurrent() {
		boolean empty;
		lock.readLock().lock();
		empty = queue.isEmpty();
		lock.readLock().unlock();
		return empty;
	}
	
	public void offerConcurrent(T element) {

		lock.writeLock().lock();
		queue.offer(element);
		lock.writeLock().unlock();

	}
	
	public T pollConcurrent() {
		T element = null;
		lock.writeLock().lock();
		if(queue.isEmpty() == false) {
		element = queue.poll();
		}
		lock.writeLock().unlock();
		return element;	
	}
	
	//returns internal queue as a copy and clears the content of the current queue
	public Queue<T> returnAndClearConcurrent(){
		Queue<T> copy = null;
		lock.writeLock().lock();
		//move reverence to copy variable
		copy = queue;
		queue = new LinkedList<T>();//set internal queue to a new empty queue
		lock.writeLock().unlock();
		return copy;
	}
	
	public void clearConcurrent() {
		lock.writeLock().lock();
		queue.clear();
		lock.writeLock().unlock();
	}
}
