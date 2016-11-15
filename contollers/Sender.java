/**
 * This is Sender Class aka Client for ICS460 Class. 
 * @author Andrey Yefermov, Javan Soliday, Khang Tran 
 *
 */

package contollers;

import java.io.BufferedInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import clientThreads.PacketReceiverThread;
import clientThreads.PacketSenderThread;
import globals_constants.Constants;
import gui.ClientGUI;
import packets.Packet;


public class Sender implements Runnable, Constants{
	private File fileToSend;
	private DataInputStream in;
	public final static int PORT = 13;
	public static final String HOSTNAME = "localhost";
	public final static int BUFFER_SIZE = 100; //default buffer size
	public static final int WINDOW_SIZE = 5;
	public final static int DATA_SIZE = 500;
	public static int dataSize=DATA_SIZE;//default data size
	public final static int DATA_COUNT_SIZE = 4;
	public final static int END_OF_FILE_FLAG = -1;
	private int lastSent = 0; //added
	private int waitingForACK = 0; //added
	private int MaxSegSize = 8; //added
	private Packet[] buffer;
	private int buffersIndex;
	private int bufferReadingIndex;
	private Packet nextToSendPacket;
	private int seqNum;
	private static int expectAckno;
	private DatagramPacket inStream;
	private DatagramSocket socket;
	private Double errorRate;
	private Double dropRate;
	private int timeout = 2000;
	private long stopTime = 0;
	

	public Sender() {
		resetAllVariables();
	}
	public void resetAllVariables(){
		expectAckno=1;
		seqNum=1;
		buffersIndex=0;
		errorRate = 0.3;
		dropRate = 0.3;
		bufferReadingIndex = 0;
		nextToSendPacket=null;
		in = null;
		buffer = new Packet[BUFFER_SIZE];
		inStream = null;
		socket = null;
		timeout = 3000;
		
	}
	
	
	public void startSending(){
		resetAllVariables();
	//	fileToSend = getFile();
		out(Calendar.getInstance().getTime()+"\nClient: ........START SENDIND PROCESS..........");
		out("Client: Opening File To Send.... "+fileToSend.getName());
		Queue<Packet> received = new LinkedList<Packet>(); //added
		byte[] fileBytes =  fileToSend.getName().getBytes(); // added
		int lastSeq = (int) Math.ceil((double) fileBytes.length /MaxSegSize); //added
		try {
			in = this.getInputFileAsDataInputStream(fileToSend);
			System.out.println("Number of packets to send: " + lastSeq); // added
		} catch (FileNotFoundException e1) {
			out("Sender: Error with opening file .... exiting....");
			JOptionPane.showMessageDialog(null,"Eroor with opening file!\nProgram is switching off.",  "File Open Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
		
		out(Calendar.getInstance().getTime()+"\nClient: Start Sending to >>>> " +HOSTNAME +":"+PORT);
		PacketSenderThread sender = null;
		PacketReceiverThread receiver = null; 
		//addPacketToBuffer(createNamePacket(fileToSend.getName()));
		received.add(createNamePacket(fileToSend.getName()));
		
		while(isMoreToRead(in)){
			while(!this.isBufferFull() && isMoreToRead(in)){
				if(isBufferBeenProcessed())
					clearBuffer();
				byte[] nextChankOfData = readNextFromFile(in);
				
				if(nextChankOfData!=null){
					Packet p = new Packet(Packet.DATA, dataSize, calcChekSum(nextChankOfData),
							expectAckno, seqNum++, nextChankOfData, nextChankOfData.length);
					//addPacketsToBuffer(p);
					received.add(p); //added
				}			
			}	
			while(!isBufferEmpty()){
				
				//this.nextToSendPacket =this.getNextFromBuffer();
				this.nextToSendPacket = received.peek(); //This is supposed
				nextToSendPacket.setAckno(expectAckno);
				boolean sended = false;
			
				try {
					InetAddress ia = InetAddress.getByName(HOSTNAME);
					socket = new DatagramSocket(0);
					sender = new PacketSenderThread( socket, ia, PORT);
					sender.start();
					receiver = new PacketReceiverThread(socket);
					receiver.start();
					Packet inP=null;
					
					
					
					out(Calendar.getInstance().getTime()+"\nClient: START sending packets:\n"+nextToSendPacket);
					while(lastSent - waitingForACK < WINDOW_SIZE && lastSent < lastSeq){
						
//						if(getProbability(0.5)){
//							
//						}else{
//							
//						}
						if(getProbability(errorRate)){
							Packet p = new Packet(Packet.DATA, dataSize,(short)-12, nextToSendPacket.getAckno(), nextToSendPacket.getSeqno(),nextToSendPacket.getData(),22);
							out("Client: Sending Packet With ERROR...."+p);
							pause(timeout);
							sender.sendPacket(p);
						}else if(getProbability(this.dropRate)){
							out("Sender: Packet has been DROPPED by me >>> "+nextToSendPacket);
						}else{	
							out("Client: Sending Packet...."+nextToSendPacket);
							pause(timeout);
							sender.sendPacket(nextToSendPacket);
							
						}
						
													
						inP= receiver.receivePacket(timeout);
						if(inP==null){
							out(Calendar.getInstance().getTime()+"\nClient: TIMEOUT resending packet......."+nextToSendPacket);
							continue;
						}
						if(!this.isACKPacket(inP) || !isLegimativeACKPacket(inP)){
							out(Calendar.getInstance().getTime()+"\nClient: ACK is WRONG...disposing\n"+inP);
							continue;
						}
						out(Calendar.getInstance().getTime()+"\nClient: I got RIGHT ACK \n"+inP);
						sended=true;
					}
				
					expectAckno++;
									 	
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SocketException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(isBufferBeenProcessed()){
				clearBuffer();
			}
			
		}
		boolean endOfTransactionFlag = false;
		Packet p = null;
		Packet endOfTransactionPacket = createEOTPacket();
		while(!endOfTransactionFlag){
			pause(timeout);
			sender.sendPacket(endOfTransactionPacket);
			p= receiver.receivePacket(timeout);
			if(p!=null && p.getAckno() == END_OF_FILE_FLAG){
				endOfTransactionFlag=true;
				break;
			}else {
				out(Calendar.getInstance().getTime()+"\nClient: TIMEOUT resending packet......."+endOfTransactionPacket);
				
			}
		}
		closeInputStream();
		out("\n\nClient: File "+fileToSend.getName() +" \nbeen SUCCESSFULLY sent to "+ Sender.HOSTNAME + ".\n\n");
		out("########################################################################");
	}

	public DataInputStream getInputFileAsDataInputStream(File file) throws FileNotFoundException {
		return new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
		
	}
	public static File getFile(){
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		int result = fileChooser.showOpenDialog(null);
		//if user clicked Cancel button
		if(result == JFileChooser.CANCEL_OPTION){
			JOptionPane.showMessageDialog(null,"No File Was Selected. \nProgram is switching off.", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
		
		File file = fileChooser.getSelectedFile();
		if((file ==null)||(file.getName().equals(""))){
			JOptionPane.showMessageDialog(null, "Invalid Name", "Invalid Name\nProgram is switching off.", JOptionPane.ERROR_MESSAGE);
		//	System.exit(1);
			
		}
		return file;
	}
	
	
	private Packet createNamePacket(String name) {
		
		Packet p = new Packet(Packet.DATA, dataSize, calcChekSum(name.getBytes()),
				expectAckno, seqNum++, name.getBytes(), name.getBytes().length);

		return p;
		
	}
	private Packet reseivePacket(DatagramSocket socket){
		inStream = new DatagramPacket(new byte[1024], 1024);
		Packet pIn = null;
		try {
			socket.receive(inStream);
			try {
				pIn = (Packet)deserialize(inStream.getData());
			} catch (ClassNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}
		
		return pIn;
	}
	

	private void pause(int mills) {
		try {
			Thread.sleep(mills);
		} catch (InterruptedException e) {
		}
		
	}
	private short calcChekSum(byte[] nextChankOfData) {
		// TODO Auto-generated method stub
		return 0;
	}
	public File getFileToSend() {
		return fileToSend;
	}
	
	public Packet getNextPacketToSend(){
		return nextToSendPacket;
	}
	
	public void clearBuffer(){
		buffer = new Packet[BUFFER_SIZE];
		buffersIndex=0;
		bufferReadingIndex=0;
	}
	
	public static Packet createEOTPacket(){
		Packet p =new Packet(Packet.DATA, dataSize);
			p.setAckno(expectAckno);
			p.setSeqno(END_OF_FILE_FLAG);
			
		
		return p;
	}
	
	public void closeInputStream(){
		try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean addPacketToBuffer(Packet p){
		if(p == null || isBufferFull() || !isLegimativeDataPacket(p) )
			return false;
		buffer[buffersIndex++] = p;
		return true;
	}
	
	public int getAmountOfAvailableSpaceInBuffer(){
		return getBufferSize()-getNumOfPacketsInBuffer();
	}
	public int getNumOfPacketsInBuffer(){
		return buffersIndex;
	}
	public int getBufferSize(){
		return buffer.length;
	}
	public boolean addPacketsToBuffer(Packet... packets){
		if(getAmountOfAvailableSpaceInBuffer()<packets.length)
			return false;
		int count =0;
		for(Packet p : packets){
		
			if(addPacketToBuffer(p)){
				count++;
			}
		}
		return count == packets.length;
	}
	public Packet getFromBufferAt(int pos){
		if(pos<0 || isBufferEmpty() || !(pos<buffersIndex))
			return null;
		return buffer[pos];
	}
	
	public boolean isBufferBeenProcessed(){
		return buffersIndex==bufferReadingIndex && isBufferEmpty();
	}
	
	//this flag to make possible to reRead whole buffer
	public void makeBufferNotProccessed(){
		bufferReadingIndex =0;
	}
	
	public Packet getNextFromBuffer(){
		if(isBufferBeenProcessed() || isBufferEmpty())
			return null;
		return buffer[bufferReadingIndex++];
	}
	
	
	
	public boolean isACKPacket(Packet p){
		return p.getLen() == Packet.ACK_LEN;
	}
	public boolean isLegimativeACKPacket(Packet p){
		return p.getLen() == Packet.ACK_LEN && isCheckSumRight(p);
	}
	private boolean isCheckSumRight(Packet p) {
		
		return p.getCksum() == calcChekSum(p);
	}
	private boolean isLegimativeDataPacket(Packet p) {///////////////needs CODE
		
		return p.getLen()==Packet.DATA_LEN;
	}
	private void dropDataPacket(){
		nextToSendPacket = null;
	}
	
	private boolean isBufferFull(){
		return buffersIndex == buffer.length;
	}
	private boolean isBufferEmpty(){
		return buffersIndex == 0 || buffersIndex==bufferReadingIndex;
		
	}
	
	private boolean getProbability(double rate){
		return Math.random() < rate;
	}
	private boolean getProbability(int probProcent){
		return Math.random() < (((double)probProcent)/100);
	}
	public Packet createPacket(int type, byte[] data,int expectAckno, int seqNum){
		Packet packet = new Packet(Packet.DATA, dataSize);
		packet.setValueOfBytesInLoad(data.length, data);
		packet.setData(createFixedSizeByteArray(data));
		packet.setSeqno(seqNum);
		packet.setAckno(expectAckno);
		packet.setCksum(calcChekSum(packet));
		return packet;
	}
	
	private short calcChekSum(Packet packet) {
		// TODO Auto-generated method stub
		return 0;
	}
	public byte[] createFixedSizeByteArray(byte[] data){
		if(data == null)
			return new byte[dataSize];
		
		if(data.length==dataSize)
			return data;
		byte[] answr = new byte[dataSize];
		System.arraycopy(data, 0, answr, 0, data.length);
		
		return answr;
	}
	public int readAckNum(Packet packet){
		return packet.getAckno();
	}
	public byte[] readNextFromFile(DataInputStream in){
		if(!isMoreToRead(in))
			return null;
		
		
		
		byte[] answr = null;
		LinkedList<Byte> bytes = new LinkedList<Byte>();
		int count = 0;
		while(isMoreToRead(in) && count < (dataSize-DATA_COUNT_SIZE)){
			try {
				byte b = in.readByte();
				
				bytes.add(b);
				count++;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		answr = new byte[bytes.size()];
		count = 0;
		while(!bytes.isEmpty()){
			answr[count++] = bytes.pop();
		}
	
		
		return answr;
	}
	

	
	public boolean isMoreToRead(DataInputStream in){
		if(in!=null)
			try {
				return in.available() > 0;
			} catch (IOException e) {
				out("Sender: ERROR ERROR ERROR >>> Something happen with file reading");
			}
		return false;
	}
	
	
	public File readFile(String fileName){
		return new File(fileName);
	}
	public byte[] readFile(){	
		return readFile(this.fileToSend);
	}
	public DataInputStream openFile(File file) throws FileNotFoundException {
		return new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
		
	}
	public void out(String msg){
		System.out.println(msg);
		ClientGUI.appendToOutput(msg);
	}
	
	
	//this mehtod is returning an array of bytes from given file
	public byte[] readFile(File file){
		byte[] out=null;
		LinkedList<Byte> tmp =null;
		if(file != null){				
			try {
				in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
				tmp = new LinkedList<Byte>();
				int reading;
				while((reading=in.read())!= -1){
					tmp.add((byte) reading);
				}
				out = new byte[tmp.size()];
				int i=0;
				while(!tmp.isEmpty()){
					out[i++]=tmp.pop();
				}
				in.close();
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				
		}	
		return out;
	}
	
	private boolean isFileSelected(){
		return fileToSend != null;
	}
	public boolean isPacketAck(Packet p){
		return p.getLen() == Packet.ACK_LEN;
	}
	public boolean isPacketData(Packet p){
		return p.getLen() == Packet.DATA_LEN;
	}
	
	public static byte[] serialize(Packet pack) throws IOException {
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    ObjectOutputStream os = new ObjectOutputStream(out);
	    os.writeObject(pack);
	    return out.toByteArray();
	}
	public static Packet deserialize(byte[] data) throws IOException, ClassNotFoundException {
	    ByteArrayInputStream in = new ByteArrayInputStream(data);
	    ObjectInputStream is = new ObjectInputStream(in);
	    return (Packet) is.readObject();
	}
	public void setStartingSeqNum(int num){
		expectAckno=num;
		seqNum=num;
	}
	public void setErrorRate(Double errorRate) {
		this.errorRate = errorRate;
	}
	public void setDropRate(Double dropRate) {
		this.dropRate = dropRate;
	}
	public void setDataSize(int newSize){
		dataSize = newSize;
//		JOptionPane.showMessageDialog(null, dataSize +" "+newSize);
	}
	
	
	public void setTimeout(int waitingTime) {
		this.timeout = waitingTime;
	}
	public void setFileToSend(File fileToSend) {
		this.fileToSend = fileToSend;
	}

	public int getBuffersIndex() {
		return buffersIndex;
	}
	
	public DataInputStream getInputStream() {
		return in;
	}
	public void setInputSteam(DataInputStream in) {
		this.in = in;
	}
	@Override
	public void run() {
		startSending();
//		resetAllVariables();
		
	}
}

