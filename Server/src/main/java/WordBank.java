import java.io.Serializable;
import java.util.ArrayList;

/*

		Generates the data from the file.
		File should have a category name in first line
		rest of the lines are words belonging to that category.

*/
public class WordBank
{
	ArrayList<Word> words;
	String categoryName;

	// how many tries left in this category
	int triesLeft = 3;

	/*

	It takes an array of string, where first element is category name and rest
	are different words that user can guess. Array is prepared by loadCategories() from ServerController class.

	 */
	public WordBank (ArrayList<String> wordsArr)
	{
		this.categoryName = wordsArr.get(0);

		this.words = new ArrayList<>();
		for (int i = 1; i < wordsArr.size(); i++)
		{
			words.add(new Word(wordsArr.get(i), i - 1));
		}
	}

	/*

		Returns a random word that has triesLeft > 0
		Assumes there are words left, and that means there must be at least
		as many words as there are lives (project description specifies 3 lives)

		Returned word is also removed from the array list so it can't be reused.
		Server must save it so it can process client's requests.
	 */
	public Word getRandomWord ()
	{
		return words.remove((int) (Math.random() * words.size()));
	}

	public void removeWord (int id)
	{
		for (int i = 0; i < words.size(); i++)
		{
			if (words.get(i).id == id)
			{
				words.remove(i);
				return;
			}
		}
	}

	/*

		Smaller version of Word class that will be sent to the client.
		Has stripped all info that client doesn't need.

		Client program needs copy of this class to interpret the message.

		Keyboard is kept differently - upper case means key is available, lower case means it's not, ';' means page break
		(when printing the key buttons)

	 */
	public static class WordForClient implements Serializable
	{
		public int id;
		public String wordForClient;
		public String wordCategory;
		public Integer triesLeft;
		public String lettersAvailable;

		public WordForClient (Word word, String wordCategory)
		{
			this.id = word.id;
			this.wordForClient = word.wordForClient;
			this.triesLeft = word.triesLeft;
			this.lettersAvailable = word.lettersAvailable;
			this.wordCategory = wordCategory;
		}

		public WordForClient (int id, String wordForClient, String wordCategory, Integer triesLeft, String lettersAvailable)
		{
			this.id = id;
			this.wordForClient = wordForClient;
			this.wordCategory = wordCategory;
			this.triesLeft = triesLeft;
			this.lettersAvailable = lettersAvailable;
		}

		@Override
		public String toString ()
		{
			return "WordForClient{id=" + id + ",wordForClient=" + wordForClient + ",wordCategory=" + wordCategory + ",triesLeft=" + triesLeft + ",lettersAvailable=" + lettersAvailable + "}";
		}
	}

	/*

	Contains data for a single word:
	word - the word that user needs to guess
	wordForClient - data that will be sent to user, dot mean letter not guesses yet, _ means space
	tiesLeft - how many letters can user pick
	lettersAvailable - list of all letters on a keyboard, upper case means it's available to user to select
    */
	public static class Word
	{
		public int id;
		public String word;
		public String wordForClient;
		public Integer triesLeft;
		public String lettersAvailable;

		Word (String word, int id)
		{
			this.word = word;
			this.id = id;

			StringBuilder wordForClientBuilder = new StringBuilder();
			for (int i = 0; i < word.length(); i++)
			{
				Character ch = word.charAt(i);

				if (ch.equals(' '))
					wordForClientBuilder.append("_");
				else
					wordForClientBuilder.append(".");
			}

			this.wordForClient = wordForClientBuilder.toString();
			this.triesLeft = 6;
			this.lettersAvailable = StartUI.keys;
		}
	}
}

