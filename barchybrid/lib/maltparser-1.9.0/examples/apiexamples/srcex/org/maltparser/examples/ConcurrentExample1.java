package org.maltparser.examples;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;

import org.maltparser.concurrent.ConcurrentMaltParserModel;
import org.maltparser.concurrent.ConcurrentMaltParserService;
import org.maltparser.concurrent.ConcurrentUtils;
import org.maltparser.core.exception.MaltChainedException;

/**
 * This simple example loads the Swedish model "output/swemalt-mini.mco" and then creates five threads to parse five 
 * input files "../data/swemalt-mini/gold/sv-stb-dep-mini-test-XX.conll" where XX is between 00-04. The output is written 
 * to "output/sv-stb-dep-mini-test-parsed-XX.conll".
 *   
 * 
 * @author Johan Hall
 */
public class ConcurrentExample1 extends Thread {
    private final URL inURL;
    private final File outFile;
    private final String charSet;
    private final ConcurrentMaltParserModel model;
    
    public ConcurrentExample1(String _inFile, String _outFile, ConcurrentMaltParserModel _model) throws MalformedURLException {
        this.charSet = "UTF-8";
        this.inURL = new File(_inFile).toURI().toURL();
        this.outFile = new File(_outFile);
        this.model = _model;
    }
    
    public void run() {
    	BufferedReader reader = null;
    	BufferedWriter writer = null;
    	try {
    		reader = new BufferedReader(new InputStreamReader(inURL.openStream(), charSet));
    		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), charSet));
    		int sentenceCount = 0;
    		while (true) {
    			// Reads a sentence from the input file
	    		String[] goldTokens = ConcurrentUtils.readSentence(reader);
	    		if (goldTokens.length == 0) {
	    			break;
	    		}
	    		
	    		// Strips the head and dependency edge label
	    		String[] inputTokens = ConcurrentUtils.stripGold(goldTokens, 4);

	    		// Parse the sentence
	    		String[] outputTokens = model.parseTokens(inputTokens);

	    		// Writes the sentence to the output file
	    		ConcurrentUtils.writeSentence(outputTokens, writer);
	    		
	    		sentenceCount++;
    		}
    		
    		System.out.println("Thread with ID " + String.format("%02d", Thread.currentThread().getId()) + " parsed " +  sentenceCount + " sentences.");
    	} catch (IOException e) {
			e.printStackTrace();
    	} catch (MaltChainedException e) {
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
    
	public static String getMessageWithElapsed(String message, long startTime) {
		final StringBuilder sb = new StringBuilder();
		long elapsed = (System.nanoTime() - startTime)/1000000;
		sb.append(message);sb.append(" : ");
		sb.append(elapsed);sb.append(" ms");
		return sb.toString();
	}
	
	/**
	 * @param args no arguments needed
	 */
	public static void main(String[] args) {
		long startTime = System.nanoTime();
		
		// Loading the Swedish model swemalt-mini
		ConcurrentMaltParserModel swemaltMiniModel = null;
		try {
			URL swemaltMiniModelURL = new File("output/swemalt-mini.mco").toURI().toURL();
			swemaltMiniModel = ConcurrentMaltParserService.initializeParserModel(swemaltMiniModelURL);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(getMessageWithElapsed("Loading time", startTime));
		startTime = System.nanoTime();
		
		// Creating five threads 
		Thread[] threads = new Thread[5];
		for (int i = 0; i < threads.length; i++) {
			String sectionNo = String.format("%02d", i);
			try {
				threads[i] = new ConcurrentExample1("../data/swemalt-mini/gold/sv-stb-dep-mini-test-"+sectionNo+".conll", 
											 "output/sv-stb-dep-mini-test-parsed-"+sectionNo+".conll", swemaltMiniModel);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		System.out.println(getMessageWithElapsed("Thread initialization time", startTime));
		startTime = System.nanoTime();
		
		// Starting five thread to parse five swedish files.
        for (int i = 0; i < threads.length; i++) {
        	if (threads[i] != null) {
        		threads[i].start();
        	} else {
        		System.err.println("Thread "+ i + " is null");
        	}
        }
        
        // Finally joining all five threads
        for (int i = 0; i < threads.length; i++) {
            try {
            	if (threads[i] != null) {
            		threads[i].join();
            	} else {
	        		System.err.println("Thread "+ i + " is null");
	        	}
            } catch (InterruptedException ignore) {}
        }
		System.out.println(getMessageWithElapsed("Parsing time", startTime));
	}
}
