package org.maltparser.examples.old;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.maltparser.MaltParserService;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.core.syntaxgraph.node.DependencyNode;

/**
 * This example shows how to parse sentences from file. The only difference between example ParseSentence1 is that the input is read from file '../data/talbanken05_test.conll' 
 * and written to 'out.conll' in the CoNLL data format. 
 * 
 * To run this example requires that you have ran TrainingExperiment that creates model0.mco
 * 
 * @author Johan Hall
 */
public class ParseSentence2 {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			MaltParserService service =  new MaltParserService();
			service.initializeParserModel("-c model0 -m parse -w . -lfi parser.log");
			String testDataFile = ".."+File.separator+"data"+File.separator+"talbanken05_test.conll";
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(testDataFile), "UTF-8"));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("out.conll"), "UTF-8"));
			String line = null;
			ArrayList<String> lines = new ArrayList<String>();
			while ((line = reader.readLine()) != null) {
				if (line.trim().length()==0) {
					DependencyStructure graph = service.parse(lines.toArray(new String[lines.size()]));
					for (int i = 1; i <= graph.getHighestDependencyNodeIndex(); i++) {
						DependencyNode node = graph.getDependencyNode(i);
						if (node != null) {
							for (SymbolTable table : node.getLabelTypes()) {
								writer.write(node.getLabelSymbol(table) + "\t");
							}
							if (node.hasHead()) {
								Edge  e = node.getHeadEdge();
								writer.write(e.getSource().getIndex() + "\t");
								if (e.isLabeled()) {
									for (SymbolTable table : e.getLabelTypes()) {
										writer.write(e.getLabelSymbol(table) + "\t");
									}
								} else {
									for (SymbolTable table : graph.getDefaultRootEdgeLabels().keySet()) {
										writer.write(graph.getDefaultRootEdgeLabelSymbol(table) + "\t");
									}
								}
							}
							writer.write('\n');
							writer.flush();
						}
					}
					writer.write('\n');
					writer.flush();
					System.out.print(".");
					lines.clear();
				} else {
					lines.add(line);
				}
			}
			reader.close();
			writer.flush();
			writer.close();
			System.out.println();
			service.terminateParserModel();
		} catch (MaltChainedException e) {
			System.err.println("MaltParser exception: " + e.getMessage());
		} catch (FileNotFoundException e) {
			System.err.println("MaltAPITest exception: " + e.getMessage());
		} catch (UnsupportedEncodingException e) {
			System.err.println("MaltAPITest exception: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("MaltAPITest exception: " + e.getMessage());
		}
	}

}
