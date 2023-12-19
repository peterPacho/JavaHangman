import javafx.animation.PauseTransition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;


import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/*

	Controller for UI made with fxml.
	It takes care only of the first page - select port and start server.

 */

public class StartController implements Initializable
{
	@FXML
	private VBox root;

	@FXML
	private TextField text_port;

	@FXML
	private Button button_port;

	// https://stackoverflow.com/questions/7555564/what-is-the-recommended-way-to-make-a-numeric-textfield-in-javafx
	@Override
	public void initialize (URL location, ResourceBundle resources)
	{
		text_port.setText(String.valueOf(StartUI.port));

		// make this text field accept only numbers - this is the port number that server will listen on
		text_port.textProperty().addListener(new ChangeListener<String>()
		{
			@Override
			public void changed (ObservableValue<? extends String> observable, String oldValue, String newValue)
			{
				if (!newValue.matches("\\d*"))
				{
					text_port.setText(newValue.replaceAll("[^\\d]", ""));
				}
				else if (!text_port.getText().isEmpty())
				{
					if (Integer.parseInt(text_port.getText()) > 65535)
					{
						text_port.setText("" + 65535);
					}
				}

			}
		});

		if (StartUI.autoStart)
		{

			PauseTransition waitBeforeStart = new PauseTransition();
			waitBeforeStart.setDelay(Duration.millis(100));
			waitBeforeStart.setOnFinished(e ->
			{
				try
				{
					serverCreate(null);
				}
				catch (IOException ex)
				{
					System.out.println("Couldn't auto-start the server");
					throw new RuntimeException(ex);
				}
			});

			waitBeforeStart.play();
		}
	}

	public void serverCreate (ActionEvent event) throws IOException
	{
		StartUI.port = Integer.parseInt(text_port.getText());

		FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Server.fxml"));
		Parent root2 = loader.load();

		ServerController serverController = loader.getController();

		root2.getStylesheets().add("/styles/Server.css");
		root.getScene().setRoot(root2);
	}
}
