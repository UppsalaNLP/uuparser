package org.maltparser.examples;

import java.io.File;
import java.net.URL;

import org.maltparser.concurrent.ConcurrentMaltParserModel;
import org.maltparser.concurrent.ConcurrentMaltParserService;
import org.maltparser.concurrent.graph.ConcurrentDependencyGraph;

/**
 * This example shows how to parse a sentence with MaltParser by first initialize a parser model.
 * 
 * To run this example requires that you have created output/swemalt-mini.mco, please read the README file.
 * 
 * @author Johan Hall
 */
public class ParseSentence1 {

	public static void main(String[] args) {
		ConcurrentDependencyGraph outputGraph = null;
		
		// Loading the Swedish model swemalt-mini
		ConcurrentMaltParserModel model = null;
		try {
			URL swemaltMiniModelURL = new File("output/swemalt-mini.mco").toURI().toURL();
			model = ConcurrentMaltParserService.initializeParserModel(swemaltMiniModelURL);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Creates an array of tokens, which contains the Swedish sentence 'Samtidigt får du högsta sparränta plus en skattefri sparpremie.'
		// in the CoNLL data format.
		String[] tokens = new String[10];
		tokens[0] = "1\tSamtidigt\t_\tAB\tAB\t_";
		tokens[1] = "2\tfår\t_\tVB\tVB\tPRS|AKT";
		tokens[2] = "3\tdu\t_\tPN\tPN\tUTR|SIN|DEF|SUB";
		tokens[3] = "4\thögsta\t_\tJJ\tJJ\tSUV|UTR/NEU|SIN/PLU|DEF|NOM";
		tokens[4] = "5\tsparränta\t_\tNN\tNN\tUTR|SIN|IND|NOM";
		tokens[5] = "6\tplus\t_\tAB\tAB\t_";
		tokens[6] = "7\ten\t_\tDT\tDT\tUTR|SIN|IND";
		tokens[7] = "8\tskattefri\t_\tJJ\tJJ\tPOS|UTR|SIN|IND|NOM";
		tokens[8] = "9\tsparpremie\t_\tNN\tNN\tUTR|SIN|IND|NOM";
		tokens[9] = "10\t.\t_\tMAD\tMAD\t_";
		try {
			outputGraph = model.parse(tokens);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(outputGraph);
	}
}
