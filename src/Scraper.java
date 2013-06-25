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
		outputWordDefinitionPairs();
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
		
		//int j = 0;
		for(String word : wordList) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			String json = getWordDataJson(word);
			int idx = 0;
			boolean success = true;
			do {
				try {
					def = parser.parse(json)
							.getAsJsonObject().get("primaries")
							.getAsJsonArray().get(0)
							.getAsJsonObject().get("entries")
							.getAsJsonArray().get(idx++)
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
			
			/*int count = 0;
			for(JsonElement result : defs) {
				String defText = result.getAsJsonObject().get("text").getAsString();
				String currDefs = worddefpairs.get(word);
				if(currDefs != null) {
					worddefpairs.put(word, currDefs + "\n\t " + (count+1) + ". " + defText);
				} else {
					worddefpairs.put(word, (count+1) + ". " + defText);
				}
				
				count++;
				if(count == numDefsWanted) break;
			}*/
			
			//j++;
			//if(j == 30) break;
		}
		
		/*for(String word : worddefpairs.keySet()) {
			String def = worddefpairs.get(word);
			Fl.og(String.format("%2s : %2s", word, def));
		}*/
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
		int endIndex = returnString.lastIndexOf(',') - 4;
		returnString = returnString.substring(25, endIndex);
		System.out.println(returnString);
		return returnString;
	}
	
	private void outputWordDefinitionPairs() {
		File output = new File(outputFile);
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(output));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for(String word : worddefpairs.keySet()) {
			String def = worddefpairs.get(word);
			String line = word + ", " + def + "\n";
			Fl.og(line);
			writer.write(line);
		}
	}
}
