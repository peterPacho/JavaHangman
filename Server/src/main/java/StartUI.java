import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class StartUI extends Application
{
	// some settings and const
	public static boolean autoStart = false; // to skip the port selecting GUI and start the server with default port
	public static int port = 5555; // default port
	public static int maxCategoriesToSendToClient = 3; // if more files than this number it will pick random ones
	public static String keys = "QWERTYUIOP;ASDFGHJKL;ZXCVBNM"; // ';' shows when to do page break when printing those keys in the client
	public static int recommendedWordLength = 20; // if too long server will warn


	public static void main (String[] args)
	{
		launch(args);
	}

	@Override
	public void start (Stage primaryStage)
	{
		try
		{
			// setup the UI
			Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/Start.fxml")));
			primaryStage.setTitle("Hangman Game Server");
			Scene sceneStart = new Scene(root, 800, 600);
			sceneStart.getStylesheets().add("/styles/Start.css");

			primaryStage.setScene(sceneStart);
			primaryStage.show();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(-1);
		}
	}

	@Override
	public void stop ()
	{
		System.exit(0);
	}


}
