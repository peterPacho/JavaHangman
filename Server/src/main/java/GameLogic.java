import com.sun.org.apache.xpath.internal.operations.Bool;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.function.Consumer;

/*

	Each client thread gets its own GameLogic object.

 */

public class GameLogic
{
	private final ArrayList<WordBank> wordCategories;
	private WordBank.Word currentWord; // what is the last word that was send to the client
	private String currentCategory;
	private final Consumer<Serializable> log;
	private final int id;

	public GameLogic (Consumer<Serializable> log, ArrayList<ArrayList<String>> categories, int id)
	{
		this.log = log;
		this.id = id;

		// generate the data for this user
		wordCategories = new ArrayList<>();

		// if there are more categories than value in StartUI, pick random numbers.
		if (categories.size() > StartUI.maxCategoriesToSendToClient)
		{
			// create array with all available indexes
			ArrayList<Integer> indexes = new ArrayList<>();
			for (int i = 0; i < categories.size(); i++)
			{
				indexes.add(i);
			}

			// remove random indexes until left with maxCategories
			while (indexes.size() > StartUI.maxCategoriesToSendToClient)
			{
				indexes.remove((int) (Math.random() * indexes.size() - 1));
			}

			// add what's left to
			indexes.forEach(number ->
			{
				wordCategories.add(new WordBank(categories.get(number)));
			});
		}
		// if not then just copy them all
		else
		{
			for (ArrayList<String> words : categories)
			{
				wordCategories.add(new WordBank(words));
			}
		}
	}

	/*
		Communication between client and server works mostly by sending plain text.
	*/
	public Object processClientRequest (String request)
	{
		if (request.equals("GET:CATEGORIES"))
			return getCategories();

		else if (request.contains("GET:CATEGORY;"))
			return getCategoryByName(request.substring(request.indexOf(";") + 1));

		else if (request.contains("KEY:PRESSED;"))
			return wordLetterGuessed(request.substring(request.indexOf(";") + 1));


		// if reached this point then no other if was true
		log.accept("Client " + id + " unknown request received: \"" + request + "\".$$red");
		return "ERR";
	}

	private Object getCategories ()
	{
		// check if some cat failed, if so then lock all others by incrementing lives remaining by 1000
		// also, if current category has no more lives, lock all other cats
		WordBank failedCat = null;
		for (WordBank bank : wordCategories)
		{
			if (bank.triesLeft > 1000)
				break;

			if (bank.triesLeft == 0)
			{
				failedCat = bank;
				break;
			}
		}

		if (failedCat != null)
		{
			for (WordBank bank : wordCategories)
			{
				if (bank != failedCat)
				{
					if (bank.triesLeft < 10)
						bank.triesLeft += 1000;
				}
			}
		}


		StringBuilder toReturn = new StringBuilder();
		for (WordBank singleCategory : wordCategories)
		{
			toReturn.append(singleCategory.categoryName).append("-").append(singleCategory.triesLeft).append(";");
		}
		log.accept("Client " + id + " requested category names$$gray");
		return toReturn.toString();
	}

	private Object getCategoryByName (String catName)
	{
		WordBank categoryRequested = null;

		// try to find this category on the list
		for (WordBank categories : wordCategories)
		{
			if (categories.categoryName.equals(catName))
			{
				categoryRequested = categories;
			}
		}

		// not found
		if (categoryRequested == null)
		{
			log.accept("Client " + id + " requested not-existing category (" + catName + ").$$red");
			return "ERR";
		}

		// this should never happen as button on the client side should be disabled if lives go to 0
		if (categoryRequested.triesLeft <= 0)
		{
			log.accept("Client " + id + " requested category \"" + catName + "\" but it has 0 tries left.$$gray");
			return "ZERO_LIVES";
		}

		// remove that word from the array list so it's never send again to this client
		currentWord = categoryRequested.getRandomWord();
		currentCategory = categoryRequested.categoryName;

		// send stripped version of that Word object to the client
		WordBank.WordForClient wordToReturn = new WordBank.WordForClient(currentWord, currentCategory);
		log.accept("Client " + id + " requested random word from \"" + categoryRequested.categoryName + "\" (" + categoryRequested.words.size() + " left) $$gray");
		categoryRequested.triesLeft--;

		log.accept("(The word is \"" + currentWord.word + "\" but don't tell anybody)$$gray");

		return wordToReturn;
	}

	/*
		Single key, uppercase, received from the client
	 */
	private Object wordLetterGuessed (String key)
	{
		key = key.toUpperCase(); // that shouldn't be needed

		// check if that key available (it should be but safety reasons)
		if (!currentWord.lettersAvailable.contains(key) || key.length() != 1)
		{
			log.accept("Client " + id + " requested not valid key (" + key + ")$$red");
		}
		else
		{
			// fill the word for client with letters if key is correct
			Integer indexOf = -1;
			while (currentWord.word.toUpperCase().substring(indexOf + 1).contains(key))
			{
				indexOf = currentWord.word.toUpperCase().indexOf(key, indexOf + 1);
				currentWord.wordForClient = currentWord.wordForClient.substring(0, indexOf) + currentWord.word.charAt(indexOf) + currentWord.wordForClient.substring(indexOf + 1);
			}

			if (indexOf == -1)
				currentWord.triesLeft--;

			// replace the key in the lettersAvailable string with lower case
			// since it was used already
			int index = currentWord.lettersAvailable.indexOf(key);
			currentWord.lettersAvailable = currentWord.lettersAvailable.substring(0, index) + key.toLowerCase() + currentWord.lettersAvailable.substring(index + 1);


			String result = "correct guess";
			if (indexOf == -1)
				result = "not found";
			log.accept("Client " + id + " requested letter " + key + ". It was " + result + ". Lives left: " + currentWord.triesLeft + "$$gray");
		}

		// if after all that lives == 0, return the whole word
		// letters that were not guessed are sent lower case
		// client will print lower case letter differently
		if (currentWord.triesLeft <= 0)
		{
			StringBuilder str = new StringBuilder();

			for (int i = 0; i < currentWord.word.length(); i++)
			{
				String toAppend = String.valueOf(currentWord.word.charAt(i));

				// if this position was guesses incorrectly
				if (currentWord.word.charAt(i) != currentWord.wordForClient.charAt(i))
				{
					toAppend = toAppend.toLowerCase();
				}

				str.append(toAppend);
			}

			currentWord.wordForClient = str.toString();
		}

		// if whole word guessed correctly, mark that category as won
		else if (!currentWord.wordForClient.contains("."))
		{
			log.accept("Client " + id + " guessed word in category " + currentCategory + " correctly.$$gray");

			for (WordBank bank : wordCategories)
			{
				if (bank.categoryName.equals(currentCategory))
				{
					bank.triesLeft += 101;
					break;
				}
			}

			currentWord.triesLeft += 100;
		}

		return new WordBank.WordForClient(currentWord, currentCategory);
	}
}
