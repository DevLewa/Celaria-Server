package server.leaderboard;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Leaderboard
 * 
 * 
 *
 */
public class LeaderboardState{
	
	public static enum LEADERBOARDADDTYPE{
		REPLACE,//default
		OVERWRITE_BADGE_ONLY
	}

	List<LeaderboardEntry> list;
	ReentrantReadWriteLock listLock;

	boolean isSorted;//if the list is sorted

	boolean sorting;//if sorting is in process

	public LeaderboardState(){
		list = new LinkedList<LeaderboardEntry>();
		listLock = new ReentrantReadWriteLock();
		isSorted = true;

		sorting = false;
	}


	public LeaderboardEntry getTopEntry(){
		LeaderboardEntry e = null;

		listLock.readLock().lock();
		if(list.size()>0){
			e = new LeaderboardEntry(list.get(0));
		}
		listLock.readLock().unlock();

		return e;
	}



	public void addEntry(LEADERBOARDADDTYPE type,LeaderboardEntry e){
	

		listLock.writeLock().lock();//writelock as this whole operation has to be atomic
		
		//check if list contains ID
		boolean containsID = false;
		int indexFound = 0;
		for (ListIterator<LeaderboardEntry> iter = list.listIterator(); iter.hasNext(); ) {
			LeaderboardEntry element = iter.next();
			if(element.getPlayerID() == e.getPlayerID()) {
				containsID = true;
				break;
			}else {
				indexFound++;
			}
		}
		
		if(containsID){
			//update current element if the new time is less then the current one
			LeaderboardEntry lastEntry = list.get(indexFound);
			switch(type) {
			case REPLACE:
			{
				if(lastEntry.timeSet() == true){
					if(e.getTime() < lastEntry.getTime()){
						list.remove(indexFound);
						list.add(e);
					}
				}else{
					//if time is not set then replace the element
					list.remove(indexFound);
					list.add(e);
				}
			}
				break;
			case OVERWRITE_BADGE_ONLY:
				lastEntry.badge = e.badge;
				break;
			}
			
		}else{
			list.add(e);
		}

		isSorted = false;

		//TODO: Optimize
		sort();

		listLock.writeLock().unlock();

	}

	public boolean isSorted(){
		if(sorting == true){
			return false;
		}else{
			return isSorted;
		}
	}

	public boolean isSorting(){
		return sorting;
	}

	public LeaderboardEntry[] getListArrayCopy(){
		listLock.readLock().lock();
		LeaderboardEntry[] ret = list.toArray(new LeaderboardEntry[list.size()]);
		listLock.readLock().unlock();

		return ret;
	}


	public void resetTimes(){
		listLock.writeLock().lock();
		Iterator<LeaderboardEntry> iterator = list.iterator(); 
		while (iterator.hasNext()){
			LeaderboardEntry e = iterator.next();
			e.resetTime();
		}
		listLock.writeLock().unlock();
	}

	private void sort(){
		isSorted = true;
		sorting = true;

		Collections.sort(list, new Comparator<LeaderboardEntry>() {
			@Override
			public int compare(LeaderboardEntry o1, LeaderboardEntry o2) {
				return o1.compareTo(o2);
			}
		});
		sorting = false;

	}



	/**
	 * NOTE: Can be optimized.
	 * @param playerID
	 */
	public void remove(int playerID){
		listLock.writeLock().lock();
		Iterator<LeaderboardEntry> iterator = list.iterator(); 
		while (iterator.hasNext()){
			LeaderboardEntry e = iterator.next();
			if(e.getPlayerID() == playerID){
				list.remove(e);
				break;
			}
		}
		listLock.writeLock().unlock();
	}

	public int getListSize(){
		listLock.readLock().lock();
		int size = list.size();
		listLock.readLock().unlock();

		return size;
	}

	public List<LeaderboardEntry> getListCopy(){
		listLock.readLock().lock();
		List<LeaderboardEntry> newList = new LinkedList<LeaderboardEntry>(list);
		listLock.readLock().unlock();
		return newList;
	}

}
