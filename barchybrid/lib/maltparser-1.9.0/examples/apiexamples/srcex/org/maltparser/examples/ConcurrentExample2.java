package org.maltparser.examples;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.maltparser.concurrent.ConcurrentMaltParserModel;
import org.maltparser.concurrent.ConcurrentMaltParserService;
import org.maltparser.concurrent.ConcurrentUtils;
import org.maltparser.core.exception.MaltChainedException;


/**
 * This example loads the the Swedish model "output/swemalt-mini.mco" and reads all sentences to 
 * a list and then n threads are created to parse the sentences. Default number of
 * threads are 5, but this value can easily be changed by passing an integer between 1 to 275 to the program. 
 * 
 * The input sentences in "../data/swemalt-mini/parsed/sv-stb-dep-mini-test-parsed.conll" contain already parsed 
 * sentences with the same model by using the MaltParser program (single-threaded).
 * 
 * Finally output sentences are compared with input sentences to see if there are any difference. If success then no differences 
 * should by found (0 differs).
 * 
 * @author Johan Hall
 *
 */
public class ConcurrentExample2 implements Runnable {
	private final List<String[]> inputSentences;
	private final List<String[]> outputSentences;
	private final ConcurrentMaltParserModel model;
	
	public ConcurrentExample2(List<String[]> sentences, ConcurrentMaltParserModel _model) {
		this.inputSentences = new ArrayList<String[]>(sentences);
		this.outputSentences = Collections.synchronizedList(new ArrayList<String[]>());
		this.model = _model;
	}
	
	public void run() {
		for (int i = 0; i < inputSentences.size(); i++) {
			try {
				outputSentences.add(model.parseTokens(inputSentences.get(i)));
			} catch (MaltChainedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public List<String[]> getOutputSentences() {
		return Collections.synchronizedList(new ArrayList<String[]>(outputSentences));
	}
	
	public static String getMessageWithElapsed(String message, long startTime) {
		final StringBuilder sb = new StringBuilder();
		long elapsed = (System.nanoTime() - startTime)/1000000;
		sb.append(message);sb.append(" : ");
		sb.append(elapsed);sb.append(" ms");
		return sb.toString();
	}

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
		List<String[]> inSentences = new ArrayList<String[]>();
		List<String[]> inWithoutGoldSentences = new ArrayList<String[]>();
    	BufferedReader reader = null;
    	try {
    		reader = new BufferedReader(new InputStreamReader(new FileInputStream("../data/swemalt-mini/parsed/sv-stb-dep-mini-test-parsed.conll"), "UTF-8"));
    		while (true) {
    			// Reads a sentence from the input file
	    		String[] goldTokens = ConcurrentUtils.readSentence(reader);
	    		if (goldTokens.length == 0) {
	    			break;
	    		}
	    		// Strips the head and dependency edge label and add the sentence to the list of sentences
	    		inSentences.add(goldTokens);
	    		inWithoutGoldSentences.add(ConcurrentUtils.stripGold(goldTokens,4));
    		}
    	} catch (IOException e) {
			e.printStackTrace();
    	} finally {
    		if (reader != null) {
    			try {
    				reader.close();
    	    	} catch (IOException e) {
    				e.printStackTrace();
    	    	}
    		}
    	}
		System.out.println(getMessageWithElapsed("Read sentences time", startTime));
		startTime = System.nanoTime();
		
		// Creates n threads, default 5
		int numberOfThreads = 5;
		if (args.length == 1) {
			int n = 0;
			try {
				n = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.out.println("Argument is not an integer.");
			}
			if (n >= 1 && n<=inSentences.size()) {
				numberOfThreads = n;
			} else {
				System.out.println("The first argument must be between 1 - "+inSentences.size());
			}
		}
		System.out.println(numberOfThreads + " threads are used to parse "+inSentences.size()+" sentences.");
		
		Thread[] threads = new Thread[numberOfThreads];
		ConcurrentExample2[] runnables = new ConcurrentExample2[numberOfThreads];
		int interval = (inSentences.size()/numberOfThreads);
		int startIndex = 0;
		int t = 0;
		while (startIndex < inSentences.size()) {
			int endIndex = (startIndex+interval < inSentences.size() && t < threads.length -1?startIndex+interval:inSentences.size());
			System.out.println("  Thread " + String.format("%03d",t) + " will parse sentences between " + String.format("%04d",startIndex) + " - " + String.format("%04d",(endIndex-1))
					+ ", number of sentences: " + (endIndex-startIndex));
			runnables[t] = new ConcurrentExample2(inWithoutGoldSentences.subList(startIndex, endIndex), swemaltMiniModel);
			threads[t] = new Thread(runnables[t]);
			startIndex = endIndex;
			t++;
		}
		System.out.println(getMessageWithElapsed("Create threads time", startTime));
		startTime = System.nanoTime();
		
		// Starting threads to parse all sentences.
        for (int i = 0; i < threads.length; i++) {
        	if (threads[i] != null) {
        		threads[i].start();
        	} else {
        		System.err.println("Thread "+ i + " is null");
        	}
        }
        
        // Finally joining all threads
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
		startTime = System.nanoTime();
		
		// Finally output sentences are compared with input sentences to see if there are any difference. If success then no differences should by found (0 differs).
        List<String[]> outSentences = new ArrayList<String[]>();
        for (int i = 0; i < threads.length; i++) {
        	outSentences.addAll(runnables[i].getOutputSentences());
        }
        if (inSentences.size() == outSentences.size()) {
        	int diffCount = 0;
	        for (int i = 0; i < outSentences.size(); i++) {
	        	diffCount = ConcurrentUtils.diffSentences(inSentences.get(i), outSentences.get(i))?diffCount+1:diffCount;;
	        }
	        System.out.println("Number of parsed sentences is "+ outSentences.size() +" and "+diffCount + " differs compared with the parsed input."); 
        } else {
        	System.out.println("Number of sentences differs - in="+inSentences.size()+", out="+outSentences.size());
        }
	}
}
