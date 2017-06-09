package org.maltparser.concurrent.test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.maltparser.concurrent.ConcurrentMaltParserService;
import org.maltparser.concurrent.ConcurrentMaltParserModel;
import org.maltparser.core.exception.MaltChainedException;

/**
 * @author Johan Hall
 *
 */
public final class ConcurrentTest {

	public static String getMessageWithElapsed(String message, long startTime) {
		final StringBuilder sb = new StringBuilder();
		long elapsed = (System.nanoTime() - startTime)/1000000;
		sb.append(message);sb.append(" : ");
		sb.append(String.format("%02d:%02d:%02d", elapsed/3600000, elapsed%3600000/60000, elapsed%60000/1000));sb.append(" ( ");
		sb.append(elapsed);sb.append(" ms)");
		return sb.toString();
	}
	
	public static void main(String[] args) {
		long startTime = System.nanoTime();
		if (args.length != 1) {
			System.out.println("No experiment file.");
		}
		List<Experiment> experiments = null;
		
		try {
			experiments = Experiment.loadExperiments(args[0]);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ExperimentException e) {
			e.printStackTrace();
		}
		if (experiments == null) {
			System.out.println("No experiments to process.");
			System.exit(0);
		}
//		System.out.println(experiments);

		try {
			
			ConcurrentMaltParserModel[] models = new ConcurrentMaltParserModel[experiments.size()];
			int nThreads = 0;
			for (int i = 0; i < experiments.size(); i++) {
				Experiment experiment = experiments.get(i);
				nThreads += experiment.nInURLs();
				models[i] = ConcurrentMaltParserService.initializeParserModel(experiment.getModelURL());
			}
			System.out.println(getMessageWithElapsed("Finished loading models", startTime));
			Thread[] threads = new Thread[nThreads];
			int t = 0;
			for (int i = 0; i < experiments.size(); i++) {
				Experiment experiment = experiments.get(i);
				List<URL> inUrls = experiment.getInURLs();
				List<File> outFiles = experiment.getOutFiles();
				for (int j = 0; j < inUrls.size(); j++) {
					threads[t] = new ThreadClass(experiment.getCharSet(), inUrls.get(j), outFiles.get(j), models[i]);
					t++;
				}
			}
			System.out.println(getMessageWithElapsed("Finished init threads", startTime));
	        for (int i = 0; i < threads.length; i++) {
	        	if (threads[i] != null) {
	        		threads[i].start();
	        	} else {
	        		System.out.println("Thread "+ i + " is null");
	        	}
	        }
	        for (int i = 0; i < threads.length; i++) {
	            try {
	            	if (threads[i] != null) {
	            		threads[i].join();
	            	} else {
		        		System.out.println("Thread "+ i + " is null");
		        	}
	            } catch (InterruptedException ignore) {}
	        }
	        System.out.println(getMessageWithElapsed("Finished parsing", startTime));
		} catch (MaltChainedException e) {
            e.printStackTrace();
        } 
	}
}
