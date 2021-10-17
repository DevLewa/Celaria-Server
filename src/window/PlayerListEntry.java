package window;

/**
 * Listentry for the "PlayerListModel" class.
 *
 */

public class PlayerListEntry {
	
	private String name;
	private int ID;
	
	public PlayerListEntry(int ID,String name){
		this.name = name;
		this.ID = ID;
	}
	
	public int getID(){
		return ID;
	}
	
	@Override
	public String toString(){
		return "[ID: "+ID+"] "+name;
	}

}
