package clientThreads;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Calendar;

import javax.swing.JOptionPane;

import globals_constants.Constants;
import packets.Packet;

public class PacketReceiverThread extends Thread{
	private DatagramSocket socket;
	private volatile boolean stopped = false;
	public PacketReceiverThread(DatagramSocket socket){
		this.socket = socket;
	}
	public void halt(){
		this.stopped = true;
	}
	
	@Override
	public void run(){

	}
	
	public Packet receivePacket(long timeToWait){
		if(stopped)
			return null;
		byte[] buffer = new byte[Constants.BUFFER_SISE_OF_DATAGRAM];	
		return receivePacket(timeToWait, new DatagramPacket(buffer, buffer.length));
			
	}
	public Packet receivePacket(long timeout, DatagramPacket inStream) {
		if(stopped)
			return null;
		Packet answr= null;
		System.out.println("***********************************************");
			System.out.println("....Packet Receiver: Waiting for income Packet");
			DatagramPacket in = inStream;
			try{
				
				socket.setSoTimeout((int)timeout);
				socket.receive(in);
				
				answr = Packet.deserialize(in.getData());
				System.out.println(Calendar.getInstance().getTime()+"\n---Packet Receiver: I got >>> \n"+answr);
				return answr;
			}catch(IOException ex){

			} catch (ClassNotFoundException e) {

			}finally{
				Thread.yield();
			}		
		return answr;
	}
}
