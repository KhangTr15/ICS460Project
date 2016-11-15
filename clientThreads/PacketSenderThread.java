package clientThreads;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import packets.Packet;

public class PacketSenderThread extends Thread{
	private InetAddress server;
	private DatagramSocket socket;
	private int port;
	private volatile boolean stopped = false;
	
	public PacketSenderThread(DatagramSocket socket, InetAddress address, int port) {
		this.server = address;
		this.port = port;
		this.socket = socket;
		this.socket.connect(server, port);
	}
	public void halt(){
		this.stopped = true;
	}
	
	@Override
	public void run(){
	}
	
	public void sendPacket(Packet packet){
		if(!stopped){
			Packet p = null;
			try {
				p = packet;
				byte[] data = Packet.serialize(p);
				System.out.println("---Packet Sender: Sending... Packet>>>>\n"+p+"\nhas size in bytes to send: "+data.length);
				DatagramPacket output = new DatagramPacket(data, data.length, server, port);
				socket.send(output);
				Thread.yield();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
