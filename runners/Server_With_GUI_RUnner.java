package runners;

import gui.ServerGUI;

public class Server_With_GUI_RUnner {
	public static void main(String[] args) {
		Thread sender = new Thread(new ServerGUI());
		sender.run();
	}
	
}
