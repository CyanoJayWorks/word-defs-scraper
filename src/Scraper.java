import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.agopinath.lthelogutil.Fl;


public class Scraper {
	private final static String API_CALL_PREFIX = "http://glosbe.com/gapi/translate?from=eng&dest=eng&format=json&phrase=";
	private final static String API_CALL_SUFFIX = "&pretty=true";
	
	private HttpClient client;
	private List<String> wordList;
	private Map<String, String> worddefpairs;
	
	
	public static void main(String args[]) {
		new Scraper().start();
	}
	
	private void start() {
		populateWordList("wordlist/sat-barrons-main.txt");
		mapWordsToDefinitions();
		outputWordDefinitionPairs();
	}

	private void populateWordList(String wordFile) {
		wordList = new ArrayList<String>();
		Scanner s = null;
		try {
			s = new Scanner(new File(wordFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		while(s.hasNextLine()) {
			String word = s.nextLine().trim();
			wordList.add(word);
		}
	}

	private void mapWordsToDefinitions() {
		worddefpairs = new HashMap<String, String>();
		client = new DefaultHttpClient();
		
		for(String word : wordList) {
			String gson = getWordDataGson(word);
			
		}
	}
	
	private String getWordDataGson(String word) {
		HttpGet get = new HttpGet(API_CALL_PREFIX + word + API_CALL_SUFFIX);
		String json = null;
		
		try {
			HttpResponse response = client.execute(get);
			Fl.og("Received response from GET for word: " + word);

			HttpEntity entity = response.getEntity();

			if (entity != null) json = getString(entity);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IOException from HTTP error");
			e.printStackTrace();
		}
		
		return json;
	}
	
	private String getString(HttpEntity entity) {
		String returnString = null;

		BufferedReader br = null;

		try {
			br = new BufferedReader(new InputStreamReader(entity.getContent()));

			String currentLine = "";

			if ((currentLine = br.readLine()) != null) {
				returnString = currentLine;
				
				while ((currentLine = br.readLine()) != null)
					returnString += ("\n" + currentLine);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return returnString;
	}
	
	private void outputWordDefinitionPairs() {
		
	}
}
