package org.maltparser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.maltparser.concurrent.ConcurrentMaltParserModel;
import org.maltparser.concurrent.ConcurrentMaltParserService;
import org.maltparser.concurrent.ConcurrentUtils;
import org.maltparser.concurrent.MaltParserRunnable;
import org.maltparser.core.config.ConfigurationException;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.options.OptionManager;

public class ConcurrentEngine {
	private final int optionContainer;
	private ConcurrentMaltParserModel model;
	
	public ConcurrentEngine(int optionContainer)  {
		System.out.println("Start ConcurrentEngine");
		this.optionContainer = optionContainer;
	}
	
	public static boolean canUseConcurrentEngine(int optionContainer) throws MaltChainedException {
		if (!OptionManager.instance().getOptionValueString(optionContainer,"config", "flowchart").equals("parse"))
			return false;
		if (OptionManager.instance().getOptionValueString(optionContainer,"config", "url").length() > 0)
			return false;
		return true;
	}
	
	public static String getMessageWithElapsed(String message, long startTime) {
		final StringBuilder sb = new StringBuilder();
		long elapsed = (System.nanoTime() - startTime)/1000000;
		sb.append(message);sb.append(": ");
		sb.append(elapsed);sb.append(" ms");
		return sb.toString();
	}
	
	
	public void loadModel() throws MaltChainedException {
		System.out.println("Start loadModel");
		long startTime = System.nanoTime();
		File workingDirectory = getWorkingDirectory(OptionManager.instance().getOptionValue(optionContainer, "config", "workingdir").toString());
		String configName = OptionManager.instance().getOptionValueString(optionContainer,"config", "name");
		String pathToModel = workingDirectory.getPath()+File.separator+configName+".mco";
		try {
			model = ConcurrentMaltParserService.initializeParserModel(new File(pathToModel).toURI().toURL());
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(getMessageWithElapsed("Loading time", startTime));
	}
	

	public File getWorkingDirectory(String path) throws MaltChainedException {
		File workingDirectory;
		if (path == null || path.equalsIgnoreCase("user.dir") || path.equalsIgnoreCase(".")) {
			workingDirectory = new File(System.getProperty("user.dir"));
		} else {
			workingDirectory = new File(path);
		}

		if (workingDirectory == null || !workingDirectory.isDirectory()) {
			new ConfigurationException("The specified working directory '"+path+"' is not a directory. ");
		}
		return workingDirectory;
	}
	
	
	public void parse() throws MaltChainedException {
		System.out.println("Start parse");
		long startTime = System.nanoTime();
		List<String[]> inputSentences = new ArrayList<String[]>();

    	BufferedReader reader = null;
    	String infile = OptionManager.instance().getOptionValueString(optionContainer,"input", "infile");
    	String incharset = OptionManager.instance().getOptionValueString(optionContainer,"input", "charset");
    	try {
    		reader = new BufferedReader(new InputStreamReader(new FileInputStream(infile), incharset));
    		while (true) {
	    		String[] tokens = ConcurrentUtils.readSentence(reader);
	    		if (tokens.length == 0) {
	    			break;
	    		}
	    		inputSentences.add(tokens);

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
		

		int numberOfThreads = 8;

		Thread[] threads = new Thread[numberOfThreads];
		MaltParserRunnable[] runnables = new MaltParserRunnable[numberOfThreads];
		int nSentences = inputSentences.size();
		int interval = (nSentences/numberOfThreads);
		int startIndex = 0;
		int t = 0;
		System.out.println("Number of sentences : " + nSentences);
		while (startIndex < nSentences) {
			int endIndex = (startIndex+interval < nSentences && t < threads.length -1?startIndex+interval:nSentences);
			System.out.println("  Thread " + String.format("%03d",t) + " will parse sentences between " + String.format("%04d",startIndex) + " - " + String.format("%04d",(endIndex-1))
					+ ", number of sentences: " + (endIndex-startIndex));
			runnables[t] = new MaltParserRunnable(inputSentences.subList(startIndex, endIndex), model);
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
    	String outfile = OptionManager.instance().getOptionValueString(optionContainer,"output", "outfile");
    	String outcharset = OptionManager.instance().getOptionValueString(optionContainer,"output", "charset");
    	BufferedWriter writer = null;
    	try {
	    	writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outfile), outcharset));
	        for (int i = 0; i < threads.length; i++) {
	        	List<String[]> outputSentences = runnables[i].getOutputSentences();
	        	for (int j = 0; j < outputSentences.size(); j++) {
	        		ConcurrentUtils.writeSentence(outputSentences.get(j), writer);
	        	}
	        }
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				try {
					writer.close();
		    	} catch (IOException e) {
					e.printStackTrace();
		    	}
			}
		}
    	System.out.println(getMessageWithElapsed("Write sentences time", startTime));
	}
	
	public void terminate() throws MaltChainedException {
		System.out.println("Start terminate");
	}
}
