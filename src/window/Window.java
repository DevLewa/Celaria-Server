package window;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;




import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultCaret;

import server.Server;
import server.ServerCore;
import server.ListenerHandler.ServerListener;



/**
 * GUI implementation for the Celaria Server. 
 * 
 * 
 *
 */

public class Window{
	JFrame frame;

	Server server;

	JPanel east;
	JPanel west;
	JPanel north;
	JPanel south;
	JPanel center;
	JPanel topPanel;
	JPanel topControlPanel;
	JPanel playerListPanel;

	JList playerList;
	PlayerListModel playerList_content;
	JScrollPane playerScrollPane;

	JTextArea consoleOutput;
	JTextField consoleInput;


	JLabel playerCountLabel;
	
	JLabel consoleTextHeader;
	JLabel playerListTextHeader;

	
	JPanel bottomRightCornerPanel;

	
	JScrollPane consoleScrollPane;

	JButton startButton;

	GuiRealtimeUpdater guiUpdater;

	Thread guiUpdater_thread;

	
	JCheckBox consoleAutoScroll;

	WindowServerListener listener;

	public Window(Server server){
		
		
		//setNativeGUILook();

		this.server = server;




		//frame
		frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	
		
		WindowAdapter exitListener = new WindowAdapter() {

		    @Override
		    public void windowClosing(WindowEvent event) {
		    	if(server.isRunning() == true){
					try {
						server.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		    	}
		    	System.exit(0);
		    }
		};
		frame.addWindowListener(exitListener);
		

		
		
		
		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(),BoxLayout.Y_AXIS));

		//Panels------------------------
		east = new JPanel();
		west = new JPanel();
		south = new JPanel();
		north = new JPanel();
		center = new JPanel();

		topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

		topControlPanel = new JPanel();

		playerListPanel = new JPanel();
		playerListPanel.setLayout(new BorderLayout());//new BoxLayout(west,BoxLayout.Y_AXIS));



		bottomRightCornerPanel = new JPanel();
		bottomRightCornerPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		



		east.setLayout(new BorderLayout());//BoxLayout(east,BoxLayout.Y_AXIS));
		west.setLayout(new BoxLayout(west, BoxLayout.PAGE_AXIS));
		center.setLayout(new BoxLayout(center,BoxLayout.X_AXIS));
		//add panels to frame----------------------


		frame.getContentPane().add(north);
		frame.getContentPane().add(center);//east and west are in center!
		frame.getContentPane().add(south);
		//----ELEMENTS-----------------------------

		//Buttons
		addButtons();


		playerList_content = new PlayerListModel();
		playerList = new JList(playerList_content);//JTextArea(20,10);

		playerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		playerList.setVisible(true);
		
		//---------------------------------------------------------
		playerScrollPane = new JScrollPane(playerList);
		playerScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		consoleOutput = new JTextArea(20,40);
		consoleOutput.setLineWrap(true);
		consoleOutput.setEditable(false);

		consoleOutput.getDocument().addDocumentListener(new LineLimitDocumentListener(1500));

		//((AbstractDocument) consoleOutput.getDocument()).setD //new LineLimitDocumentListener(20));
		
		consoleScrollPane = new JScrollPane(consoleOutput);
		consoleScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);


		//------------------------------------------------------------------------------------------------------


		//autoscroll SOMETIMES NOT WORKING!
		consoleInput = new JTextField(20);



		playerCountLabel = new JLabel();
		

		south.setLayout(new BoxLayout(south, BoxLayout.LINE_AXIS));
		
		south.add(playerCountLabel);
		south.add(bottomRightCornerPanel);
		//bottomRightCornerPanel.setBackground(new Color(255,0,0));
		

		consoleAutoScroll = new JCheckBox("console autoscroll");
		

		
		bottomRightCornerPanel.add(consoleAutoScroll);


		consoleTextHeader = new JLabel();
		//consoleTextHeader.setBackground(new Color(0,0,0));
		consoleTextHeader.setText("Console");

		playerListTextHeader = new JLabel();
		playerListTextHeader.setText("Player list");


		//sizes
		playerListPanel.setMinimumSize(new Dimension(130, 120));
		playerListPanel.setPreferredSize(new Dimension(200,120));
		playerListPanel.setMaximumSize(new Dimension(250,Short.MAX_VALUE));


		//add to panels ----------------------------
		east.add(BorderLayout.NORTH,consoleTextHeader);
		east.add(BorderLayout.CENTER,consoleScrollPane);
		//east.add(BorderLayout.SOUTH,consoleInput);

		playerListPanel.add(BorderLayout.NORTH,playerListTextHeader);
		playerListPanel.add(BorderLayout.CENTER,playerScrollPane);



		west.add(playerListPanel);


		//JPanel p = new JPanel();
		//p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		
		

		north.add(topPanel);

		
		topControlPanel.setAlignmentX( Component.CENTER_ALIGNMENT );
		
		topPanel.add(topControlPanel);
		
		

		center.add(west);
		center.add(east);



		frame.setSize(800,600);
		frame.setLocationRelativeTo(null);//centers view

		frame.setVisible(true);

		
		
		
		//automatically select consoleautoscroll 
		consoleAutoScroll.setSelected(true);
		DefaultCaret caret = (DefaultCaret)consoleOutput.getCaret();
		consoleOutput.setCaretPosition(consoleOutput.getDocument().getLength());
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		//enable disable autoscroll
		consoleAutoScroll.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent arg0) {
	        	updateAutoscroll();
	        }
	    });		


		guiUpdater = new GuiRealtimeUpdater(this);

		listener = new WindowServerListener(this);

		guiUpdater_thread = new Thread(guiUpdater,"GUI Updater");
		guiUpdater_thread.start();

		
		refreshPlayerCount();


		server.addServerListener(getServerListener());



		



	}
	//https://stackoverflow.com/questions/44264637/java-jscrollpane-toggle-autoscroll-on-off
	private void updateAutoscroll() {
		DefaultCaret caret = (DefaultCaret)consoleOutput.getCaret();
		if(consoleAutoScroll.isSelected()){
            consoleOutput.setCaretPosition(consoleOutput.getDocument().getLength());
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        } else {
            caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        }
	}
	
	public void setNativeGUILook(){
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InstantiationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IllegalAccessException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (UnsupportedLookAndFeelException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}
	

	public void setServerHeader(String text){
		frame.setTitle(text);
	}


	public void setBottomInfoText(String t){
		playerCountLabel.setText(t);
	}

	public void addButtons(){
		startButton = new JButton("Start Server");
		startButton.setText(getStartButtonText());
		startButton.addActionListener(new startButtonListener());
		startButtonColorRefresh();
		topControlPanel.add(startButton);

	}

	public boolean getServerRunning(){
		return server.isRunning();
	}

	public String getStartButtonText(){
		String text = "Start Server";
		if(getServerRunning() == true){
			text = "Stop Server";
		}
		return text;
	}

	public void consolePrint(String output){
		consoleOutput.append(output);
	}

	public void consolePrintln(String output){
		consoleOutput.append(output+"\n");

	}

	public void startButtonColorRefresh(){
		if(getServerRunning() == false){
			startButton.setBackground(new Color(76, 255, 0));
		}else{
			startButton.setBackground(new Color(255, 66, 66));
		}
	}


	class startButtonListener implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if(server.isRunning() == false){
				server.start();
			}else{
				try {
					server.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			startButton.setText(getStartButtonText());
			startButtonColorRefresh();
		}
	}

	class StopListener implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent arg0) {
			//button.setText("Stop");
		}
	}

	public void refreshPlayerCount(){
		setBottomInfoText("Players Online: "+server.getPlayerCount());
	}


	public PlayerListModel getPlayerListModel(){
		return playerList_content;
	}



	public ServerListener getServerListener(){
		return listener;
	}

}
