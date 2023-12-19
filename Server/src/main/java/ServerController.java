import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import jdk.javadoc.internal.tool.Start;

import javax.print.DocFlavor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class ServerController implements Initializable
{
	/*
		This array list contains all categories and words that user can select.
		It puts each line of the file into the ArrayList<String> and then each file is put
		into ArrayList of those ArrayLists.

		Files are prepared that first line is the category name, and each next line is the word
		that player has to guess. Those words are later send to WordBank class.
	 */
	public static ArrayList<ArrayList<String>> categories;

	private long startTime; // used to show uptime

	// log window stuff
	private boolean autoScroll = true;
	private boolean printTime = true;
	private Clipboard clipboard;
	private EventHandler<MouseEvent> logLabelEvent;
	private Tooltip logLabelTip;

	int totalClients = 0;
	int connectedClients = 0;

	@FXML
	Label label_port, label_uptime, label_clients, label_totalClients;
	@FXML
	VBox log_vbox;
	@FXML
	ScrollPane log_container;
	@FXML
	Button button_autoscroll, button_print_time;

	Server server;


	/*
		Used to print time.
		Prepends 0 to the front if value is less than 10.
	 */
	private String timeHelper (long value)
	{
		if (value < 10)
			return "0" + value;
		return String.valueOf(value);
	}

	/*
		Converts the time in ms to hh:mm:ss.
		Used to display runtime (current time - start time) is sent to this function.
		Output is sent to the label.
	 */
	private String getTime (long time)
	{
		long runTimeSeconds = time / 1000;
		long runTimeMinutes = runTimeSeconds / 60;
		long runTimeHours = runTimeMinutes / 60;

		runTimeMinutes = runTimeMinutes % 60;
		runTimeSeconds = runTimeSeconds % 60;

		return timeHelper(runTimeHours) + ":" + timeHelper(runTimeMinutes) + ":" + timeHelper(runTimeSeconds);
	}

	/*
		Creates new label and adds it to the VBox.
	 */
	public void addToLog (String text)
	{
		addToLog(text, "black");
	}

	public void addToLog (String text, String color)
	{
		String prefix = "";

		// if enabled, prepends the current date and time to the log entry
		if (printTime && !text.isEmpty())
		{
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
			LocalDateTime now = LocalDateTime.now();
			prefix = dtf.format(now) + ": ";
		}

		Label newEntry = new Label(prefix + text);
		newEntry.setOnMouseClicked(logLabelEvent);
		newEntry.setTextFill(Color.valueOf(color));
		newEntry.setTooltip(logLabelTip);

		log_vbox.getChildren().add(newEntry);
	}

	/*
		Handles the communication between server threads and the UI thread.

		To specify color, message should have $$color at the end.
	 */
	public void processReceivedData (String data)
	{
		// check if color specified
		if (data.contains("$$"))
		{
			addToLog(data.substring(0, data.indexOf("$$")), data.substring(data.indexOf("$$") + 2));
		}
		else
		{
			addToLog(data);
		}

		// check for extra meaning
		if (data.contains("New client connected"))
		{
			totalClients++;
			connectedClients++;

			label_totalClients.setText("Total clients: " + totalClients);
			label_clients.setText("Connected clients: " + connectedClients);
		}
		else if (data.contains(" disconnected."))
		{
			connectedClients--;
			label_clients.setText("Connected clients: " + connectedClients);
		}
	}

	@Override
	public void initialize (URL location, ResourceBundle resources)
	{
		label_port.setText("Listening on port " + StartUI.port);

		log_vbox.heightProperty().addListener(e ->
		{
			if (autoScroll)
				log_container.setVvalue(1.0);
		});

		// this will display the server's uptime
		// https://stackoverflow.com/questions/44060204/javafx-label-will-not-continuously-update
		startTime = System.currentTimeMillis();
		Timer uptime = new Timer();
		uptime.schedule(new TimerTask()
		{
			@Override
			public void run ()
			{
				Platform.runLater(() -> label_uptime.setText("Server uptime: " + getTime(System.currentTimeMillis() - startTime)));
			}
		}, 1000, 1000);

		clipboard = Clipboard.getSystemClipboard();

		// event handler for the labels that make up the log
		// double mouse click to copy the label / log entry to the clipboard
		logLabelEvent = event ->
		{
			if (event.getButton().equals(MouseButton.PRIMARY))
			{
				if (event.getClickCount() == 2)
				{
					Label labelThatWasClicked = (Label) event.getSource();

					ClipboardContent clipCon = new ClipboardContent();
					clipCon.putString(labelThatWasClicked.getText());
					clipboard.setContent(clipCon);
				}
			}
		};

		// tooltip that will be attached to each label that make up the log
		logLabelTip = new Tooltip("Double click to copy to clipboard");
		logLabelTip.setShowDelay(Duration.millis(300));

		// try to load text files
		try
		{
			JavaDoesntLetMePassIntegerByReferenceSoImWrappingItWithThisClass catC = new JavaDoesntLetMePassIntegerByReferenceSoImWrappingItWithThisClass();
			JavaDoesntLetMePassIntegerByReferenceSoImWrappingItWithThisClass wordC = new JavaDoesntLetMePassIntegerByReferenceSoImWrappingItWithThisClass();

			loadCategories(catC, wordC);

			if (catC.number == 0)
			{
				addToLog("Couldn't load any word category file.", "red");
				addToLog("Server will run but clients won't work.");
			}

			addToLog("Word data files loaded (" + wordC.number + " words in " + catC.number + " categories).");

			if (catC.number > StartUI.maxCategoriesToSendToClient)
			{
				addToLog("Maximum number of categories to send to client set to " + StartUI.maxCategoriesToSendToClient, "gray");
				addToLog("Server will pick random categories for users", "gray");
			}
		}
		catch (IOException e)
		{
			addToLog("Couldn't load word data files!", "red");
			throw new RuntimeException(e);
		}

		// try to start the server
		server = new Server(data -> Platform.runLater(() -> processReceivedData(data.toString())));
	}

	// Log functions
	public void autoscrollToggleButtonClick (ActionEvent event)
	{
		autoScroll = !autoScroll;

		if (autoScroll)
			button_autoscroll.setText("Auto-scroll ON");
		else
			button_autoscroll.setText("Auto-scroll OFF");
	}

	public void clearLogButtonClick (ActionEvent event)
	{
		log_vbox.getChildren().clear();
	}

	public void printTimeButtonClick (ActionEvent event)
	{
		printTime = !printTime;
		if (printTime)
			button_print_time.setText("Print time ON");
		else
			button_print_time.setText("Print time OFF");
	}

	// Load the text files into the "2D array"
	private void loadCategories (JavaDoesntLetMePassIntegerByReferenceSoImWrappingItWithThisClass categoriesCounter, JavaDoesntLetMePassIntegerByReferenceSoImWrappingItWithThisClass wordsCounter) throws IOException
	{
		categories = new ArrayList<>();
		ArrayList<String> wordsThatAreTooLong = new ArrayList<>();

		int counter = 1;
		while (true)
		{
			InputStream is = this.getClass().getResourceAsStream("/words/category" + counter + ".txt");

			if (is == null)
			{
				break;
			}

			categories.add(new ArrayList<>());
			categoriesCounter.number++;
			ArrayList<String> words = categories.get(categories.size() - 1);
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));

			String word;
			boolean dontCapitalizeCatName = true;
			while ((word = reader.readLine()) != null)
			{
				if (dontCapitalizeCatName)
				{
					words.add(word);
					dontCapitalizeCatName = false;
				}
				else
				{
					words.add(word.toUpperCase());
					wordsCounter.number++;

					if (word.length() > StartUI.recommendedWordLength)
						wordsThatAreTooLong.add(words.get(0) + " : " + word);
				}
			}

			counter++;

			is.close();
		}

		if (!wordsThatAreTooLong.isEmpty())
		{
			addToLog("Warning: At least one word has length over " + StartUI.recommendedWordLength + " characters.", "gray");
			addToLog("List of too long words (<category> : <word>):", "gray");

			wordsThatAreTooLong.forEach(word -> addToLog(word, "gray"));
			addToLog("");
		}
	}

	// Nobody reads the code anyway, right?
	private static class JavaDoesntLetMePassIntegerByReferenceSoImWrappingItWithThisClass
	{
		public Integer number = 0;
	}
}
