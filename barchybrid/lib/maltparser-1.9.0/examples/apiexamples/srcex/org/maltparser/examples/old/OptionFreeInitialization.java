package org.maltparser.examples.old;

import org.maltparser.MaltParserService;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.io.dataformat.DataFormatSpecification;
import org.maltparser.core.syntaxgraph.DependencyStructure;


/*
 * This examples shows how you can initialize MaltParserService without loading MaltParser option manager. Note that no parser model (.mco file) can be loaded or created when option free initialization is true. 
 * 
 * The constructor MaltParserService(true) can be useful when you only want to use MaltParser internal data structures for creating for example a dependency structure.
 * 
 */
public class OptionFreeInitialization {

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: ");
			System.out.println(" java -cp classes:../../malt.jar org.maltparser.examples.OptionFreeInitialization <data format file>");
			System.out.println("Example: ");
			System.out.println(" java -cp classes:../../malt.jar org.maltparser.examples.OptionFreeInitialization ../../appdata/dataformat/conllx.xml");
			return;
		}
		try {
			// Create a MaltParserService object without option manager
			MaltParserService service =  new MaltParserService(true);
			// Creates an array of tokens, which contains the Swedish sentence 'Grundavdraget upphör alltså vid en taxerad inkomst på 52500 kr.'
			// in the CoNLL data format.
			String[] tokens = new String[11];
			tokens[0] = "1\tGrundavdraget\t_\tN\tNN\tDD|SS\t2\tSS";
			tokens[1] = "2\tupphör\t_\tV\tVV\tPS|SM\t0\tROOT";
			tokens[2] = "3\talltså\t_\tAB\tAB\tKS\t2\t+A";
			tokens[3] = "4\tvid\t_\tPR\tPR\t_\t2\tAA";
			tokens[4] = "5\ten\t_\tN\tEN\t_\t7\tDT";
			tokens[5] = "6\ttaxerad\t_\tP\tTP\tPA\t7\tAT";
			tokens[6] = "7\tinkomst\t_\tN\tNN\t_\t4\tPA";
			tokens[7] = "8\tpå\t_\tPR\tPR\t_\t7\tET";
			tokens[8] = "9\t52500\t_\tR\tRO\t_\t10\tDT";
			tokens[9] = "10\tkr\t_\tN\tNN\t_\t8\tPA";
			tokens[10] = "11\t.\t_\tP\tIP\t_\t2\tIP";
			// Print out the string array
			for (int i = 0; i < tokens.length; i++) {
				System.out.println(tokens[i]);
			}
			// Reads the data format specification file
			DataFormatSpecification dataFormatSpecification = service.readDataFormatSpecification(args[0]);
			// Use the data format specification file to build a dependency structure based on the string array
			DependencyStructure graph = service.toDependencyStructure(tokens, dataFormatSpecification);
			// Print the dependency structure
			System.out.println(graph);
		} catch (MaltChainedException e) {
			System.err.println("MaltParser exception: " + e.getMessage());
		}
	}
}
