package org.maltparser.examples.old;

import org.maltparser.MaltParserService;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.syntaxgraph.DependencyStructure;

/**
 * This example shows how to parse a sentence with MaltParser by first initialize a parser model.
 * 
 * To run this example requires that you have ran TrainingExperiment that creates model0.mco
 * 
 * @author Johan Hall
 */
public class ParseSentence1 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			MaltParserService service =  new MaltParserService();
			// Inititalize the parser model 'model0' and sets the working directory to '.' and sets the logging file to 'parser.log'
			service.initializeParserModel("-c model0 -m parse -w . -lfi parser.log");
			
			// Creates an array of tokens, which contains the Swedish sentence 'Grundavdraget upphör alltså vid en taxerad inkomst på 52500 kr.'
			// in the CoNLL data format.
			String[] tokens = new String[11];
			tokens[0] = "1\tGrundavdraget\t_\tN\tNN\tDD|SS";
			tokens[1] = "2\tupphör\t_\tV\tVV\tPS|SM";
			tokens[2] = "3\talltså\t_\tAB\tAB\tKS";
			tokens[3] = "4\tvid\t_\tPR\tPR\t_";
			tokens[4] = "5\ten\t_\tN\tEN\t_";
			tokens[5] = "6\ttaxerad\t_\tP\tTP\tPA";
			tokens[6] = "7\tinkomst\t_\tN\tNN\t_";
			tokens[7] = "8\tpå\t_\tPR\tPR\t_";
			tokens[8] = "9\t52500\t_\tR\tRO\t_";
			tokens[9] = "10\tkr\t_\tN\tNN\t_";
			tokens[10] = "11\t.\t_\tP\tIP\t_";
			// Parses the Swedish sentence above
			DependencyStructure graph = service.parse(tokens);
			// Outputs the dependency graph created by MaltParser.
			System.out.println(graph);
			// Terminates the parser model
			service.terminateParserModel();
		} catch (MaltChainedException e) {
			System.err.println("MaltParser exception: " + e.getMessage());
		}
	}

}
