/*

	This controls the word guessing part of the game.

 */

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.Transition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.*;

public class GuessUIController implements Initializable
{
	// references needed to change UI / refresh
	public WordBank.WordForClient gameData;
	public Parent CategoryUI;
	public ClientConnection clientConnection = null;
	public Label roundResult;
	public Button buttonBack;

	private EventHandler<MouseEvent> keyClicked;

	// this class' objects
	public GridPane GuessUI;
	public CategoryUIController categoryUIController;
	public Label labelCategory;
	public HBox keyboard1;
	public HBox keyboard2;
	public HBox keyboard3;
	public VBox heartContainer;
	public HBox letterContainerMaster;

	// font and label sizes for the word that user needs to guess
	// those will be reduced if word is too long to fit in screen
	int wordFontSize = 40;
	int wordLabelSize = 50;
	int wordMargin = 5;

	// this is just a array list of those keyboardN HBox, to make looping easier
	ArrayList<HBox> keyboard;
	ArrayList<ImageView> hearts;
	HashMap<String, Label> keyboardKeys; // maps label to letter

	private void keyClicked (Label keyPressed)
	{
		// strip the event handler from this key right away
		// so it can't be pressed twice
		keyPressed.setOnMouseClicked(null);

		String keyVal = keyPressed.getText();

		// make the backup of current lives
		// if not changed after then this guess was correct
		Integer triesBefore = gameData.triesLeft;

		// send info to the server
		clientConnection.sendMsg("KEY:PRESSED;" + keyVal);

		gameData = (WordBank.WordForClient) clientConnection.getObject();
		if (gameData == null)
		{
			System.out.println("ERR: Object received from server is null.");
			// re-enable the key so user can click again
			keyPressed.setOnMouseClicked(keyClicked);
			return;
		}

		// animate key according to the result
		animateKey(keyPressed, Objects.equals(triesBefore, gameData.triesLeft) || gameData.triesLeft > 100);

		// update the displayed word
		createWordLetters();

		// update hearts
		animateHeart(gameData.triesLeft);

		// if lives == 0 disable all keys
		if (gameData.triesLeft <= 0 || gameData.triesLeft >= 100)
		{
			for (HBox container : keyboard)
			{
				for (Node singleKey : container.getChildren())
				{
					singleKey.getStyleClass().remove("keyboardKeyEnabled");
					singleKey.getStyleClass().add("keyboardKeyDisabled");
					singleKey.setOnMouseClicked(null);
				}
			}

			buttonBack.setText("Go back");

			if (gameData.triesLeft <= 0)
			{
				roundResult.setText("You lost this round");
				buttonBack.setTooltip(new Tooltip("Maybe next time?"));
				roundResult.setStyle("-fx-text-fill: red;");
			}
			else if (gameData.triesLeft == 106)
			{
				roundResult.setText("! FLAWLESS VICTORY !");
				buttonBack.setText("Go back my champion");
				buttonBack.setTooltip(new Tooltip("We are the champions, my friends\n" + "And we'll keep on fighting till the end\n" + "We are the champions\n" + "We are the champions\n" + "No time for losers\n" + "'Cause we are the champions of the World"));
			}
			else
			{
				buttonBack.setTooltip(new Tooltip("You deserve a break"));

				if (gameData.triesLeft == 101)
					roundResult.setText("You win (that was close)");
				else
					roundResult.setText("You win");
			}
		}

	}

	@Override
	public void initialize (URL url, ResourceBundle resourceBundle)
	{
		keyboard = new ArrayList<>(Arrays.asList(keyboard1, keyboard2, keyboard3));
		keyboardKeys = new HashMap<>();

		keyClicked = new EventHandler<MouseEvent>()
		{
			@Override
			public void handle (MouseEvent mouseEvent)
			{
				Label keyPressed = (Label) mouseEvent.getSource();

				keyClicked(keyPressed);
			}
		};

	}

	// when user clicks the key and server responds, animate this key
	// depending whether click was correct or not
	// also disables the key
	private void animateKey (Label keyToChange, Boolean guessedCorrectly)
	{
		keyToChange.getStyleClass().remove("keyboardKeyEnabled");
		keyToChange.getStyleClass().add("keyboardKeyDisabled");
		keyToChange.setOnMouseClicked(null);

		String styleToApply = "keyboardKeyDisabledCorrect";
		if (!guessedCorrectly)
			styleToApply = "keyboardKeyDisabledFailed";
		keyToChange.getStyleClass().add(styleToApply);
	}

	/*
		Disables / removes last heart from the list.
		Call when # of lives reduced.
		Can reduce # of hearts only by one.

		If game is won, then triesLeft in incremented by 100, so this function
		returns immediately not reducing the # of hearts.
	 */
	private void animateHeart (int heartsLeft)
	{
		if (heartsLeft >= hearts.size() || hearts.isEmpty())
			return;

		ImageView toRemove = hearts.remove(hearts.size() - 1);
		toRemove.setImage(StartingClass.heartBrokenImg);

		FadeTransition fadeOut = new FadeTransition(Duration.millis(1000), toRemove);
		fadeOut.setDelay(Duration.millis(1000));
		fadeOut.setFromValue(1.0);
		fadeOut.setToValue(0.0);
		fadeOut.play();
	}

	/*
		Each character in gameData.wordForClient will be put into a label.
	 */
	private void createWordLetters ()
	{
		letterContainerMaster.getChildren().clear();

		for (int i = 0; i < gameData.wordForClient.length(); i++)
		{
			Label newLabel = generateSingleCharacter(gameData.wordForClient.charAt(i));
			newLabel.setStyle("-fx-font-size: " + wordFontSize + ";-fx-min-width: " + wordLabelSize + ";-fx-min-height: " + wordLabelSize + ";");

			HBox.setMargin(newLabel, new Insets(wordMargin));
			letterContainerMaster.getChildren().add(newLabel);
		}
	}

	private void createHearts ()
	{
		hearts = new ArrayList<>();

		heartContainer.getChildren().clear();
		for (int i = 0; i < gameData.triesLeft; i++)
		{
			ImageView newImgV = new ImageView(StartingClass.heartImg);
			newImgV.setPreserveRatio(true);
			newImgV.setFitHeight(40);
			newImgV.getStyleClass().add("categoryHeartImageView");

			// VBox.setMargin(newImgV, new Insets(5, 0, 0, 0));
			heartContainer.getChildren().add(newImgV);

			hearts.add(newImgV);
		}
	}

	// called from CategoryUI after setting the objects like dataFromServer
	public void init ()
	{
		labelCategory.setText("Word category \"" + gameData.wordCategory + "\":");

		// reduce the font size if word too long
		int sizeOverflow = gameData.wordForClient.length() * (wordLabelSize + 5) - StartingClass.maxWordToGuessWidth;
		if (sizeOverflow > 0)
		{
			int length = gameData.wordForClient.length();

			wordLabelSize -= sizeOverflow / length;
			wordFontSize -= sizeOverflow / length;
			wordMargin -= sizeOverflow / (length * 10);

			if (wordLabelSize < 20)
				wordLabelSize = 20;
			if (wordFontSize < 15)
				wordFontSize = 15;
			if (wordMargin < 0)
				wordMargin = 0;
		}

		createWordLetters();

		// generate keyboard
		keyboard.forEach(hbox -> hbox.getChildren().clear());
		keyboardKeys.clear();

		int keyboardRow = 0;
		int indent = 0;
		for (int i = 0; i < gameData.lettersAvailable.length(); i++)
		{
			Character singleKey = gameData.lettersAvailable.charAt(i);
			if (singleKey.equals(';'))
			{
				keyboardRow++;
				indent += 10 + (keyboardRow - 1) * 30;
			}
			else
			{
				Label newLabel = generateSingleKey(singleKey);
				HBox.setMargin(newLabel, new Insets(5, 5, 5, 5 + indent));

				if (indent != 0)
					indent = 0;

				keyboard.get(keyboardRow).getChildren().add(newLabel);
				keyboardKeys.put(String.valueOf(singleKey), newLabel);
			}
		}

		createHearts();
	}

	/*
		Returns a single label that will act as keyboard key.
		If given key is upper case label is enabled, otherwise disabled.
	 */
	private Label generateSingleKey (Character key)
	{
		Label newKey = new Label(key.toString().toUpperCase());

		newKey.getStyleClass().add("keyboardKeyMain");

		// button should be disabled
		if (Character.isLowerCase(key))
		{
			newKey.getStyleClass().add("keyboardKeyDisabled");
		}
		// button should be enabled
		else
		{
			newKey.getStyleClass().add("keyboardKeyEnabled");
			newKey.setOnMouseClicked(keyClicked);
		}

		return newKey;
	}

	/*
	Returns a single label that will act as one character in word
	If lower case then it means game is over and that letter was not guessed, so change its style
 */
	private Label generateSingleCharacter (Character key)
	{
		Label newKey = new Label();

		if (key.equals('.'))
		{
			newKey.getStyleClass().add("wordChar");
			newKey.setText("_");
		}
		else if (key.equals('_'))
		{
			newKey.setText(" ");
			newKey.getStyleClass().add("wordCharSpace");
		}
		else if (key.equals('-'))
		{
			newKey.setText("-");
			newKey.getStyleClass().add("wordCharSpace");
		}
		else
		{
			newKey.setText(key.toString().toUpperCase());
			newKey.getStyleClass().add("wordChar");

			if (Character.isLowerCase(key))
				newKey.getStyleClass().add("wordCharWrong");
			else
				newKey.getStyleClass().add("wordCharOk");
		}

		return newKey;
	}

	public void returnToCategoryUI (ActionEvent event)
	{
		GuessUI.getScene().setRoot(CategoryUI);
		categoryUIController.displayCategories();
	}

	public void keyTyped (KeyEvent keyEvent)
	{
		Label keyPressed = keyboardKeys.get(keyEvent.getCharacter().toUpperCase());

		// if event handler is null that key is disabled
		if (keyPressed != null && keyPressed.getOnMouseClicked() != null)
			keyClicked(keyPressed);

	}
}
