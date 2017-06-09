package org.maltparser.examples;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;

import org.maltparser.concurrent.ConcurrentMaltParserModel;
import org.maltparser.concurrent.ConcurrentMaltParserService;
import org.maltparser.concurrent.ConcurrentUtils;

/**
 * This example shows how to parse sentences from file. The only difference between example ParseSentence1 is that the input is read from 
 * file '../data/swemalt-mini/input/sv-stb-dep-mini-test-blind.conll' and written to 'output/out.conll' in the CoNLL data format. 
 * 
 * To run this example requires that you have created output/swemalt-mini.mco, please read the README file.
 * 
 * @author Johan Hall
 */
public class ParseSentence2 {
	public static void main(String[] args) {
		// Loading the Swedish model swemalt-mini
		ConcurrentMaltParserModel model = null;
		try {
			URL swemaltMiniModelURL = new File("output/swemalt-mini.mco").toURI().toURL();
			model = ConcurrentMaltParserService.initializeParserModel(swemaltMiniModelURL);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
    	BufferedReader reader = null;
    	BufferedWriter writer = null;
    	try {
    		reader = new BufferedReader(new InputStreamReader(new File("../data/swemalt-mini/input/sv-stb-dep-mini-test-blind.conll").toURI().toURL().openStream(), "UTF-8"));
    		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File("output/sv-stb-dep-mini-test-parsed.conll")), "UTF-8"));
    		int sentenceCount = 0;
    		while (true) {
    			// Reads a sentence from the input file
	    		String[] inputTokens = ConcurrentUtils.readSentence(reader);
	    		
	    		// If there are no tokens then we have reach the end of file
	    		if (inputTokens.length == 0) {
	    			break;
	    		}

	    		// Parse the sentence
	    		String[] outputTokens = model.parseTokens(inputTokens);
	    		
	    		// Writes the sentence to the output file
	    		ConcurrentUtils.writeSentence(outputTokens, writer);
	    		
	    		sentenceCount++;
    		}
    		System.out.println("Parsed " + sentenceCount +" sentences");
    	} catch (Exception e) {
			e.printStackTrace();
    	} finally {
    		if (reader != null) {
    			try {
    				reader.close();
    	    	} catch (IOException e) {
    				e.printStackTrace();
    	    	}
    		}
    		if (writer != null) {
    			try {
    				writer.close();
    	    	} catch (IOException e) {
    				e.printStackTrace();
    	    	}
    		}
    	}
	}
}
