package runners;

import contollers.Sender;
import gui.ClientGUI;

public class Client_With_GUI_runner {

	public static void main(String[] args) {
		Thread clientGUI = new Thread(new ClientGUI(new Sender()));
		clientGUI.start();

	}

}
