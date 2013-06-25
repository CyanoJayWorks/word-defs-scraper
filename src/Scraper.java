import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;


public class Scraper {
	private final static String API_CALL_PREFIX = "http://www.google.com/dictionary/json?callback=dict_api.callbacks.id100&q=";
	private final static String API_CALL_SUFFIX = "&sl=en&tl=en&restrict=pr%2Cde&client=te";
	
	private HttpClient client;
	private List<String> wordList;
	private Map<String, String> worddefpairs;
	private String inputFile = "wordlist/sat-barrons-main.txt";
	private String outputFile = "output/" + inputFile.substring(inputFile.indexOf('/'), inputFile.indexOf('.')) + "-defs.txt";
	
	public static void main(String args[]) {
		new Scraper().start();
	}
	
	private void start() {
		populateWordList();
		mapWordsToDefinitions();
	}

	private void populateWordList() {
		wordList = new ArrayList<String>();
		Scanner s = null;
		try {
			s = new Scanner(new File(inputFile));
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
		
		JsonParser parser = new JsonParser();
		String def = null;
		File output = new File(outputFile);
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(output, true));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for(String word : wordList) {
			try {
				Thread.sleep(5000);
			} catch(Exception e) {
				e.printStackTrace();
			}
			String json = getWordDataJson(word);
			int idx = 1;
			boolean success = true;
			do {
				try {
					JsonArray temp = null;
					try {
						temp = parser.parse(json)
							.getAsJsonObject().get("primaries")
							.getAsJsonArray().get(0)
							.getAsJsonObject().get("entries")
							.getAsJsonArray();
					} catch(Exception e) {
						e.printStackTrace();
						temp = parser.parse(json)
								.getAsJsonObject().get("webDefinitions")
								.getAsJsonArray().get(0)
								.getAsJsonObject().get("entries")
								.getAsJsonArray();
					}
					def = temp.get(idx--)
							.getAsJsonObject().get("terms")
							.getAsJsonArray().get(0)
							.getAsJsonObject().get("text")
							.getAsString();
					success = true;
				} catch(Exception e) {
					success = false;
				}
			} while(!success);
			
			Fl.og(word + " : " + def);
			
			worddefpairs.put(word, def);
			
			String line = "\"" + word + "\", \"" + def + "\"\n";
			writer.write(line);
			writer.flush();
		}
		
		writer.close();
	}
	
	private String getWordDataJson(String word) {
		HttpGet get = new HttpGet(API_CALL_PREFIX + word + API_CALL_SUFFIX);
		String json = null;
		
		try {
			HttpResponse response = client.execute(get);

			HttpEntity entity = response.getEntity();

			if (entity != null) json = getString(entity);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
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
		int endIndex = returnString.lastIndexOf(',') - 4;
		returnString = returnString.substring(25, endIndex);
		return returnString;
	}
}
