package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import contollers.Receiver;
import contollers.Sender;


public class ClientGUI extends JFrame implements ActionListener, Runnable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static JTextArea jtaOutput;
	private JTextField jtfPacketSize = new JTextField();
	private JTextField jtfTimeOutClient = new JTextField();
	private JTextField jtfTimeOutServer = new JTextField();
	private JTextField jtfSlidingWinSize = new JTextField();
	private JTextField jtfSeqNumStart = new JTextField();
	private JTextField jtfSeqNumEnd = new JTextField();
	private JComboBox<String> jcbSituationErrror = new JComboBox<>(new String[]{"drop 25% of data or contol frames","Corrupt 25% of data or control frames","user-specified"});
	private JTextField jtfDropRate = new JTextField();
	private JTextField jtfErrorRate= new JTextField();
	private JButton jbtStop = new JButton("Stop");
	private JButton jbtStart = new JButton("Start");
	private JButton jbtChoose= new JButton("Choose");
	private Sender sender;
	private static JScrollPane sc ;
	
	
	public ClientGUI(Sender controller) {
		this.sender = controller;
		
		setSize(700,700);
		setLayout(new BorderLayout(1,1));
		setLocationRelativeTo(null);
		setResizable(true);
		setTitle("Client");
		this.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		JPanel controlPanel = new JPanel();//control panel
		controlPanel.setLayout(new GridLayout(12,2));
		controlPanel.add(new Label("Select File To Send:"));
		jbtChoose.addActionListener(this);
		controlPanel.add(jbtChoose);
		controlPanel.add(new Label("Size Of Packet:"));
		jtfPacketSize.setText(500+"");
		controlPanel.add(jtfPacketSize);
		
		
		controlPanel.add(new Label("TimeOut Interval Client:"));
		jtfTimeOutClient.setText(2000+"");
		controlPanel.add(jtfTimeOutClient);
		
		controlPanel.add(new Label("TimeOut Interval Server:"));
		jtfTimeOutServer.setText(2000+"");
		controlPanel.add(jtfTimeOutServer);
		
		controlPanel.add(new Label("Sliding Windows Size:"));
		controlPanel.add(jtfSlidingWinSize);
		controlPanel.add(new Label("Range of sequance Numbers:"));
		controlPanel.add(new Label());

		controlPanel.add(new Label("Start Num:"));
		jtfSeqNumStart.setText("1");
		controlPanel.add(jtfSeqNumStart);
		controlPanel.add(new Label("End Num:"));
		jtfSeqNumEnd.setText("~");
		controlPanel.add(jtfSeqNumEnd);
		controlPanel.add(new Label("Situation Error:"));
		jcbSituationErrror.addActionListener(this);
		controlPanel.add(jcbSituationErrror);
		
		controlPanel.add(new Label("Drop Packets:"));
		controlPanel.add(jtfDropRate);		
		controlPanel.add(new Label("Lose ACKs:"));
		controlPanel.add(jtfErrorRate);
		jtfDropRate.setVisible(false);
		jtfErrorRate.setVisible(false);
		jbtStart.addActionListener(this);
		jbtStart.setEnabled(false);
		jbtStop.addActionListener(this);
		jbtStop.setEnabled(false);
		controlPanel.add(jbtStart);
		controlPanel.add(jbtStop);
		add(controlPanel, BorderLayout.CENTER);//adding controlpanel to frame
		
		
		
		//settings for message output
		jtaOutput = new JTextArea("Testing\nHello");
		jtaOutput.setBorder(new TitledBorder("LOG:"));
		jtaOutput.setWrapStyleWord(true);
		jtaOutput.setLineWrap(true);
		jtaOutput.setEditable(false);
		sc = new JScrollPane(jtaOutput);
		sc.setPreferredSize(new Dimension(300,0));	
		add(sc,BorderLayout.EAST);//adding log text area to frame
		
		
		JLabel authorsInfo = new JLabel("Made By: Andrey Yefremov, Javan Soliday, and Khang Tran 2016", JLabel.CENTER);
		add(authorsInfo,BorderLayout.SOUTH);
		setVisible(true);
		
	}
	
	public static void appendToOutput(String txt){
		jtaOutput.append("\n"+txt);

		jtaOutput.update(jtaOutput.getGraphics());
		jtaOutput.setCaretPosition(jtaOutput.getDocument().getLength());
//		jtaOutput.setCaretPosition(jtaOutput.getText().length() - 1);
//		
		
		//JScrollPane variable pane initialized with JTextArea area
		//We will update area with new text
//		JTextArea temp = (JTextArea) sc.getViewport().getView();
//		//new text to add
//		JTextArea c = new JTextArea();
//		c.append("text \n"+txt);
//		//update through pointers
//		temp = c;
//		sc.validate();
		
//		jtaOutput.repaint();
		
	}
	@Override
	public void actionPerformed(ActionEvent ev) {
		if(ev.getSource()==jbtStart){
			sender.setDataSize(Integer.parseInt(jtfPacketSize.getText().trim()));
			sender.setTimeout(Integer.parseInt(jtfTimeOutClient.getText().trim()));
			sender.setStartingSeqNum(Integer.parseInt(jtfSeqNumStart.getText().trim()));		
			Receiver.setStartingSeqNum(Integer.parseInt(jtfSeqNumStart.getText().trim()));
			Receiver.Timeout(Integer.parseInt(jtfTimeOutServer.getText().trim()));			
			setErrorRate();
			if(jcbSituationErrror.getSelectedIndex()==2){
				Receiver.setDropRate(Double.parseDouble(jtfDropRate.getText().trim()));
				Receiver.setErrorRate(Double.parseDouble(this.jtfErrorRate.getText().trim()));
				sender.setDropRate(Double.parseDouble(jtfDropRate.getText().trim()));
				sender.setErrorRate(Double.parseDouble(this.jtfErrorRate.getText().trim()));
			}
			pause(2000);
			sender.startSending();
			
			
		}else if(ev.getSource()==jbtStop){
			System.exit(0);
		}else if(ev.getSource()==jcbSituationErrror){
			setErrorRate();
			
			
		}else if(ev.getSource()==jbtChoose){
			this.sender.setFileToSend(getFile());
			if(sender.getFileToSend()!=null){
				ClientGUI.jtaOutput.append("\nYou Shoose File to send: "+sender.getFileToSend().getName());
			}
			jbtStart.setEnabled(true);
			jbtStop.setEnabled(true);
		}
		
	}
	private void setErrorRate(){
		
		
		if(jcbSituationErrror.getSelectedIndex()==0){
			
			sender.setDropRate(0.025);
			Receiver.setDropRate(0.025);
			sender.setErrorRate(0.0);
			Receiver.setErrorRate(0.0);
			jtfDropRate.setVisible(false);
			jtfErrorRate.setVisible(false);
		}else if(jcbSituationErrror.getSelectedIndex()==1){
			sender.setDropRate(0.0);
			Receiver.setDropRate(0.0);
			sender.setErrorRate(0.025);
			Receiver.setErrorRate(0.025);
			jtfDropRate.setVisible(false);
			jtfErrorRate.setVisible(false);
		}else if(jcbSituationErrror.getSelectedIndex()==2){
			jtfDropRate.setVisible(true);
			jtfErrorRate.setVisible(true);
			
		}
		
		//System.out.println(jcbSituationErrror.getSelectedIndex());
		
	}
	public void pause(int mills){
		try {
			Thread.sleep(mills);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static File getFile(){
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		int result = fileChooser.showOpenDialog(null);
		//if user clicked Cancel button
		if(result == JFileChooser.CANCEL_OPTION)
			JOptionPane.showMessageDialog(null,"No File Was Selected", "Error", JOptionPane.ERROR_MESSAGE);
		//	System.exit(1);
		
		File file = fileChooser.getSelectedFile();
		
		if((file ==null)||(file.getName().equals(""))){
			JOptionPane.showMessageDialog(null, "Invalid Name", "Invalid Name", JOptionPane.ERROR_MESSAGE);
		//	System.exit(1);
		}
		return file;
	}

	@Override
	public void run() {
		
		
	}
}
