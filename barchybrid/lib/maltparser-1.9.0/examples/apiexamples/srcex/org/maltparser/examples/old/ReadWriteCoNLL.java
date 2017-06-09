package org.maltparser.examples.old;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.io.dataformat.DataFormatInstance;
import org.maltparser.core.io.dataformat.DataFormatSpecification;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.symbol.hash.HashSymbolTableHandler;
import org.maltparser.core.syntaxgraph.DependencyGraph;
import org.maltparser.core.syntaxgraph.reader.SyntaxGraphReader;
import org.maltparser.core.syntaxgraph.reader.TabReader;
import org.maltparser.core.syntaxgraph.writer.SyntaxGraphWriter;
import org.maltparser.core.syntaxgraph.writer.TabWriter;

/**
 * This example reads dependency graphs formatted according to the CoNLL format and writes the graphs
 * to another file.
 * 
 * @author Johan Hall
 */
public class ReadWriteCoNLL {
	private DependencyGraph inputGraph;
	private SyntaxGraphReader tabReader;
	private SyntaxGraphWriter tabWriter;
	
	public ReadWriteCoNLL(String dataFormatFileName) throws MaltChainedException {
		// Creates a symbol table handler
		SymbolTableHandler symbolTables = new HashSymbolTableHandler();
		
		// Initialize data format instance of the CoNLL data format from conllx.xml (conllx.xml located in same directory)
		DataFormatSpecification dataFormat = new DataFormatSpecification();
		dataFormat.parseDataFormatXMLfile(dataFormatFileName);
		DataFormatInstance dataFormatInstance = dataFormat.createDataFormatInstance(symbolTables, "none");

		// Creates a dependency graph
		inputGraph = new DependencyGraph(symbolTables);
		
		// Creates a tabular reader with the CoNLL data format
		tabReader = new TabReader();
		tabReader.setDataFormatInstance(dataFormatInstance);
		
		// Creates a tabular writer with the CoNLL data format
		tabWriter = new TabWriter();
		tabWriter.setDataFormatInstance(dataFormatInstance);
	}
	
	public void run(String inFile, String outFile, String charSet) throws MaltChainedException {
		
		// Opens the input and output file with a character encoding set
		tabReader.open(inFile, charSet);
		tabWriter.open(outFile, charSet);
		
		boolean moreInput = true;
		// Reads Sentences until moreInput is false
		while (moreInput) {
			moreInput = tabReader.readSentence(inputGraph);
			if (inputGraph.hasTokens()) {			
				tabWriter.writeSentence(inputGraph);
			}
		}

		// Closes the reader and writer
		tabReader.close();
		tabWriter.close();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if (args.length == 4) {
			System.out.println(args[0] + " -> "+ args[1]+" with data format "+args[2]+" character encoding "+args[3]);
			try {
				new ReadWriteCoNLL(args[2]).run(args[0], args[1], args[3]);
			} catch (MaltChainedException e) {
				System.err.println("MaltParser exception : " + e.getMessage());
			}
		} else {
			System.out.println("Usage: ");
			System.out.println(" java -cp classes:../../malt.jar org.maltparser.examples.ReadWriteCoNLL <input file> <output file> <data format file> <character encoding> ");
			System.out.println("Example: ");
			System.out.println(" java -cp classes:../../malt.jar org.maltparser.examples.ReadWriteCoNLL ../data/talbanken05_test.conll out.conll ../../appdata/dataformat/conllx.xml UTF-8 ");
		}
	}

}
