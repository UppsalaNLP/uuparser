package org.maltparser.examples;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.maltparser.concurrent.ConcurrentMaltParserModel;
import org.maltparser.concurrent.ConcurrentMaltParserService;
import org.maltparser.concurrent.ConcurrentUtils;
import org.maltparser.concurrent.graph.ConcurrentDependencyGraph;
import org.maltparser.concurrent.graph.ConcurrentDependencyNode;
import org.maltparser.core.exception.MaltChainedException;

/**
 * An example showing how to use the concurrent dependency graph
 * 
 * @author Johan Hall
 *
 */
public class ConcurrentExample3 {
	public static void main(String[] args) {
		// Loading the Swedish model swemalt-mini
		ConcurrentMaltParserModel swemaltMiniModel = null;
		try {
			URL swemaltMiniModelURL = new File("output/swemalt-mini.mco").toURI().toURL();
			swemaltMiniModel = ConcurrentMaltParserService.initializeParserModel(swemaltMiniModelURL);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		BufferedReader reader = null;
		try {
    		reader = new BufferedReader(new InputStreamReader(new FileInputStream("../data/swemalt-mini/gold/sv-stb-dep-mini-test.conll"), "UTF-8"));
    		int sentenceCount = 0;
    		int nodeCount = 0;
    		int projectiveCount = 0;
    		int nodeRootAsHeadcount = 0;
    		while (true) {
    			// Reads a sentence from the input file
	    		String[] goldTokens = ConcurrentUtils.readSentence(reader);
	    		if (goldTokens.length == 0) {
	    			break;
	    		}
	    		// Strips the head and dependency edge label
	    		String[] inputTokens = ConcurrentUtils.stripGold(goldTokens);
	    		
	    		// Parse the sentence and get the resulting dependency graph
	    		ConcurrentDependencyGraph graph = swemaltMiniModel.parse(inputTokens);
	    		nodeCount += graph.nTokenNodes();
	    		projectiveCount += (graph.isProjective())?1:0;
	    		
	    		// Iterates over the token nodes to see if the head node is the root node. 
	    		// Index 0 is the root node, which are excluded. 
	    		for (int i = 1; i < graph.nDependencyNodes(); i++) {
	    			ConcurrentDependencyNode node = graph.getDependencyNode(i);
	    			if (node.hasHead() && node.getHead().isRoot()) {
	    				nodeRootAsHeadcount++;
	    			}
	    		}
	    		
	    		sentenceCount++;
    		}
    		System.out.println("Number of sentence: " + sentenceCount);
    		System.out.println("Number of projective sentence: " + projectiveCount);
    		System.out.println("Number of token nodes: " + nodeCount);
    		System.out.println("Number of token nodes with root as head: " + nodeRootAsHeadcount);
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
    	}
	}
}
