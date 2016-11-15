package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

import contollers.Receiver;
import contollers.Sender;

public class ServerGUI extends JFrame implements Runnable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Receiver receiver;
	private static JTextArea jtaOutput;
//	public ServerGUI(Receiver controller) {
	private static Thread receiverThread;
	public ServerGUI() {
//		this.controller = controller;
		
		setSize(700,700);
		setLayout(new BorderLayout(1,1));
		setResizable(true);
		setTitle("Server");
		this.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		
		//settings for message output
		jtaOutput = new JTextArea("This is Servers LOG");
		jtaOutput.setBorder(new TitledBorder("LOG:"));
		jtaOutput.setWrapStyleWord(true);
		jtaOutput.setLineWrap(true);
		jtaOutput.setEditable(false);
		JScrollPane sc = new JScrollPane(jtaOutput);
		sc.setPreferredSize(new Dimension(300,0));	
		add(sc,BorderLayout.EAST);//adding log text area to frame
		
		
		
		JLabel authorsInfo = new JLabel("Made By: Andrey Yefremov, Javan Soliday, and Khang Tran 2016", JLabel.CENTER);
		add(authorsInfo,BorderLayout.SOUTH);
		setVisible(true);
		receiver = new Receiver();
		System.out.println("HHHHHHHHHHHHHHHHHHHHHHHHH "+receiver);
		receiverThread = new Thread(receiver);
		receiverThread.start();
	}
	
	public static void appendToOutput(String txt){
		jtaOutput.append("\n"+txt);
		jtaOutput.update(jtaOutput.getGraphics());
	}
	
//	public  static void startServer(){
//		//System.out.println("GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG"+receiver);
//	//	receiverThread.start();
//		//receiver.startUDPServer();
//		
//	}
	
	@Override
	public void run() {
//		System.out.println("GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG");
	//	receiver = new Thread(new Receiver());
		
		
	}
	
	
	
}
