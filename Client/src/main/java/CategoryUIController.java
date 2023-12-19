import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.ResourceBundle;

public class CategoryUIController implements Initializable
{
	public VBox categorySelectorContainer;
	public VBox CategoryUI;
	public Label labelResult;
	public Label labelPickCategory;
	private ClientConnection clientConnection = null;
	CategoryUIController thisReference;
	private ArrayList<Integer> lastLivesData; // last categories data, used to animate hearts

	/*
		Stuff needed to display word categories
	*/
	@FXML
	public VBox categoriesParent;
	private EventHandler<MouseEvent> categoriesClicked; // event for user clicking on a category


	public void close (ActionEvent event)
	{
		System.exit(0);
	}

	/*
		Used to "transfer" client from StartingClass (where only connection to the client is attempted) to this class.
	 */
	public void setClient (ClientConnection clientConnection)
	{
		this.clientConnection = clientConnection;
	}

	@Override
	public void initialize (URL url, ResourceBundle resourceBundle)
	{
		thisReference = this;
		lastLivesData = new ArrayList<>();

		categoriesClicked = new EventHandler<MouseEvent>()
		{
			@Override
			public void handle (MouseEvent mouseEvent)
			{
				String categoryName = ((AnchorPane) mouseEvent.getSource()).getId();
				clientConnection.sendMsg("GET:CATEGORY;" + categoryName);

				Object fromServer = clientConnection.getObject();
				if (fromServer == null)
				{
					showAlertAndQuit("Can't establish connection to server. Quiting.");
					return;
				}

				String stringFromServer = fromServer.toString();

				if (stringFromServer.equals("ERR") || stringFromServer.equals("UNKNOWN"))
				{
					showAlertAndQuit("Requested \"" + categoryName + "\". Server returned error. Quiting.");
					return;
				}

				if (stringFromServer.equals("ZERO_LIVES"))
				{
					System.out.println("This category has 0 lives. You shouldn't be able to select it.");
					return;
				}

				// This launches the game guessing UI
				try
				{
					FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/GuessUI.fxml"));
					Parent root3 = loader.load();

					GuessUIController guessUIController = loader.getController();
					root3.getStylesheets().add("/styles/GuessUI.css");

					// give that class data that it requires
					guessUIController.CategoryUI = CategoryUI;
					guessUIController.gameData = (WordBank.WordForClient) fromServer;
					guessUIController.categoryUIController = thisReference;
					guessUIController.clientConnection = clientConnection;
					guessUIController.init();

					CategoryUI.getScene().setRoot(root3);
				}
				catch (IOException e)
				{
					throw new RuntimeException(e);
				}
			}
		};
	}

	public void displayCategories ()
	{
		// send request to server
		clientConnection.sendMsg("GET:CATEGORIES");
		String dataFromServer = clientConnection.getMsg();

		// if something wrong check again
		if (dataFromServer == null || !dataFromServer.contains(";"))
		{
			clientConnection.sendMsg("GET:CATEGORIES");
			dataFromServer = clientConnection.getMsg();

			// if failed again
			if (dataFromServer == null || !dataFromServer.contains(";"))
			{
				showAlertAndQuit("Couldn't connect to server or server returned unknown message");
				return;
			}
		}

		ArrayList<String> catNames = new ArrayList<>();
		ArrayList<Integer> catLives = new ArrayList<>();

		// String from server should be in format
		// <category name>-<lives left>;<category name>-<lives left>; etc...
		String[] catsAndLives = dataFromServer.split(";");

		for (int i = 0; i < catsAndLives.length; i++)
		{
			String[] catLive = catsAndLives[i].split("-");

			catNames.add(catLive[0]);
			catLives.add(Integer.parseInt(catLive[1]));
		}

		if (lastLivesData.isEmpty())
			lastLivesData.addAll(catLives);

		CategorySelector catSelect = new CategorySelector(categoriesClicked);
		catSelect.showCategories(categoriesParent, catNames, catLives, lastLivesData);

		// update the results label
		int catsToComplete = catNames.size();
		for (Integer i : catLives)
		{
			if (i == 0)
			{
				catsToComplete = -1;
				break;
			}
			else if (i > 100)
				catsToComplete--;
		}

		if (catsToComplete == -1)
		{
			labelResult.setText("You lost the game. Three lives not enough?");
			labelResult.setStyle("-fx-text-fill: red");
			labelPickCategory.setVisible(false);

		}
		else if (catsToComplete == 2)
		{
			labelResult.setText("Two more to go");

		}
		else if (catsToComplete == 1)
		{
			labelResult.setText("Getting pretty close... just one more.");

		}
		else if (catsToComplete == 0)
		{
			labelResult.setText("You win! Nice.");
			labelPickCategory.setVisible(false);
			labelResult.setStyle("-fx-text-fill: green");
		}

		// make backup of current lives
		lastLivesData.clear();
		lastLivesData.addAll(catLives);
	}

	public void showAlertAndQuit (String msg)
	{
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Error");
		alert.setContentText(msg);
		alert.setOnCloseRequest(e ->
		{
			System.exit(1);
		});
		alert.show();
	}


	public void mainMenu (ActionEvent event)
	{
		clientConnection.close();
		clientConnection = null;
		CategoryUI.getScene().setRoot(StartingClass.mainRoot);
	}

	/*

	This class is responsible for displaying the word categories for user to pick from.

 */
	private static class CategorySelector
	{
		private final EventHandler<MouseEvent> event; // what happens when user clicked on a category

		public CategorySelector (EventHandler<MouseEvent> event)
		{
			this.event = event;
		}

		/*
			Creates a single category display.
			If category completed, lives should be equal to 100 + lives left.
			So if category was completed with 2 lives left, this value should be equal 102.
			If some category failed, then lives for the rest of categories are += 1000 to disable them
		*/
		private AnchorPane createCategory (String name, int lives, int lastLives)
		{
			AnchorPane newCat = new AnchorPane();
			newCat.getStyleClass().add("categoryContainerHBox");
			newCat.setVisible(true);
			newCat.setId(name);

			// remember that if category is completed, 100 is added to lives remaining
			if (lives > 0 && lives < 100)
				newCat.setOnMouseClicked(event);

			Label newCatName = new Label(name);
			newCatName.getStyleClass().add("categoryTitle");

			HBox heartContainer = new HBox();

			int loopLimit = lastLives % 100;
			if (lastLives > 1000)
				loopLimit = lastLives % 1000;

			ArrayList<ImageView> hearts = new ArrayList<>();

			// add previous number of lives
			// because they can only reduce, we will delete the extra hearts
			for (int i = 0; i < loopLimit; i++)
			{
				ImageView newImgV = new ImageView(StartingClass.heartImg);
				newImgV.setPreserveRatio(true);
				newImgV.setFitHeight(50);
				newImgV.getStyleClass().add("categoryHeartImageView");

				HBox.setMargin(newImgV, new Insets(5, 0, 0, 0));
				heartContainer.getChildren().add(newImgV);
				hearts.add(newImgV);
			}

			int livesThatShouldBeDisplayed = lives % 100;
			if (livesThatShouldBeDisplayed > 10)
				livesThatShouldBeDisplayed = lives % 1000;

			if (livesThatShouldBeDisplayed < loopLimit)
			{
				ImageView liveToDelete = hearts.get(0);

				PauseTransition pause = new PauseTransition(Duration.millis(250));
				pause.setOnFinished(e ->
				{
					liveToDelete.setImage(StartingClass.heartBrokenImg);

					FadeTransition fadeOut = new FadeTransition(Duration.millis(800), liveToDelete);
					fadeOut.setOnFinished(eeee ->
					{
						heartContainer.getChildren().remove(liveToDelete);
					});
					fadeOut.setFromValue(1.0);
					fadeOut.setToValue(0.0);
					fadeOut.setDelay(Duration.millis(1500));
					fadeOut.play();
				});
				pause.play();

			}


			newCat.getChildren().addAll(newCatName, heartContainer);
			AnchorPane.setLeftAnchor(newCatName, 0.0);
			AnchorPane.setRightAnchor(heartContainer, 0.0);

			/*
				If lives == 0 this cat failed
				If lives > 1000 some other cat failed
				Else then this cat available.
			 */
			if (lives <= 0 || lives > 100)
			{
				newCat.getStyleClass().add("categoryContainerHBoxDisabled");

				if (lives <= 0)
				{
					newCat.getStyleClass().add("categoryContainerHBoxFailed");
				}
				else if (lives < 1000)
				{
					newCat.getStyleClass().add("categoryContainerHBoxCompleted");
				}
			}

			return newCat;
		}

		/*
			Given array of words and array of lives left displays the info on the screen
		 */
		public void showCategories (VBox categoriesParent, ArrayList<String> catNames, ArrayList<Integer> catLives, ArrayList<Integer> lastCatLives)
		{
			categoriesParent.getChildren().clear();

			for (int i = 0; i < catNames.size(); i++)
			{
				AnchorPane newCat = createCategory(catNames.get(i), catLives.get(i), lastCatLives.get(i));
				VBox.setMargin(newCat, new Insets(10));
				categoriesParent.getChildren().add(newCat);
			}

			categoriesParent.setVisible(true);
		}
	}
}
