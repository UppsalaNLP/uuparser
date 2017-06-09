package org.maltparser.examples.old;

import java.io.File;

import org.maltparser.MaltParserService;
import org.maltparser.core.exception.MaltChainedException;

/**
 * This example uses two Single Malt configurations (model0.mco and model1.mco) to parse one 
 * input file (../data/talbanken05_test.conll) and outputs the results to two files out1.conll and out2.conll 
 * 
 * To run this example requires that you have ran TrainingExperiment that creates model0.mco and model1.mco
 * 
 * @author Johan Hall
 */
public class ParsingExperiment {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			String testDataFile = ".."+File.separator+"data"+File.separator+"talbanken05_test.conll";
			// Parses the test data file using the parser model model0.mco and using the option container 0
			new MaltParserService(0).runExperiment("-c model0 -i "+testDataFile+" -o out1.conll -m parse");
			// Parses the test data file using the parser model model1.mco and using the option container 1
			new MaltParserService(1).runExperiment("-c model1 -i "+testDataFile+" -o out2.conll -m parse");
		} catch (MaltChainedException e) {
			System.err.println("MaltParser exception : " + e.getMessage());
		}
	}
}
