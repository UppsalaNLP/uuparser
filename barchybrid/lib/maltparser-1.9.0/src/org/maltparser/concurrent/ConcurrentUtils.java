package org.maltparser.concurrent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class contains some basic methods to read sentence from file, write sentence to file, 
 * strip gold-standard information from the input, print sentence to stream and check difference between two sentences.
 * 
 * @author Johan Hall
 *
 */
public class ConcurrentUtils {
    /**
     * Reads a sentence from the a reader and returns a string array with tokens.
     * 
     * The method expect that each line contains a token and empty line is equal to end of sentence.
     * 
     * There are no check for particular data format so if the input is garbage then the output will also be garbage. 
     * 
     * @param reader a buffered reader
     * @return a string array with tokens
     * @throws IOException
     */
    public static String[] readSentence(BufferedReader reader) throws IOException {
    	ArrayList<String> tokens = new ArrayList<String>();
    	String line;
		while ((line = reader.readLine()) != null) {
			if (line.trim().length() == 0) {
				break;
			} else {
				tokens.add(line.trim());
			}

		}
    	return tokens.toArray(new String[tokens.size()]);
    }

    
    
    /**
     * Writes a sentence to a writer. It expect a string array with tokens.
     * 
     * Each token will be one line and after all tokens are written there will be one empty line marking the ending of sentence.
     * 
     * @param inTokens 
     * @param writer a buffered writer
     * @throws IOException
     */
    public static void writeSentence(String[] inTokens, BufferedWriter writer) throws IOException {
    	for (int i = 0; i < inTokens.length; i++) {
    		writer.write(inTokens[i]);
    		writer.newLine();
    	}
    	writer.newLine();
    	writer.flush();
    }
    
    /**
     * Strips the two last columns for each tokens. This method can be useful when reading a file with gold-standard 
     * information in the last two columns and you want to parse without gold-standard information.  
     * 
     * The method expect that each columns are separated with a tab-character.
     * 
     * @param inTokens a string array with tokens where each column are separated with a tab-character
     * @return a string array with tokens without the last two columns
     */
    public static String[] stripGold(String[] inTokens) {
    	return stripGold(inTokens, 2);
    }
    
    /**
     * Strips the <i>stripNumberOfEndingColumns</i> last columns for each tokens. This method can be useful when reading 
     * a file with gold-standard information in the last <i>stripNumberOfEndingColumns</i> columns and you want to 
     * parse without gold-standard information.
     *
     * 
     * @param inTokens a string array with tokens where each column are separated with a tab-character
     * @param stripNumberOfEndingColumns a string array with tokens without the last <i>stripNumberOfEndingColumns</i> columns
     * @return
     */
    public static String[] stripGold(String[] inTokens, int stripNumberOfEndingColumns) {
    	String[] outTokens = new String[inTokens.length];
    	
    	for (int i = 0; i < inTokens.length; i++) {
    		int tabCounter = 0;
    		int j = inTokens[i].length()-1;
    		for (; j >= 0; j--) {
    			if (inTokens[i].charAt(j) == '\t') {
    				tabCounter++;
    			}
    			if (tabCounter == stripNumberOfEndingColumns) {
    				outTokens[i] = inTokens[i].substring(0, j);
    				break;
    			}
    		}
    	}
    	return outTokens;
    }
   
    /**
     * Prints a sentence to the Standard-out stream. It expect a string array with tokens.
     * 
     * Each token will be one line and after all tokens are printed there will be one empty line marking the ending of sentence.
     * 
     * @param inTokens a string array with tokens
     */
    public static void printTokens(String[] inTokens) {
    	printTokens(inTokens, System.out);
    }
    
    /**
     * Prints a sentence to a stream. It expect a string array with tokens.
     * 
     * Each token will be one line and after all tokens are printed there will be one empty line marking the ending of sentence.
     * @param inTokens a string array with tokens
     * @param stream a print stream
     */
    public static void printTokens(String[] inTokens, PrintStream stream) {
    	for (int i = 0; i < inTokens.length; i++) {
    		stream.println(inTokens[i]);
    	}
    	stream.println();
    }
    
    /**
     * Check if there are difference between two sentences
     * 
     * @param goldTokens the sentence one with an array of tokens
     * @param outputTokens the sentence two with an array of tokens
     * @return true, if the sentences differ otherwise false
     */
    public static boolean diffSentences(String[] goldTokens, String[] outputTokens) {
    	if (goldTokens.length != outputTokens.length) {
    		return true;
    	}
    	for (int i = 0; i < goldTokens.length; i++) {
    		if (!goldTokens[i].equals(outputTokens[i])) {
    			return true;
    		}
    	}
    	return false;
    }
    
    public static void simpleEvaluation(List<String[]> goldSentences, List<String[]> parsedSentences, int headColumn, int dependencyLabelColumn, PrintStream stream) {
    	if (goldSentences.size() != parsedSentences.size()) {
    		stream.println("Number of sentences in gold and output differs");
    		return;
    	}
    	int nTokens = 0;
    	int nCorrectHead = 0;
    	int nCorrectLabel = 0;
    	int nCorrectBoth = 0;
    	
    	for (int i = 0; i < goldSentences.size(); i++) {
    		String[] goldTokens = goldSentences.get(i);
    		String[] parsedTokens = parsedSentences.get(i);
        	if (goldTokens.length != parsedTokens.length) {
        		stream.println("Number of tokens in gold and output differs in sentence " + i);
        		return;
        	}
        	for (int j = 0; j < goldTokens.length; j++) {
        		nTokens++;
        		String[] goldColumns = goldTokens[j].split("\t");
        		String[] parsedColumns = parsedTokens[j].split("\t");
//        		System.out.format("%s %s", goldColumns[headColumn],parsedColumns[headColumn]);
        		if (goldColumns[headColumn].equals(parsedColumns[headColumn])) {
        			nCorrectHead++;
        		}
        		if (goldColumns[dependencyLabelColumn].equals(parsedColumns[dependencyLabelColumn])) {
        			nCorrectLabel++;
        		}
        		if (goldColumns[headColumn].equals(parsedColumns[headColumn]) && goldColumns[dependencyLabelColumn].equals(parsedColumns[dependencyLabelColumn])) {
        			nCorrectBoth++;
        		}
        	}
    	}
    	stream.format("Labeled   attachment score: %d / %d * 100 = %.2f %%\n", nCorrectBoth, nTokens, (((float)nCorrectBoth/(float)nTokens)*100.0));
    	stream.format("Unlabeled attachment score: %d / %d * 100 = %.2f %%\n", nCorrectHead, nTokens, (((float)nCorrectHead/(float)nTokens)*100.0));
    	stream.format("Label accuracy score:       %d / %d * 100 = %.2f %%\n", nCorrectLabel, nTokens, (((float)nCorrectLabel/(float)nTokens)*100.0));
    }
}
