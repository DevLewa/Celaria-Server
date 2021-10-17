package server.player.data;

/**
 * Class which stores information about the skin of the particular player
 * this includes RGB Color values for the different sections of the player character
 * as well as reflectivity values, etc...
 * 
 * 
 *
 */

public class PlayerSkin {
	
	public byte skinID;
	
	public byte eye_R;
	public byte eye_G;
	public byte eye_B;
	
	public byte skin_R;
	public byte skin_G;
	public byte skin_B;
	
	public byte armor_R;
	public byte armor_G;
	public byte armor_B;
	
	public PlayerSkin(){
		//set default values
	}
	
	public PlayerSkin(PlayerSkin source){
		this.skinID = source.skinID;
		
		this.eye_R = source.eye_R;
		this.eye_G = source.eye_G;
		this.eye_B = source.eye_B;
		
		this.skin_R = source.skin_R;
		this.skin_G = source.skin_G;
		this.skin_B = source.skin_B;
		
		this.armor_R = source.armor_R;
		this.armor_G = source.armor_G;
		this.armor_B = source.armor_B;
		
	}
	
	
	
}
