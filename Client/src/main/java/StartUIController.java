import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;


/*

	Main window of the game.
	If connection to the server successful then it goes to GameUI.

 */

public class StartUIController implements Initializable
{
	public TextField text_port;
	public Label label_error;
	public VBox StartUI;
	private PauseTransition errorLabelAutoHide = null;
	private ClientConnection clientConnection = null;

	public void connectToServer (ActionEvent event)
	{
		String ip = text_port.getText();
		int port;

		// check if ip and port in proper format
		if (!ip.contains(":"))
		{
			showErrorLabel("Incorrect IP format.\nPlease enter in IP:port format.\nFor example: \"127.0.0.1:5555\"");
			return;
		}

		// try parse port
		try
		{
			port = Integer.parseInt(ip.substring(ip.indexOf(":") + 1));
		}
		catch (NumberFormatException ignored)
		{
			showErrorLabel("Incorrect port.\nPlease enter in IP:port format.\nFor example: \"127.0.0.1:5555\"");
			return;
		}

		ip = ip.substring(0, ip.indexOf(":"));

		clientConnection = new ClientConnection(data -> Platform.runLater(() ->
		{
			handleClientMsg(data.toString());
		}), ip, port);

		clientConnection.start();
	}

	public void showErrorLabel (String error)
	{
		label_error.setText(error);
		label_error.setVisible(true);

		if (errorLabelAutoHide != null)
			errorLabelAutoHide.stop();
		errorLabelAutoHide = new PauseTransition();
		errorLabelAutoHide.setDelay(Duration.seconds(3));
		errorLabelAutoHide.setOnFinished(e ->
		{
			label_error.setVisible(false);
			errorLabelAutoHide = null;
		});
		errorLabelAutoHide.play();
	}

	/*
		Gets called when client thread sends the message while in the start ui.
	 */
	private void handleClientMsg (String msg)
	{
		switch (msg)
		{
			case "err:socket":
			case "err:streams":
				showErrorLabel("Couldn't connect to the server");
				break;
			case "err:send":
				showErrorLabel("Connected but couldn't send data.");
				break;
			case "ok:connected":
				// If connected to the server, change the UI
				try
				{
					FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CategoryUI.fxml"));
					Parent root2 = loader.load();

					CategoryUIController categoryUIController = loader.getController();
					categoryUIController.setClient(this.clientConnection);
					root2.getStylesheets().add("/styles/CategoryUI.css");
					categoryUIController.displayCategories();
					StartUI.getScene().setRoot(root2);
				}
				catch (IOException e)
				{
					throw new RuntimeException(e);
				}

				break;
			default:
				System.out.println(msg);
				break;
		}
	}

	@Override
	public void initialize (URL url, ResourceBundle resourceBundle)
	{

	}
}
