import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;


public class StartingClass extends Application
{
	public static Image heartImg, heartBrokenImg;
	public static Parent mainRoot;
	public static int maxWordToGuessWidth = 12 * 55; // max number of character in one row in GuessUI, 55 is space for one label
	public static int windowWidth = 700;

	public static void main (String[] args)
	{
		launch(args);
	}


	@Override
	public void start (Stage primaryStage) throws Exception
	{
		Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/StartUI.fxml")));
		mainRoot = root;
		primaryStage.setTitle("Hangman V9003");

		try
		{
			heartImg = new Image("/img/pixelHeart.png");
			heartBrokenImg = new Image("/img/pixelHeartBroken.png");
		}
		catch (Exception e)
		{
			System.out.println("Couldn't load image !");
		}

		Scene sceneStart = new Scene(root, 800, windowWidth);
		sceneStart.getStylesheets().add("/styles/StartUI.css");

		primaryStage.setScene(sceneStart);
		primaryStage.show();
	}

	@Override
	public void stop () throws IOException
	{
		System.exit(0);
	}

}
