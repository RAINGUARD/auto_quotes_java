package games;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import main.API;
import preferences.ScrambleQuotePreferences;

/**
 * @author neilh
 * This class receives a quote and generates a puzzle grid and solution grid of the type stripper quote
 */

public class ScrambleQuote {
	
	private String[][] bankGrid;
	private ArrayList<String> baseChars = new ArrayList<String>();
	private ArrayList<String> logicalChars = new ArrayList<String>();
	private API api = new API();
	
	public ScrambleQuote(String quote) throws SQLException, IOException {
		
		generateLogicalChars(quote);
		createLetterBank(quote);
	}

	//method creates the logicalChars arraylist as well as initializing important variables such as row count...
	private void generateLogicalChars(String quote) throws UnsupportedEncodingException, SQLException {
		int index = 0;
		int rows = 1;
		
		String newQuote = quote.trim();
		ScrambleQuotePreferences.LENGTH = api.getLength(newQuote);
		ArrayList<String> tempList = api.getLogicalChars(newQuote);
		
		initializeVariables();
		
		//removing duplicate spaces
		String prevChar = tempList.get(0);
		logicalChars.add(prevChar);
		for(int i = 1; i<tempList.size(); i++) {
			if(!(tempList.get(i).equals(prevChar) && tempList.get(i).equals(" "))) {
				logicalChars.add(tempList.get(i));
				index++;
			}
			
			if(index>=ScrambleQuotePreferences.COLUMNS) {
				rows++;
				index = 0;
			}
			prevChar = tempList.get(i);
		}
		
		ScrambleQuotePreferences.ROWS = rows;
		int totalHeight = (ScrambleQuotePreferences.ROWS*ScrambleQuotePreferences.CELL_HEIGHT)+((ScrambleQuotePreferences.ROWS-1)*45);
		ScrambleQuotePreferences.STARTING_Y = (540-totalHeight)/2;
	}

	private void initializeVariables() {
		if(ScrambleQuotePreferences.LENGTH>15) {
			ScrambleQuotePreferences.COLUMNS = 16;
		} else {
			ScrambleQuotePreferences.COLUMNS = ScrambleQuotePreferences.LENGTH;
		}
		
		ScrambleQuotePreferences.CELL_WIDTH = 4*(26-ScrambleQuotePreferences.COLUMNS);
		ScrambleQuotePreferences.CELL_HEIGHT = ScrambleQuotePreferences.CELL_WIDTH;
		ScrambleQuotePreferences.GRID_FONT_SIZE = 1.6*(28-ScrambleQuotePreferences.COLUMNS);
		
	}

	//method creates the arraylist used for the clue portion of the puzzle
	private void createLetterBank(String quote) throws UnsupportedEncodingException, SQLException {
		for(int i = 0; i<logicalChars.size(); i++) {
			if(!logicalChars.get(i).equals(" "))
				baseChars.add(logicalChars.get(i));
		}
		
		
		if(ScrambleQuotePreferences.EXPERT_MODE)
			Collections.shuffle(baseChars);
		
		ScrambleQuotePreferences.BANK_ROWS = 1;
		if(baseChars.size()<16) {
			ScrambleQuotePreferences.BANK_COLUMNS = baseChars.size();
		} else {
			ScrambleQuotePreferences.BANK_COLUMNS = 16;
			int n = 0;
			for(int i = 0; i<baseChars.size(); i++) {
				n++;
				if(n>16) {
					ScrambleQuotePreferences.BANK_ROWS++;
					n = 0;
				}
			}
		}
		
		int index = 0;
		bankGrid = new String[ScrambleQuotePreferences.BANK_ROWS][ScrambleQuotePreferences.BANK_COLUMNS];
		for(int i = 0; i<ScrambleQuotePreferences.BANK_ROWS; i++) {
			for(int j = 0; j<ScrambleQuotePreferences.BANK_COLUMNS; j++) {
				if(index<baseChars.size()) {
					bankGrid[i][j] = baseChars.get(index);
					index++;
				} else {
					bankGrid[i][j] = " ";
				}
			}
		}
	}

	public String[][] getBankGrid() {
		return bankGrid;
	}
	
	public ArrayList<String> getLogicalChars(){
		return logicalChars;
	}
}
