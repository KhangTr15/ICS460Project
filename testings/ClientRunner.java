package testings;

import contollers.Sender;

public class ClientRunner {

	public static void main(String[] args) {
		
		Thread sender = new Thread(new Sender());
		sender.start();

	}

}
