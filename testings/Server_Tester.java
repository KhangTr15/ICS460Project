package testings;

import contollers.Receiver;

public class Server_Tester {

	public static void main(String[] args) {
		Thread receiver = new Thread(new Receiver());
		receiver.start();

	}

}
