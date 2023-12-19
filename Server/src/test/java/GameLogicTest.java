import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GameLogicTest
{
	GameLogic gm;

	// those are required for the GameLogic class to function
	Consumer<Serializable> log;
	static ArrayList<ArrayList<String>> categories;

	@BeforeAll
	static void setup ()
	{
		// creates a dummy list of single category and some words to guess
		categories = new ArrayList<>();
		categories.add(new ArrayList<>());
		categories.get(0).addAll(Arrays.asList("catName", "word", "word", "word"));
	}

	@BeforeEach
	void setup2 ()
	{
		log = new Consumer<Serializable>()
		{
			@Override
			public void accept (Serializable serializable)
			{

			}
		};

		gm = new GameLogic(log, categories, 0);
	}

	@Test
	void testUnknownCommand ()
	{
		assertEquals("ERR", gm.processClientRequest("some msg that is not valid").toString(), "Not valid msg didn't return ERR");
	}

	@Test
	void testGetCategories ()
	{
		assertEquals("catName-3;", gm.processClientRequest("GET:CATEGORIES").toString(), "Returned wrong category name / lives string");
	}

	@Test
	void testGetWord ()
	{
		assertEquals("ERR", gm.processClientRequest("GET:CATEGORY;wrongCatName").toString(), "Wrong cat name didn't return error");
	}

	@Test
	void testGetWord2 ()
	{
		// this should return a single word from the catName
		WordBank.WordForClient wc = (WordBank.WordForClient) gm.processClientRequest("GET:CATEGORY;catName");

		assertEquals(6, wc.triesLeft);
		assertEquals("....", wc.wordForClient);
		assertEquals("catName", wc.wordCategory);
	}

	@Test
	void testLetterGuessedWrong ()
	{
		gm.processClientRequest("GET:CATEGORY;catName");
		WordBank.WordForClient wc = (WordBank.WordForClient) gm.processClientRequest("KEY:PRESSED;A");

		assertEquals("....", wc.wordForClient);
		assertEquals(5, wc.triesLeft);
	}

	@Test
	void testLetterGuessedRight ()
	{
		gm.processClientRequest("GET:CATEGORY;catName");
		WordBank.WordForClient wc = (WordBank.WordForClient) gm.processClientRequest("KEY:PRESSED;W");

		assertEquals("w...", wc.wordForClient);
		assertEquals(6, wc.triesLeft);
	}

	@Test
	void testWordGuessed ()
	{
		// normal game progress
		gm.processClientRequest("GET:CATEGORY;catName");
		WordBank.WordForClient wc = (WordBank.WordForClient) gm.processClientRequest("KEY:PRESSED;W");

		assertEquals("w...", wc.wordForClient);
		assertEquals(6, wc.triesLeft);

		wc = (WordBank.WordForClient) gm.processClientRequest("KEY:PRESSED;A");

		assertEquals("w...", wc.wordForClient);
		assertEquals(5, wc.triesLeft);

		wc = (WordBank.WordForClient) gm.processClientRequest("KEY:PRESSED;O");

		assertEquals("wo..", wc.wordForClient);
		assertEquals(5, wc.triesLeft);

		wc = (WordBank.WordForClient) gm.processClientRequest("KEY:PRESSED;Q");

		assertEquals("wo..", wc.wordForClient);
		assertEquals(4, wc.triesLeft);

		wc = (WordBank.WordForClient) gm.processClientRequest("KEY:PRESSED;D");

		assertEquals("wo.d", wc.wordForClient);
		assertEquals(4, wc.triesLeft);

		wc = (WordBank.WordForClient) gm.processClientRequest("KEY:PRESSED;R");

		assertEquals("word", wc.wordForClient);
		assertEquals(104, wc.triesLeft);
	}

	@Test
	void testCatLiveDec ()
	{
		// requesting the word form some cat should decrease its lives by 1
		gm.processClientRequest("GET:CATEGORY;catName");

		assertEquals("catName-2;", gm.processClientRequest("GET:CATEGORIES").toString());
	}

	@Test
	void testCatFailed ()
	{
		// use up 3 lives
		for (int i = 0; i < 3; i++)
			gm.processClientRequest("GET:CATEGORY;catName");

		// request categories check if 0 lives for that cat
		assertEquals("catName-0;", gm.processClientRequest("GET:CATEGORIES").toString());
	}

	@Test
	void testNotValidKey ()
	{
		gm.processClientRequest("GET:CATEGORY;catName");

		WordBank.WordForClient wc = (WordBank.WordForClient) gm.processClientRequest("KEY:PRESSED;-");

		assertEquals(6, wc.triesLeft);
		assertEquals("....", wc.wordForClient);
	}
}
