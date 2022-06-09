package main;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;
import org.apache.poi.hslf.usermodel.HSLFLine;
import org.apache.poi.hslf.usermodel.HSLFPictureData;
import org.apache.poi.hslf.usermodel.HSLFPictureShape;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTable;
import org.apache.poi.hslf.usermodel.HSLFTableCell;
import org.apache.poi.hslf.usermodel.HSLFTextBox;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.hslf.usermodel.HSLFTextRun;
import org.apache.poi.sl.usermodel.TableCell.BorderEdge;
import org.apache.poi.sl.usermodel.TextParagraph.TextAlign;
import org.apache.poi.sl.usermodel.VerticalAlignment;
import org.apache.poi.util.IOUtils;

import games.DropNFloatQuote;
import games.DropQuote;
import games.FloatQuote;
import games.ScrambleQuote;
import games.SplitQuote;
import games.StripperQuote;
import preferences.DropNFloatQuotePreferences;
import preferences.DropQuotePreferences;
import preferences.FloatQuotePreferences;
import preferences.ScrambleQuotePreferences;
import preferences.SplitQuotePreferences;
import preferences.StripperQuotePreferences;

/**
 * @author Neil Haggerty This class creates the powerpoint files and draws the puzzles onto them.        
 */

public class PPTGenerator {

	private File quotes = new File("Test2.txt");
	private static String PPT_FILE_NAME;
	private API api = new API();
	
	public PPTGenerator() throws SQLException, IOException {
			
	}

	//creating the split quote powerpoint
	public void createSplitQuote() throws SQLException, IOException {

		//get timestamp for ppt file name
		String timeStamp = String.valueOf(System.currentTimeMillis());
		PPT_FILE_NAME = "Puzzles_" + timeStamp + ".ppt";
		
		Scanner scan = new Scanner(quotes);
		File ppt_file_name = new File(PPT_FILE_NAME);
		int puzzle_slide_no = 1;
		
		//this is the powerpoint slideshow
		HSLFSlideShow ppt = new HSLFSlideShow();

		//Create number of puzzles and slides specified by SplitQuotePreferences
		for(int n = 0; n<SplitQuotePreferences.PUZZLE_COUNT; n++) {

			//creating the puzzle
			String quote = lengthCheck(scan, SplitQuotePreferences.LENGTH_MAX, SplitQuotePreferences.LENGTH_MIN);
			if(quote==null)
				continue;
			SplitQuote puzzle = new SplitQuote(quote);
			String[][] grid = puzzle.getPuzzleGrid();

			//creating the ppt slide
			HSLFSlide slide = ppt.createSlide();
			createTitle(slide, SplitQuotePreferences.TITLE, SplitQuotePreferences.FONT_NAME, SplitQuotePreferences.TITLE_FONT_SIZE, SplitQuotePreferences.TITLE_COLOR);
			createLogo(ppt, slide);

			//create the table that will be the puzzle on the slide
			HSLFTable table = slide.createTable(SplitQuotePreferences.ROWS, SplitQuotePreferences.COLUMNS); 
			
			//creating the puzzle grid. Iterating through the cells and updating the approprate values. 
			for (int i = 0; i<SplitQuotePreferences.COLUMNS; i++) {
				for (int j = 0; j<SplitQuotePreferences.ROWS; j++) {
					String char_string = String.valueOf(grid[j][i]);
					HSLFTableCell cell1 = table.getCell(j, i);

					cell1.setText(char_string);

					if(!char_string.equals(""))
						setBorders(cell1, SplitQuotePreferences.GRID_COLOR);
					HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
					rt1.setFontFamily(SplitQuotePreferences.FONT_NAME);
					rt1.setFontSize(SplitQuotePreferences.GRID_FONT_SIZE);
					rt1.setFontColor(SplitQuotePreferences.TEXT_COLOR);
					cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
					cell1.setHorizontalCentered(true);
				}
			}

			//setting the width and height of the cells, and the tables draw location
			for (int i = 0; i<SplitQuotePreferences.COLUMNS; i++) {
				table.setColumnWidth(i, SplitQuotePreferences.CELL_WIDTH);
			}
			for (int i = 0; i<SplitQuotePreferences.ROWS; i++) {
				table.setRowHeight(i, SplitQuotePreferences.CELL_HEIGHT);
			}
			table.moveTo(SplitQuotePreferences.STARTING_X, SplitQuotePreferences.STARTING_Y);

			createSlideNumber(slide, puzzle_slide_no, SplitQuotePreferences.FONT_NAME, SplitQuotePreferences.SLIDE_NUMBER_COLOR);
			puzzle_slide_no++;
		}

		//call method to create the solution slides. Pass it the slideshow variable.
		splitQuoteSolutions(ppt);

		//write everything to powerpoint file, then launch the file, then close any open streams
		FileOutputStream out = new FileOutputStream(ppt_file_name);
		ppt.write(out);
		out.close();
		System.out.println("Puzzle created: " + ppt_file_name);
		Desktop.getDesktop().browse(ppt_file_name.toURI());
		ppt.close();
		scan.close();
	}

	//method that creates the solution slides for split quote
	private void splitQuoteSolutions(HSLFSlideShow ppt) throws SQLException, IOException {
		Scanner scan = new Scanner(quotes);
		int puzzle_slide_no = 1;

		//Each loop iteration creates a slide with a puzzle on it and adds it to the ppt file
		for(int n = 0; n<SplitQuotePreferences.PUZZLE_COUNT; n++) {

			//getting quotes, creating slides tables
			String quote = lengthCheck(scan, SplitQuotePreferences.LENGTH_MAX, SplitQuotePreferences.LENGTH_MIN);
			if(quote==null)
				continue;
			SplitQuote puzzle = new SplitQuote(quote);
			String[][] grid = puzzle.getSolutionGrid();

			HSLFSlide slide = ppt.createSlide();
			String title_name = SplitQuotePreferences.TITLE+" Solution";
			createTitle(slide, title_name, SplitQuotePreferences.FONT_NAME, SplitQuotePreferences.TITLE_FONT_SIZE, SplitQuotePreferences.TITLE_COLOR);
			createLogo(ppt, slide);

			HSLFTable table = slide.createTable(SplitQuotePreferences.ROWS, SplitQuotePreferences.COLUMNS); 
			
			//creating the solution grid. Iterating through the cells of the table and updating the values appropriately
			for (int i = 0; i<SplitQuotePreferences.COLUMNS; i++) {
				for (int j = 0; j<SplitQuotePreferences.ROWS; j++) {
					String char_string = String.valueOf(grid[j][i]);
					HSLFTableCell cell1 = table.getCell(j, i);

					cell1.setText(char_string);

					if(!char_string.equals(""))
						setBorders(cell1, SplitQuotePreferences.GRID_COLOR);
					HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
					rt1.setFontFamily(SplitQuotePreferences.FONT_NAME);
					rt1.setFontSize(SplitQuotePreferences.GRID_FONT_SIZE);
					rt1.setFontColor(SplitQuotePreferences.TEXT_COLOR);
					cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
					cell1.setHorizontalCentered(true);
				}
			}

			//setting the width and height of the cells, and the tables draw location
			for (int i = 0; i<SplitQuotePreferences.COLUMNS; i++) {
				table.setColumnWidth(i, SplitQuotePreferences.CELL_WIDTH);
			}
			for (int i = 0; i<SplitQuotePreferences.ROWS; i++) {
				table.setRowHeight(i, SplitQuotePreferences.CELL_HEIGHT);
			}
			table.moveTo(SplitQuotePreferences.STARTING_X, SplitQuotePreferences.STARTING_Y);

			createSlideNumber(slide, puzzle_slide_no, SplitQuotePreferences.FONT_NAME, SplitQuotePreferences.SLIDE_NUMBER_COLOR);
			puzzle_slide_no++;
		}
	}

	//creates the powerpoint for drop quote puzzle
	public void createDropQuote() throws SQLException, IOException {

		String timeStamp = String.valueOf(System.currentTimeMillis());
		PPT_FILE_NAME = "Puzzles_" + timeStamp + ".ppt";
		
		Scanner scan = new Scanner(quotes);
		File ppt_file_name = new File(PPT_FILE_NAME);

		int puzzle_slide_no = 1;
		HSLFSlideShow ppt = new HSLFSlideShow();

		//Each loop iteration creates a slide with a puzzle on it and adds it to the ppt file
		for(int n = 0; n<DropQuotePreferences.PUZZLE_COUNT; n++) {

			//create puzzle
			String quote = lengthCheck(scan, DropQuotePreferences.LENGTH_MAX, DropQuotePreferences.LENGTH_MIN);
			if(quote==null)
				continue;
			DropQuote puzzle = new DropQuote(quote);
			String[][] grid = puzzle.getScrambleGrid();

			//create slide
			HSLFSlide slide = ppt.createSlide();
			createTitle(slide, DropQuotePreferences.TITLE, DropQuotePreferences.FONT_NAME, DropQuotePreferences.TITLE_FONT_SIZE, DropQuotePreferences.TITLE_COLOR);
			createLogo(ppt, slide);

			//creating the clue grid/s at top of slide 
			HSLFTable table = slide.createTable(DropQuotePreferences.ROWS, DropQuotePreferences.COLUMNS); 
			for (int i = 0; i<DropQuotePreferences.COLUMNS; i++) {
				for (int j = 0; j<DropQuotePreferences.ROWS; j++) {
					String char_string = String.valueOf(grid[j][i]);
					HSLFTableCell cell1 = table.getCell(j, i);

					cell1.setText(char_string);

					if(DropQuotePreferences.HAS_BOARDERS)
						setBorders(cell1, DropQuotePreferences.GRID_COLOR);
					HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
					rt1.setFontFamily(DropQuotePreferences.FONT_NAME);
					rt1.setFontColor(DropQuotePreferences.TEXT_COLOR);
					rt1.setFontSize(DropQuotePreferences.GRID_FONT_SIZE);
					cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
					cell1.setHorizontalCentered(true);
				}
			}

			//setting the width and height of the cells, and the tables draw location
			for (int i = 0; i<DropQuotePreferences.COLUMNS; i++) {
				table.setColumnWidth(i, DropQuotePreferences.CELL_WIDTH);
			}
			for (int i = 0; i<DropQuotePreferences.ROWS; i++) {
				table.setRowHeight(i, DropQuotePreferences.CELL_HEIGHT);
			}
			table.moveTo(DropQuotePreferences.STARTING_X, DropQuotePreferences.STARTING_Y);

			//creating the grid/s on the slide for the puzzle
			HSLFTable table2 = slide.createTable(DropQuotePreferences.ROWS, DropQuotePreferences.COLUMNS);
			grid = puzzle.getPuzzleGrid();
			for (int i = 0; i<DropQuotePreferences.COLUMNS; i++) {
				for (int j = 0; j<DropQuotePreferences.ROWS; j++) {
					String char_string = String.valueOf(grid[j][i]);
					if(char_string.equals(" ")) {
						HSLFTableCell cell1 = table2.getCell(j, i);

						cell1.setText(" ");
						cell1.setFillColor(DropQuotePreferences.FILL_COLOR);

						if(DropQuotePreferences.HAS_BOARDERS)
							setBorders(cell1, DropQuotePreferences.GRID_COLOR);
						HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
						rt1.setFontFamily(DropQuotePreferences.FONT_NAME);
						rt1.setFontSize(DropQuotePreferences.GRID_FONT_SIZE);
						rt1.setFontColor(DropQuotePreferences.TEXT_COLOR);
						cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
						cell1.setHorizontalCentered(true);
					} else {
						HSLFTableCell cell1 = table2.getCell(j, i);

						cell1.setText(" ");

						if(DropQuotePreferences.HAS_BOARDERS)
							setBorders(cell1, DropQuotePreferences.GRID_COLOR);
						HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
						rt1.setFontFamily(DropQuotePreferences.FONT_NAME);
						rt1.setFontSize(DropQuotePreferences.GRID_FONT_SIZE);
						rt1.setFontColor(DropQuotePreferences.TEXT_COLOR);
						cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
						cell1.setHorizontalCentered(true);
					}
				}
			}

			//setting the width and height of the cells, and the tables draw location
			for (int i = 0; i<DropQuotePreferences.COLUMNS; i++) {
				table2.setColumnWidth(i, DropQuotePreferences.CELL_WIDTH);
			}
			for (int i = 0; i<DropQuotePreferences.ROWS; i++) {
				table2.setRowHeight(i, DropQuotePreferences.CELL_HEIGHT);
			}
			table2.moveTo(DropQuotePreferences.STARTING_X, DropQuotePreferences.STARTING_Y*4);


			createSlideNumber(slide, puzzle_slide_no, DropQuotePreferences.FONT_NAME, DropQuotePreferences.SLIDE_NUMBER_COLOR);
			puzzle_slide_no++;
		}

		//call method to create solution slides
		dropSolutions(ppt);

		//write everything to powerpoint file, then launch the file, then close any open streams
		FileOutputStream out = new FileOutputStream(ppt_file_name);
		ppt.write(out);
		out.close();
		System.out.println("Puzzle created: " + ppt_file_name);
		Desktop.getDesktop().browse(ppt_file_name.toURI());		
		ppt.close();
		scan.close();
	}

	//method creates solution slides for drop quote.
	private void dropSolutions(HSLFSlideShow ppt) throws SQLException, IOException {
		Scanner scan = new Scanner(quotes);
		int puzzle_slide_no = 1;

		//Each loop iteration creates a slide with a puzzle on it and adds it to the ppt file
		for(int n = 0; n<DropQuotePreferences.PUZZLE_COUNT; n++) {
			String quote = lengthCheck(scan, DropQuotePreferences.LENGTH_MAX, DropQuotePreferences.LENGTH_MIN);
			if(quote==null)
				continue;
			DropQuote puzzle = new DropQuote(quote);			
			HSLFSlide slide = ppt.createSlide();
			String title_name = DropQuotePreferences.TITLE+" Solution";
			createTitle(slide, title_name, DropQuotePreferences.FONT_NAME, DropQuotePreferences.TITLE_FONT_SIZE, DropQuotePreferences.TITLE_COLOR);
			createLogo(ppt, slide);
			

			//creating the solution on the slide
			HSLFTable table = slide.createTable(DropQuotePreferences.ROWS, DropQuotePreferences.COLUMNS);
			String[][] grid = puzzle.getPuzzleGrid();
			for (int i = 0; i<DropQuotePreferences.COLUMNS; i++) {
				for (int j = 0; j<DropQuotePreferences.ROWS; j++) {
					String char_string = String.valueOf(grid[j][i]);

					//Fill in spaces
					if(char_string.equals(" ")) {
						HSLFTableCell cell1 = table.getCell(j, i);
						cell1.setText(" ");
						cell1.setFillColor(DropQuotePreferences.FILL_COLOR);
						if(DropQuotePreferences.HAS_BOARDERS)
							setBorders(cell1, DropQuotePreferences.GRID_COLOR);
						HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
						rt1.setFontColor(DropQuotePreferences.TEXT_COLOR);
						rt1.setFontFamily(DropQuotePreferences.FONT_NAME);
						rt1.setFontSize(DropQuotePreferences.GRID_FONT_SIZE);
						cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
						cell1.setHorizontalCentered(true);
					} else {
						HSLFTableCell cell1 = table.getCell(j, i);

						cell1.setText(char_string);

						if(DropQuotePreferences.HAS_BOARDERS)
							setBorders(cell1, DropQuotePreferences.GRID_COLOR);
						HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
						rt1.setFontFamily(DropQuotePreferences.FONT_NAME);
						rt1.setFontSize(DropQuotePreferences.GRID_FONT_SIZE);
						rt1.setFontColor(DropQuotePreferences.TEXT_COLOR);
						cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
						cell1.setHorizontalCentered(true);
					}
				}
			}

			//setting the width and height of the cells, and the tables draw location
			for (int i = 0; i<DropQuotePreferences.COLUMNS; i++) {
				table.setColumnWidth(i, DropQuotePreferences.CELL_WIDTH);
			}
			for (int i = 0; i<DropQuotePreferences.ROWS; i++) {
				table.setRowHeight(i, DropQuotePreferences.CELL_HEIGHT);
			}
			table.moveTo(DropQuotePreferences.STARTING_X, DropQuotePreferences.STARTING_Y);

			createSlideNumber(slide, puzzle_slide_no, DropQuotePreferences.FONT_NAME, DropQuotePreferences.SLIDE_NUMBER_COLOR);
			puzzle_slide_no++;
		}
	}

	//method creates the powerpoint for float quote
	public void createFloatQuote() throws SQLException, IOException {

		String timeStamp = String.valueOf(System.currentTimeMillis());
		PPT_FILE_NAME = "Puzzles_" + timeStamp + ".ppt";
		
		Scanner scan = new Scanner(quotes);
		File ppt_file_name = new File(PPT_FILE_NAME);

		int puzzle_slide_no = 1;
		HSLFSlideShow ppt = new HSLFSlideShow();

		//Repeat all this for each puzzle
		for(int n = 0; n<FloatQuotePreferences.PUZZLE_COUNT; n++) {

			//generating the puzzle and creating the ppt slide for it
			String quote = lengthCheck(scan, FloatQuotePreferences.LENGTH_MAX, FloatQuotePreferences.LENGTH_MIN);
			if(quote==null)
				continue;
			FloatQuote puzzle = new FloatQuote(quote);
			String[][] grid = puzzle.getPuzzleGrid();
			HSLFSlide slide = ppt.createSlide();
			createTitle(slide, FloatQuotePreferences.TITLE, FloatQuotePreferences.FONT_NAME, FloatQuotePreferences.TITLE_FONT_SIZE, FloatQuotePreferences.TITLE_COLOR);
			createLogo(ppt, slide);

			//writing the puzzles values into the table
			HSLFTable table2 = slide.createTable(FloatQuotePreferences.ROWS, FloatQuotePreferences.COLUMNS);
			for (int i = 0; i<FloatQuotePreferences.COLUMNS; i++) {
				for (int j = 0; j<FloatQuotePreferences.ROWS; j++) {
					String char_string = String.valueOf(grid[j][i]);
					if(char_string.equals(" ")) {
						HSLFTableCell cell1 = table2.getCell(j, i);

						cell1.setText(" ");
						cell1.setFillColor(FloatQuotePreferences.FILL_COLOR);

						if(FloatQuotePreferences.HAS_BOARDERS)
							setBorders(cell1, FloatQuotePreferences.GRID_COLOR);
						HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
						rt1.setFontFamily(FloatQuotePreferences.FONT_NAME);
						rt1.setFontSize(FloatQuotePreferences.GRID_FONT_SIZE);
						rt1.setFontColor(FloatQuotePreferences.TEXT_COLOR);
						cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
						cell1.setHorizontalCentered(true);
					} else {
						HSLFTableCell cell1 = table2.getCell(j, i);

						cell1.setText(" ");

						if(FloatQuotePreferences.HAS_BOARDERS)
							setBorders(cell1, FloatQuotePreferences.GRID_COLOR);
						HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
						rt1.setFontFamily(FloatQuotePreferences.FONT_NAME);
						rt1.setFontSize(FloatQuotePreferences.GRID_FONT_SIZE);
						rt1.setFontColor(FloatQuotePreferences.TEXT_COLOR);
						cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
						cell1.setHorizontalCentered(true);
					}
				}
			}

			//setting the width and height of the cells, and the tables draw location
			for (int i = 0; i<FloatQuotePreferences.COLUMNS; i++) {
				table2.setColumnWidth(i, FloatQuotePreferences.CELL_WIDTH);
			}
			for (int i = 0; i<FloatQuotePreferences.ROWS; i++) {
				table2.setRowHeight(i, FloatQuotePreferences.CELL_HEIGHT);
			}
			table2.moveTo(FloatQuotePreferences.STARTING_X, FloatQuotePreferences.STARTING_Y);

			//creating the clue at bottom of slide
			grid = puzzle.getScrambleGrid();
			HSLFTable table = slide.createTable(FloatQuotePreferences.ROWS, FloatQuotePreferences.COLUMNS); 
			for (int i = 0; i<FloatQuotePreferences.COLUMNS; i++) {
				for (int j = 0; j<FloatQuotePreferences.ROWS; j++) {
					String char_string = String.valueOf(grid[j][i]);
					HSLFTableCell cell1 = table.getCell(j, i);

					cell1.setText(char_string);

					if(FloatQuotePreferences.HAS_BOARDERS)
						setBorders(cell1, FloatQuotePreferences.GRID_COLOR);
					HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
					rt1.setFontFamily(FloatQuotePreferences.FONT_NAME);
					rt1.setFontColor(FloatQuotePreferences.TEXT_COLOR);
					rt1.setFontSize(FloatQuotePreferences.GRID_FONT_SIZE);
					cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
					cell1.setHorizontalCentered(true);
				}
			}

			//setting the width and height of the cells, and the tables draw location
			for (int i = 0; i<FloatQuotePreferences.COLUMNS; i++) {
				table.setColumnWidth(i, FloatQuotePreferences.CELL_WIDTH);
			}
			for (int i = 0; i<FloatQuotePreferences.ROWS; i++) {
				table.setRowHeight(i, FloatQuotePreferences.CELL_HEIGHT);
			}
			table.moveTo(FloatQuotePreferences.STARTING_X, FloatQuotePreferences.STARTING_Y*4);

			createSlideNumber(slide, puzzle_slide_no, FloatQuotePreferences.FONT_NAME, FloatQuotePreferences.SLIDE_NUMBER_COLOR);
			puzzle_slide_no++;
		}

		//call method to generate solution slides
		floatSolutions(ppt);

		//write everything to powerpoint file, then launch the file, then close any open streams
		FileOutputStream out = new FileOutputStream(ppt_file_name);
		ppt.write(out);
		out.close();
		System.out.println("Puzzle created: " + ppt_file_name);
		Desktop.getDesktop().browse(ppt_file_name.toURI());
		ppt.close();
		scan.close();
	}

	//method that creates the solution slides for float quote
	private void floatSolutions(HSLFSlideShow ppt) throws SQLException, IOException {
		Scanner scan = new Scanner(quotes);
		int puzzle_slide_no = 1;

		//repeat all this for each puzzle
		for(int n = 0; n<FloatQuotePreferences.PUZZLE_COUNT; n++) {
			
			//create puzzle and slide for that puzzle
			String quote = lengthCheck(scan, FloatQuotePreferences.LENGTH_MAX, FloatQuotePreferences.LENGTH_MIN);
			if(quote==null)
				continue;
			FloatQuote puzzle = new FloatQuote(quote);			
			HSLFSlide slide = ppt.createSlide();
			String title_name = FloatQuotePreferences.TITLE+" Solution";
			createTitle(slide, title_name, FloatQuotePreferences.FONT_NAME, FloatQuotePreferences.TITLE_FONT_SIZE, FloatQuotePreferences.TITLE_COLOR);
			createLogo(ppt, slide);

			//creating the solution table and adding to the slide
			HSLFTable table = slide.createTable(FloatQuotePreferences.ROWS, FloatQuotePreferences.COLUMNS);
			String[][] grid = puzzle.getPuzzleGrid();
			for (int i = 0; i<FloatQuotePreferences.COLUMNS; i++) {
				for (int j = 0; j<FloatQuotePreferences.ROWS; j++) {
					String char_string = String.valueOf(grid[j][i]);

					//Fill in spaces, else add the characters
					if(char_string.equals(" ")) {
						HSLFTableCell cell1 = table.getCell(j, i);
						cell1.setText(" ");
						cell1.setFillColor(FloatQuotePreferences.FILL_COLOR);
						if(FloatQuotePreferences.HAS_BOARDERS)
							setBorders(cell1, FloatQuotePreferences.GRID_COLOR);
						HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
						rt1.setFontColor(FloatQuotePreferences.TEXT_COLOR);
						rt1.setFontFamily(FloatQuotePreferences.FONT_NAME);
						rt1.setFontSize(FloatQuotePreferences.GRID_FONT_SIZE);
						cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
						cell1.setHorizontalCentered(true);
					} else {
						HSLFTableCell cell1 = table.getCell(j, i);

						cell1.setText(char_string);

						if(FloatQuotePreferences.HAS_BOARDERS)
							setBorders(cell1, FloatQuotePreferences.GRID_COLOR);
						HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
						rt1.setFontFamily(FloatQuotePreferences.FONT_NAME);
						rt1.setFontSize(FloatQuotePreferences.GRID_FONT_SIZE);
						rt1.setFontColor(FloatQuotePreferences.TEXT_COLOR);
						cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
						cell1.setHorizontalCentered(true);
					}
				}
			}

			//setting the width and height of the cells, and the tables draw location
			for (int i = 0; i<FloatQuotePreferences.COLUMNS; i++) {
				table.setColumnWidth(i, FloatQuotePreferences.CELL_WIDTH);
			}
			for (int i = 0; i<FloatQuotePreferences.ROWS; i++) {
				table.setRowHeight(i, FloatQuotePreferences.CELL_HEIGHT);
			}
			table.moveTo(FloatQuotePreferences.STARTING_X, FloatQuotePreferences.STARTING_Y);

			createSlideNumber(slide, puzzle_slide_no, FloatQuotePreferences.FONT_NAME, FloatQuotePreferences.SLIDE_NUMBER_COLOR);
			puzzle_slide_no++;
		}
	}

	//method to create power point for stripper quote
	public void createStripperQuote() throws IOException, SQLException {

		String timeStamp = String.valueOf(System.currentTimeMillis());
		PPT_FILE_NAME = "Puzzles_" + timeStamp + ".ppt";
		
		String char_string;
		Scanner scan = new Scanner(quotes);
		File ppt_file_name = new File(PPT_FILE_NAME);

		//yOffSet adjusts the draw location for the next tables
		int yOffSet;
		
		int puzzle_slide_no = 1;
		HSLFSlideShow ppt = new HSLFSlideShow();

		//each iteration creates a slide and puzzle
		for(int n = 0; n<StripperQuotePreferences.PUZZLE_COUNT; n++) {

			//create puzzle
			String quote = lengthCheck(scan, StripperQuotePreferences.LENGTH_MAX, StripperQuotePreferences.LENGTH_MIN);
			if(quote==null)
				continue;
			StripperQuote puzzle = new StripperQuote(quote);
			yOffSet = 0;

			//create slide
			HSLFSlide slide = ppt.createSlide();
			createTitle(slide, StripperQuotePreferences.TITLE, StripperQuotePreferences.FONT_NAME, StripperQuotePreferences.TITLE_FONT_SIZE, StripperQuotePreferences.SLIDE_NUMBER_COLOR);
			createLogo(ppt, slide);

			//Creating the clue table at top of slide
			HSLFTable table = slide.createTable(StripperQuotePreferences.BANK_ROWS, StripperQuotePreferences.BANK_COLUMNS);
			String[][] grid = puzzle.getBankGrid();
			
			//writing values to table
			for (int i = 0; i<StripperQuotePreferences.BANK_ROWS; i++) {
				for (int j = 0; j<StripperQuotePreferences.BANK_COLUMNS; j++) {
					char_string = String.valueOf(grid[i][j]);
					HSLFTableCell cell1 = table.getCell(i, j);

					cell1.setText(char_string);

					//setBorders(cell1);
					HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
					rt1.setFontFamily(StripperQuotePreferences.FONT_NAME);
					rt1.setFontSize(StripperQuotePreferences.GRID_FONT_SIZE);
					rt1.setFontColor(StripperQuotePreferences.TEXT_COLOR);
					cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
					cell1.setHorizontalCentered(true);
				}
			}

			//setting the width and height of the cells, and the tables draw location
			for (int b = 0; b<StripperQuotePreferences.BANK_COLUMNS; b++) {
				table.setColumnWidth(b, StripperQuotePreferences.CELL_WIDTH);
			}
			for (int b = 0; b<StripperQuotePreferences.BANK_ROWS; b++) {
				table.setRowHeight(b, StripperQuotePreferences.CELL_HEIGHT);
			}
			table.moveTo(centerHorizontally(table), StripperQuotePreferences.BANK_STARTING_Y);

			//Creating the puzzle table at bottom of slide
			int index = 0;
			for(int i = 0; i<StripperQuotePreferences.ROWS; i++) {
				
				//separate table for each row
				table = slide.createTable(1, StripperQuotePreferences.COLUMNS);
				for (int j = 0; j < StripperQuotePreferences.COLUMNS; j++) {
					//if run out of characters, write spaces instead
					if(index<puzzle.getLogicalChars().size()) {
						char_string = puzzle.getLogicalChars().get(index);
					} else {
						char_string = " ";
					}

					HSLFTableCell cell1 = table.getCell(0, j);
					
					//if char string is a space, dont draw borders.
					if(!char_string.equals(" ")) {
						setBorders(cell1, StripperQuotePreferences.GRID_COLOR);
						//Write punctuation to tables, but leave other cells empty
						if(hasPunctuation(char_string))
							cell1.setText(char_string);
						else
							cell1.setText(" ");
					}

					HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
					rt1.setFontFamily(StripperQuotePreferences.FONT_NAME);
					rt1.setFontSize(StripperQuotePreferences.GRID_FONT_SIZE);
					rt1.setFontColor(StripperQuotePreferences.TEXT_COLOR);
					cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
					cell1.setHorizontalCentered(true);
					index++;
				}

				//setting the width and height of the cells, and the tables draw location
				for (int b = 0; b < StripperQuotePreferences.COLUMNS; b++) {
					table.setColumnWidth(b, StripperQuotePreferences.CELL_WIDTH);
				}
				table.setRowHeight(0, StripperQuotePreferences.CELL_HEIGHT);
				table.moveTo(centerHorizontally(table), StripperQuotePreferences.STARTING_Y + yOffSet);
				yOffSet += 45;
			}

			createSlideNumber(slide, puzzle_slide_no, StripperQuotePreferences.FONT_NAME, StripperQuotePreferences.SLIDE_NUMBER_COLOR);
			puzzle_slide_no++;
		}

		//call method to create solution slides
		stripperQuoteSolutions(ppt);

		//write everything to powerpoint file, then launch the file, then close any open streams
		FileOutputStream out = new FileOutputStream(ppt_file_name);
		ppt.write(out);
		out.close();
		System.out.println("Puzzle created: " + ppt_file_name);
		Desktop.getDesktop().browse(ppt_file_name.toURI());
		ppt.close();
		scan.close();
	}

	//method to create the solution slides for stripper quote
	private void stripperQuoteSolutions(HSLFSlideShow ppt) throws SQLException, IOException {
		Scanner scan = new Scanner(quotes);
		int puzzle_slide_no = 1;
		String char_string;

		//Each loop iteration creates a slide with a puzzle on it and adds it to the ppt file
		for(int n = 0; n<StripperQuotePreferences.PUZZLE_COUNT; n++) {
			int yOffSet = 30;

			//create puzzle
			String quote = lengthCheck(scan, StripperQuotePreferences.LENGTH_MAX, StripperQuotePreferences.LENGTH_MIN);
			if(quote==null)
				continue;
			StripperQuote puzzle = new StripperQuote(quote);
			
			//create slide
			HSLFSlide slide = ppt.createSlide();
			String title_name = StripperQuotePreferences.TITLE+" Solution";
			createTitle(slide, title_name, StripperQuotePreferences.FONT_NAME, StripperQuotePreferences.TITLE_FONT_SIZE, StripperQuotePreferences.TITLE_COLOR);
			createLogo(ppt, slide);
			HSLFTable table;

			//writing values to table
			int index = 0;
			for(int i = 0; i<StripperQuotePreferences.ROWS; i++) {
				//separate table for each row
				table = slide.createTable(1, StripperQuotePreferences.COLUMNS);
				for (int j = 0; j < StripperQuotePreferences.COLUMNS; j++) {
					if(index<puzzle.getLogicalChars().size()) {
						char_string = puzzle.getLogicalChars().get(index);
					} else {
						char_string = " ";
					}

					HSLFTableCell cell1 = table.getCell(0, j);

					if(!char_string.equals(" ")) {
						setBorders(cell1, StripperQuotePreferences.GRID_COLOR);
						cell1.setText(char_string);
					}

					HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
					rt1.setFontFamily(StripperQuotePreferences.FONT_NAME);
					rt1.setFontSize(StripperQuotePreferences.GRID_FONT_SIZE);
					rt1.setFontColor(StripperQuotePreferences.TEXT_COLOR);
					cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
					cell1.setHorizontalCentered(true);
					index++;
				}

				//setting the width and height of the cells, and the tables draw location
				for (int b = 0; b < StripperQuotePreferences.COLUMNS; b++) {
					table.setColumnWidth(b, StripperQuotePreferences.CELL_WIDTH);
				}
				table.setRowHeight(0, StripperQuotePreferences.CELL_HEIGHT);
				table.moveTo(centerHorizontally(table), StripperQuotePreferences.STARTING_Y + yOffSet);
				yOffSet += 45;
			}
			createSlideNumber(slide, puzzle_slide_no, StripperQuotePreferences.FONT_NAME, StripperQuotePreferences.SLIDE_NUMBER_COLOR);
			puzzle_slide_no++;
		}
	}

	//method that creates the powerpoint for the scramble quote puzzle
	public void createScrambleQuote() throws SQLException, IOException {

		String timeStamp = String.valueOf(System.currentTimeMillis());
		PPT_FILE_NAME = "Puzzles_" + timeStamp + ".ppt";
		
		String char_string;
		Scanner scan = new Scanner(quotes);
		File ppt_file_name = new File(PPT_FILE_NAME);

		int puzzle_slide_no = 1;
		HSLFSlideShow ppt = new HSLFSlideShow();

		//Each loop iteration creates a slide with a puzzle on it and adds it to the ppt file
		for(int n = 0; n<ScrambleQuotePreferences.PUZZLE_COUNT; n++) {
			int yOffSet = 0;

			//create puzzle
			String quote = lengthCheck(scan, ScrambleQuotePreferences.LENGTH_MAX, ScrambleQuotePreferences.LENGTH_MIN);
			if(quote==null)
				continue;
			ScrambleQuote puzzle = new ScrambleQuote(quote);

			//create slide
			HSLFSlide slide = ppt.createSlide();
			createTitle(slide, ScrambleQuotePreferences.TITLE, ScrambleQuotePreferences.FONT_NAME, ScrambleQuotePreferences.TITLE_FONT_SIZE, ScrambleQuotePreferences.TITLE_COLOR);
			createLogo(ppt, slide);

			//creating the clue at the top of slide
			//writing values to table
			HSLFTable table = slide.createTable(ScrambleQuotePreferences.BANK_ROWS, ScrambleQuotePreferences.BANK_COLUMNS);
			String[][] grid = puzzle.getBankGrid();
			for (int i = 0; i < ScrambleQuotePreferences.BANK_ROWS; i++) {
				for (int j = 0; j < ScrambleQuotePreferences.BANK_COLUMNS; j++) {
					char_string = String.valueOf(grid[i][j]);
					HSLFTableCell cell1 = table.getCell(i, j);

					cell1.setText(char_string);

					//setBorders(cell1);
					HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
					rt1.setFontFamily(ScrambleQuotePreferences.FONT_NAME);
					rt1.setFontSize(ScrambleQuotePreferences.GRID_FONT_SIZE);
					rt1.setFontColor(ScrambleQuotePreferences.TEXT_COLOR);
					cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
					cell1.setHorizontalCentered(true);
				}
			}

			//setting the width and height of the cells, and the tables draw location
			for (int b = 0; b < ScrambleQuotePreferences.BANK_COLUMNS; b++) {
				table.setColumnWidth(b, ScrambleQuotePreferences.CELL_WIDTH);
			}
			for (int b = 0; b < ScrambleQuotePreferences.BANK_ROWS; b++) {
				table.setRowHeight(b, ScrambleQuotePreferences.CELL_HEIGHT);
			}
			table.moveTo(centerHorizontally(table), ScrambleQuotePreferences.BANK_STARTING_Y);

			//creating the puzzle grid and writing values to tables
			int index = 0;
			for(int i = 0; i<ScrambleQuotePreferences.ROWS; i++) {
				//create a separate table for each row
				table = slide.createTable(1, ScrambleQuotePreferences.COLUMNS);
				for (int j = 0; j < ScrambleQuotePreferences.COLUMNS; j++) {
					if(index<puzzle.getLogicalChars().size()) {
						char_string = puzzle.getLogicalChars().get(index);
					} else {
						char_string = " ";
					}

					HSLFTableCell cell1 = table.getCell(0, j);

					if(!char_string.equals(" ")) {
						setBorders(cell1, ScrambleQuotePreferences.GRID_COLOR);
						if(hasPunctuation(char_string))
							cell1.setText(char_string);
						else
							cell1.setText(" ");
					}

					HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
					rt1.setFontFamily(ScrambleQuotePreferences.FONT_NAME);
					rt1.setFontSize(ScrambleQuotePreferences.GRID_FONT_SIZE);
					rt1.setFontColor(ScrambleQuotePreferences.TEXT_COLOR);
					cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
					cell1.setHorizontalCentered(true);
					index++;
				}

				//setting the width and height of the cells, and the tables draw location
				for (int b = 0; b < ScrambleQuotePreferences.COLUMNS; b++) {
					table.setColumnWidth(b, ScrambleQuotePreferences.CELL_WIDTH);
				}
				table.setRowHeight(0, ScrambleQuotePreferences.CELL_HEIGHT);
				table.moveTo(centerHorizontally(table), ScrambleQuotePreferences.STARTING_Y + yOffSet);
				yOffSet += 45;
			}

			createSlideNumber(slide, puzzle_slide_no, ScrambleQuotePreferences.FONT_NAME, ScrambleQuotePreferences.SLIDE_NUMBER_COLOR);
			puzzle_slide_no++;
		}

		//call method to create solution slides
		scrambleQuoteSolutions(ppt);

		//write everything to powerpoint file, then launch the file, then close any open streams
		FileOutputStream out = new FileOutputStream(ppt_file_name);
		ppt.write(out);
		out.close();
		System.out.println("Puzzle created: " + ppt_file_name);
		Desktop.getDesktop().browse(ppt_file_name.toURI());
		ppt.close();
		scan.close();
	}

	//method that creates the powerpoint for scramble grid puzzles
	private void scrambleQuoteSolutions(HSLFSlideShow ppt) throws SQLException, IOException {
		Scanner scan = new Scanner(quotes);
		int puzzle_slide_no = 1;
		String char_string;

		//Each loop iteration creates a slide with a puzzle on it and adds it to the ppt file
		for(int n = 0; n<ScrambleQuotePreferences.PUZZLE_COUNT; n++) {
			String quote = lengthCheck(scan, ScrambleQuotePreferences.LENGTH_MAX, ScrambleQuotePreferences.LENGTH_MIN);
			if(quote==null)
				continue;
			ScrambleQuote puzzle = new ScrambleQuote(quote);

			int yOffSet = 30;

			HSLFSlide slide = ppt.createSlide();
			String title_name = ScrambleQuotePreferences.TITLE+" Solution";
			createTitle(slide, title_name, ScrambleQuotePreferences.FONT_NAME, ScrambleQuotePreferences.TITLE_FONT_SIZE, ScrambleQuotePreferences.TITLE_COLOR);
			createLogo(ppt, slide);

			HSLFTable table;

			int index = 0;
			for(int i = 0; i<ScrambleQuotePreferences.ROWS; i++) {

				table = slide.createTable(1, ScrambleQuotePreferences.COLUMNS);
				for (int j = 0; j < ScrambleQuotePreferences.COLUMNS; j++) {
					if(index<puzzle.getLogicalChars().size()) {
						char_string = puzzle.getLogicalChars().get(index);
					} else {
						char_string = " ";
					}

					HSLFTableCell cell1 = table.getCell(0, j);

					if(!char_string.equals(" ")) {
						setBorders(cell1, ScrambleQuotePreferences.GRID_COLOR);
						cell1.setText(char_string);
					}

					HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
					rt1.setFontFamily(ScrambleQuotePreferences.FONT_NAME);
					rt1.setFontSize(ScrambleQuotePreferences.GRID_FONT_SIZE);
					rt1.setFontColor(ScrambleQuotePreferences.TEXT_COLOR);
					cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
					cell1.setHorizontalCentered(true);
					index++;
				}

				//setting the width and height of the cells, and the tables draw location
				for (int b = 0; b < ScrambleQuotePreferences.COLUMNS; b++) {
					table.setColumnWidth(b, ScrambleQuotePreferences.CELL_WIDTH);
				}
				table.setRowHeight(0, ScrambleQuotePreferences.CELL_HEIGHT);
				table.moveTo(centerHorizontally(table), ScrambleQuotePreferences.STARTING_Y + yOffSet);
				yOffSet += 45;
			}

			createSlideNumber(slide, puzzle_slide_no, ScrambleQuotePreferences.FONT_NAME, ScrambleQuotePreferences.SLIDE_NUMBER_COLOR);
			puzzle_slide_no++;
		}

	}
	
	//method creates the powerpoint with drop n float puzzles
	public void createDropNFloat() throws SQLException, IOException {
		Scanner scan = new Scanner(quotes);
		String char_string;
		String timeStamp = String.valueOf(System.currentTimeMillis());
		PPT_FILE_NAME = "Puzzles_" + timeStamp + ".ppt";
		File ppt_file_name = new File(PPT_FILE_NAME);
		int puzzle_slide_no = 1;
		HSLFSlideShow ppt = new HSLFSlideShow();
		
		//Each loop iteration creates a slide with a puzzle on it and adds it to the ppt file
		for(int n = 0; n<DropNFloatQuotePreferences.PUZZLE_COUNT; n++) {		
			
			String quote1 = lengthCheck(scan, DropNFloatQuotePreferences.LENGTH_MAX, DropNFloatQuotePreferences.LENGTH_MIN);
			String quote2 = lengthCheck(scan, DropNFloatQuotePreferences.LENGTH_MAX, DropNFloatQuotePreferences.LENGTH_MIN);
			if(quote1==null || quote2==null)
				continue;
			DropNFloatQuote puzzle = new DropNFloatQuote(quote1, quote2);
			
			int yOffSet = 0;

			HSLFSlide slide = ppt.createSlide();
			createTitle(slide, DropNFloatQuotePreferences.TITLE, DropNFloatQuotePreferences.FONT_NAME, DropNFloatQuotePreferences.TITLE_FONT_SIZE, DropNFloatQuotePreferences.TITLE_COLOR);
			createLogo(ppt, slide);

			//creating the float grid
			HSLFTable table = slide.createTable(DropNFloatQuotePreferences.FLOAT_ROWS, DropNFloatQuotePreferences.COLUMNS);
			String[][] grid = puzzle.getFloatGrid();
			for (int i = 0; i < DropNFloatQuotePreferences.FLOAT_ROWS; i++) {
				for (int j = 0; j < DropNFloatQuotePreferences.COLUMNS; j++) {
					char_string = String.valueOf(grid[i][j]);
					if(char_string.equals(" ")) {
						HSLFTableCell cell1 = table.getCell(i, j);

						cell1.setText(" ");
						cell1.setFillColor(DropNFloatQuotePreferences.FILL_COLOR);

						if(DropNFloatQuotePreferences.HAS_BOARDERS)
							setBorders(cell1, DropNFloatQuotePreferences.GRID_COLOR);
						HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
						rt1.setFontFamily(DropNFloatQuotePreferences.FONT_NAME);
						rt1.setFontSize(DropNFloatQuotePreferences.GRID_FONT_SIZE);
						rt1.setFontColor(DropNFloatQuotePreferences.TEXT_COLOR);
						cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
						cell1.setHorizontalCentered(true);
					} else {
						HSLFTableCell cell1 = table.getCell(i, j);

						cell1.setText(" ");

						if(DropNFloatQuotePreferences.HAS_BOARDERS)
							setBorders(cell1, DropNFloatQuotePreferences.GRID_COLOR);
						HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
						rt1.setFontFamily(DropNFloatQuotePreferences.FONT_NAME);
						rt1.setFontSize(DropNFloatQuotePreferences.GRID_FONT_SIZE);
						rt1.setFontColor(DropNFloatQuotePreferences.TEXT_COLOR);
						cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
						cell1.setHorizontalCentered(true);
					}
				}
			}

			//setting the width and height of the cells, and the tables draw location
			for (int b = 0; b < DropNFloatQuotePreferences.COLUMNS; b++) {
				table.setColumnWidth(b, DropNFloatQuotePreferences.FLOAT_CELL_WIDTH);
			}
			for (int b = 0; b < DropNFloatQuotePreferences.FLOAT_ROWS; b++) {
				table.setRowHeight(b, DropNFloatQuotePreferences.FLOAT_CELL_HEIGHT);
			}
			table.moveTo(DropNFloatQuotePreferences.STARTING_X, DropNFloatQuotePreferences.STARTING_Y + yOffSet);

			//offset for next table
			yOffSet += (23*DropNFloatQuotePreferences.FLOAT_ROWS);
			
			//creating the clue grid
			table = slide.createTable(DropNFloatQuotePreferences.TOTAL_ROWS, DropNFloatQuotePreferences.COLUMNS);
			grid = puzzle.getFinalScrambleGrid();
			for (int i = 0; i < DropNFloatQuotePreferences.TOTAL_ROWS; i++) {
				for (int j = 0; j < DropNFloatQuotePreferences.COLUMNS; j++) {
					char_string = String.valueOf(grid[i][j]);
					
					HSLFTableCell cell1 = table.getCell(i, j);
					cell1.setText(char_string);
					
					if(DropNFloatQuotePreferences.HAS_BOARDERS)
						setBorders(cell1, DropNFloatQuotePreferences.GRID_COLOR);
					HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
					rt1.setFontFamily(DropNFloatQuotePreferences.FONT_NAME);
					rt1.setFontSize(DropNFloatQuotePreferences.GRID_FONT_SIZE);
					rt1.setFontColor(DropNFloatQuotePreferences.TEXT_COLOR);
					cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
					cell1.setHorizontalCentered(true);
				}
			}
			
			//setting the width and height of the cells, and the tables draw location
			for (int b = 0; b < DropNFloatQuotePreferences.COLUMNS; b++) {
				table.setColumnWidth(b, DropNFloatQuotePreferences.FLOAT_CELL_WIDTH);
			}
			for (int b = 0; b < DropNFloatQuotePreferences.TOTAL_ROWS; b++) {
				table.setRowHeight(b, DropNFloatQuotePreferences.FLOAT_CELL_HEIGHT);
			}
			table.moveTo(DropNFloatQuotePreferences.STARTING_X, DropNFloatQuotePreferences.STARTING_Y + yOffSet);
			
			//offset for next table
			yOffSet += (23*(DropNFloatQuotePreferences.TOTAL_ROWS));
			
			//creating the drop grid
			table = slide.createTable(DropNFloatQuotePreferences.DROP_ROWS, DropNFloatQuotePreferences.COLUMNS);
			grid = puzzle.getDropGrid();
			for (int i = 0; i < DropNFloatQuotePreferences.DROP_ROWS; i++) {
				for (int j = 0; j < DropNFloatQuotePreferences.COLUMNS; j++) {
					char_string = String.valueOf(grid[i][j]);
					if(char_string.equals(" ")) {
						HSLFTableCell cell1 = table.getCell(i, j);

						cell1.setText(" ");
						cell1.setFillColor(DropNFloatQuotePreferences.FILL_COLOR);

						if(DropNFloatQuotePreferences.HAS_BOARDERS)
							setBorders(cell1, DropNFloatQuotePreferences.GRID_COLOR);
						HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
						rt1.setFontFamily(DropNFloatQuotePreferences.FONT_NAME);
						rt1.setFontSize(DropNFloatQuotePreferences.GRID_FONT_SIZE);
						rt1.setFontColor(DropNFloatQuotePreferences.TEXT_COLOR);
						cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
						cell1.setHorizontalCentered(true);
					} else {
						HSLFTableCell cell1 = table.getCell(i, j);

						cell1.setText(" ");

						if(DropNFloatQuotePreferences.HAS_BOARDERS)
							setBorders(cell1, DropNFloatQuotePreferences.GRID_COLOR);
						HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
						rt1.setFontFamily(DropNFloatQuotePreferences.FONT_NAME);
						rt1.setFontSize(DropNFloatQuotePreferences.GRID_FONT_SIZE);
						rt1.setFontColor(DropNFloatQuotePreferences.TEXT_COLOR);
						cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
						cell1.setHorizontalCentered(true);
					}
				}
			}

			//setting the width and height of the cells, and the tables draw location
			for (int b = 0; b < DropNFloatQuotePreferences.COLUMNS; b++) {
				table.setColumnWidth(b, DropNFloatQuotePreferences.DROP_CELL_WIDTH);
			}
			for (int b = 0; b < DropNFloatQuotePreferences.DROP_ROWS; b++) {
				table.setRowHeight(b, DropNFloatQuotePreferences.DROP_CELL_HEIGHT);
			}
			table.moveTo(DropNFloatQuotePreferences.STARTING_X, DropNFloatQuotePreferences.STARTING_Y + yOffSet);

			createSlideNumber(slide, puzzle_slide_no, DropNFloatQuotePreferences.FONT_NAME, DropNFloatQuotePreferences.SLIDE_NUMBER_COLOR);
			puzzle_slide_no++;
		}
		
		dropNFloatSolutions(ppt);
		
		//write everything to powerpoint file, then launch the file, then close any open streams
		FileOutputStream out = new FileOutputStream(ppt_file_name);
		ppt.write(out);
		out.close();
		System.out.println("Puzzle created: " + ppt_file_name);
		Desktop.getDesktop().browse(ppt_file_name.toURI());
		ppt.close();
		scan.close();
	}
	
	//method creates the solution slides for drop n float puzzles
	private void dropNFloatSolutions(HSLFSlideShow ppt) throws SQLException, IOException {
		Scanner scan = new Scanner(quotes);
		String char_string;
		
		int puzzle_slide_no = 1;
		
		//Each loop iteration creates a slide with a puzzle on it and adds it to the ppt file
		for(int n = 0; n<DropNFloatQuotePreferences.PUZZLE_COUNT; n++) {
			
			
			String quote1 = lengthCheck(scan, DropNFloatQuotePreferences.LENGTH_MAX, DropNFloatQuotePreferences.LENGTH_MIN);
			String quote2 = lengthCheck(scan, DropNFloatQuotePreferences.LENGTH_MAX, DropNFloatQuotePreferences.LENGTH_MIN);
			if(quote1==null || quote2==null)
				continue;
			DropNFloatQuote puzzle = new DropNFloatQuote(quote1, quote2);
			
			int yOffSet = 0;

			HSLFSlide slide = ppt.createSlide();
			createTitle(slide, DropNFloatQuotePreferences.TITLE+" Solution", DropNFloatQuotePreferences.FONT_NAME, DropNFloatQuotePreferences.TITLE_FONT_SIZE, DropNFloatQuotePreferences.TITLE_COLOR);
			createLogo(ppt, slide);

			//creating the float grid
			HSLFTable table = slide.createTable(DropNFloatQuotePreferences.FLOAT_ROWS, DropNFloatQuotePreferences.COLUMNS);
			String[][] grid = puzzle.getFloatGrid();
			for (int i = 0; i < DropNFloatQuotePreferences.FLOAT_ROWS; i++) {
				for (int j = 0; j < DropNFloatQuotePreferences.COLUMNS; j++) {
					char_string = String.valueOf(grid[i][j]);
					if(char_string.equals(" ")) {
						HSLFTableCell cell1 = table.getCell(i, j);

						cell1.setText(" ");
						cell1.setFillColor(DropNFloatQuotePreferences.FILL_COLOR);

						if(DropNFloatQuotePreferences.HAS_BOARDERS)
							setBorders(cell1, DropNFloatQuotePreferences.GRID_COLOR);
						HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
						rt1.setFontFamily(DropNFloatQuotePreferences.FONT_NAME);
						rt1.setFontSize(DropNFloatQuotePreferences.GRID_FONT_SIZE);
						rt1.setFontColor(DropNFloatQuotePreferences.TEXT_COLOR);
						cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
						cell1.setHorizontalCentered(true);
					} else {
						HSLFTableCell cell1 = table.getCell(i, j);

						cell1.setText(char_string);

						if(DropNFloatQuotePreferences.HAS_BOARDERS)
							setBorders(cell1, DropNFloatQuotePreferences.GRID_COLOR);
						HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
						rt1.setFontFamily(DropNFloatQuotePreferences.FONT_NAME);
						rt1.setFontSize(DropNFloatQuotePreferences.GRID_FONT_SIZE);
						rt1.setFontColor(DropNFloatQuotePreferences.TEXT_COLOR);
						cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
						cell1.setHorizontalCentered(true);
					}
				}
			}

			//setting the width and height of the cells, and the tables draw location
			for (int b = 0; b < DropNFloatQuotePreferences.COLUMNS; b++) {
				table.setColumnWidth(b, DropNFloatQuotePreferences.FLOAT_CELL_WIDTH);
			}
			for (int b = 0; b < DropNFloatQuotePreferences.FLOAT_ROWS; b++) {
				table.setRowHeight(b, DropNFloatQuotePreferences.FLOAT_CELL_HEIGHT);
			}
			table.moveTo(DropNFloatQuotePreferences.STARTING_X, DropNFloatQuotePreferences.STARTING_Y + yOffSet);
			
			//offset for next table
			yOffSet += (35*DropNFloatQuotePreferences.FLOAT_ROWS);
			
			//creating the drop grid
			table = slide.createTable(DropNFloatQuotePreferences.DROP_ROWS, DropNFloatQuotePreferences.COLUMNS);
			grid = puzzle.getDropGrid();
			for (int i = 0; i < DropNFloatQuotePreferences.DROP_ROWS; i++) {
				for (int j = 0; j < DropNFloatQuotePreferences.COLUMNS; j++) {
					char_string = String.valueOf(grid[i][j]);
					if(char_string.equals(" ")) {
						HSLFTableCell cell1 = table.getCell(i, j);

						cell1.setText(" ");
						cell1.setFillColor(DropNFloatQuotePreferences.FILL_COLOR);

						if(DropNFloatQuotePreferences.HAS_BOARDERS)
							setBorders(cell1, DropNFloatQuotePreferences.GRID_COLOR);
						HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
						rt1.setFontFamily(DropNFloatQuotePreferences.FONT_NAME);
						rt1.setFontSize(DropNFloatQuotePreferences.GRID_FONT_SIZE);
						rt1.setFontColor(DropNFloatQuotePreferences.TEXT_COLOR);
						cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
						cell1.setHorizontalCentered(true);
					} else {
						HSLFTableCell cell1 = table.getCell(i, j);

						cell1.setText(char_string);

						if(DropNFloatQuotePreferences.HAS_BOARDERS)
							setBorders(cell1, DropNFloatQuotePreferences.GRID_COLOR);
						HSLFTextRun rt1 = cell1.getTextParagraphs().get(0).getTextRuns().get(0);
						rt1.setFontFamily(DropNFloatQuotePreferences.FONT_NAME);
						rt1.setFontSize(DropNFloatQuotePreferences.GRID_FONT_SIZE);
						rt1.setFontColor(DropNFloatQuotePreferences.TEXT_COLOR);
						cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);
						cell1.setHorizontalCentered(true);
					}
				}
			}

			//setting the width and height of the cells, and the tables draw location
			for (int b = 0; b < DropNFloatQuotePreferences.COLUMNS; b++) {
				table.setColumnWidth(b, DropNFloatQuotePreferences.DROP_CELL_WIDTH);
			}
			for (int b = 0; b < DropNFloatQuotePreferences.DROP_ROWS; b++) {
				table.setRowHeight(b, DropNFloatQuotePreferences.DROP_CELL_HEIGHT);
			}
			table.moveTo(DropNFloatQuotePreferences.STARTING_X, DropNFloatQuotePreferences.STARTING_Y + yOffSet);

			createSlideNumber(slide, puzzle_slide_no, DropNFloatQuotePreferences.FONT_NAME, DropNFloatQuotePreferences.SLIDE_NUMBER_COLOR);
			puzzle_slide_no++;
		}
	}

	//This method checks if a string has punctuation and returns true or false
	private boolean hasPunctuation(String a) {
		ArrayList<String> punctuation = new ArrayList<String>();
		punctuation.add(".");
		punctuation.add("?");
		punctuation.add("!");
		punctuation.add(",");
		punctuation.add(":");
		punctuation.add(";");
		if(punctuation.contains(a))
			return true;
		else
			return false;
	}
	
	//method checks if a quote passes the min and max length requirements. If not, cycle through until one is valid and return it.
	private String lengthCheck(Scanner scan, int max, int min) throws UnsupportedEncodingException {
		String quote;
		while(scan.hasNextLine()) {
			quote = scan.nextLine();
			int length = api.getLength(quote);
			if(!(length>max) && !(length<min))
				return quote;
		}
		return null;
	}

	//method creates the title of a given slide
	private static void createTitle(HSLFSlide slide, String title, String fontName, double fontSize, Color c) {
		HSLFTextBox titleBox = slide.createTextBox();
		HSLFTextParagraph p1 = titleBox.getTextParagraphs().get(0);
		p1.setTextAlign(TextAlign.CENTER);
		HSLFTextRun run = p1.getTextRuns().get(0);
		run.setFontColor(c);
		run.setText(title.toUpperCase());
		run.setFontFamily(fontName);
		run.setFontSize(fontSize);
		titleBox.setAnchor(new Rectangle(240, 10, 400, 200));
	}

	//method creates the slide number on a given slide
	private static void createSlideNumber(HSLFSlide slide, int slideNumber, String fontName, Color c) {
		HSLFTextBox slideNumberBox = slide.createTextBox();
		HSLFTextParagraph p = slideNumberBox.getTextParagraphs().get(0);
		p.setTextAlign(TextAlign.CENTER);
		HSLFTextRun r = p.getTextRuns().get(0);
		r.setText("" + slideNumber + "");
		r.setFontFamily(fontName);
		if(slideNumber<100) {			
			r.setFontSize(30.);
		} else {
			r.setFontSize(20.);
		}
		r.setFontColor(c);
		slideNumberBox.setAnchor(new Rectangle(220, 10, 50, 30));

		createLine(slide, 220, 5, 50, 0, c); // top line
		createLine(slide, 270, 5, 0, 50, c); // right line
		createLine(slide, 220, 55, 50, 0, c); // bottom line
		createLine(slide, 220, 5, 0, 50, c); // left line
	}

	//method draws a line
	private static void createLine(HSLFSlide slide, int x, int y, int width, int height, Color c) {
		HSLFLine line = new HSLFLine();
		line.setAnchor(new Rectangle(x, y, width, height));
		line.setLineColor(c);
		slide.addShape(line);
	}

	//method that adds the logo to the top left side of a slide
	private static void createLogo(HSLFSlideShow ppt, HSLFSlide slide) throws IOException {
		byte[] picture = IOUtils.toByteArray(new FileInputStream(new File("logo.png")));
		HSLFPictureData pd = ppt.addPicture(picture, HSLFPictureData.PictureType.PNG);
		HSLFPictureShape pic_shape = slide.createPicture(pd);
		pic_shape.setAnchor(new Rectangle(0, 0, 174, 65));
	}

	//method that creates the walls of a given table cell
	private static void setBorders(HSLFTableCell cell, Color gridColor) {
		cell.setBorderColor(BorderEdge.bottom, gridColor);
		cell.setBorderColor(BorderEdge.top, gridColor);
		cell.setBorderColor(BorderEdge.right, gridColor);
		cell.setBorderColor(BorderEdge.left, gridColor);
	}
	
	private int centerHorizontally(HSLFTable table) {
		
		int width = (int) (table.getNumberOfColumns()*table.getColumnWidth(0));
		int difference = 720 - width;
		int startingX = difference/2;
		
		return startingX;
	}
	
}
