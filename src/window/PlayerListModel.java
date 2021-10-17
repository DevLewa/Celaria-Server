package window;

import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataListener;


/**
 * Listmodel for the playerentries in the Playerlist of the Serverwindow
 * 
 * 
 *
 */

public class PlayerListModel extends AbstractListModel<Object> {


	List<PlayerListEntry> list;
	
	public PlayerListModel(){
		list = new LinkedList<PlayerListEntry>();
	}
	
	@Override
	public Object getElementAt(int index) {
		// TODO Auto-generated method stub
		return list.get(index);
	}
	

	

	@Override
	public int getSize() {
		// TODO Auto-generated method stub
		return list.size();
	}
	
	//NOTE: synchronized not really the best solution (in terms of performance), as this function can be called from multiple threads on the server.
	public synchronized void removeElement(int playerID){
		SwingUtilities.invokeLater(new removeElementRunnableprivate(playerID));
	}


	public synchronized void addElement(PlayerListEntry entry){
		SwingUtilities.invokeLater(new addElementRunnable(entry));
	}

	
	private class removeElementRunnableprivate implements Runnable{

		private int playerID;
		public removeElementRunnableprivate(int playerID){
			this.playerID = playerID;
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			//first find the index of the object
			Object[] arr = list.toArray();
			boolean found = false;
			int index = -1;
			for(int i = 0;i<arr.length;i++){
				PlayerListEntry e = (PlayerListEntry)arr[i];
				if(e.getID() == playerID){
					index = i;
					found = true;
					break;
				}
			}
			if(found == true){

				list.remove(index);
				fireIntervalRemoved(this,index,index);
			}
		}
		
	}

	private class addElementRunnable implements Runnable{
		PlayerListEntry entry;
		public addElementRunnable(PlayerListEntry entry){
			this.entry = entry;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			list.add(entry);
			int index0 = list.size() - 1;
		    int index1 = index0;
		    fireIntervalAdded(this, index0, index1);
		}
		
	}

}
