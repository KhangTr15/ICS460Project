package testings;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import clientThreads.PacketSenderThread;
import contollers.Sender;
import globals_constants.Constants;
import packets.Packet;

public class PacketSending_Tester implements Constants{

	public static void main(String[] args) {
//		Packet p  = new Packet(Packet.DATA, DATA_SIZE);
		Packet p = Sender.createEOTPacket();
		p.setAckno(45);
		p.setSeqno(33);
		InetAddress ia;
		DatagramSocket socket;
		PacketSenderThread sender;
		try {
			ia = InetAddress.getByName(SERVER_IP);
			socket = new DatagramSocket();
			sender = new PacketSenderThread( socket, ia, PORT);
//			p.setSeqno(3);
//			p.setAckno(1);
			sender.sendPacket(p);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
		


	}

}
