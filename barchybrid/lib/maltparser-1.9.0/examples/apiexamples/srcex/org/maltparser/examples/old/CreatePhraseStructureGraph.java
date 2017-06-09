package org.maltparser.examples.old;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.symbol.hash.HashSymbolTableHandler;
import org.maltparser.core.syntaxgraph.PhraseStructureGraph;
import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.core.syntaxgraph.node.PhraseStructureNode;

/**
 * This example creates phrase structure graph of the sentence "Johan likes graphs" using 
 * the syntax graph package.
 * 
 * @author Johan Hall
 */
public class CreatePhraseStructureGraph {
	public CreatePhraseStructureGraph()  { }
	
	public PhraseStructureGraph run() throws MaltChainedException {
		// Creates a symbol table handler
		SymbolTableHandler symbolTables = new HashSymbolTableHandler();
		
		// Adds three symbol tables (FORM, POSTAG, CAT, EDGELABEL)
		SymbolTable formTable = symbolTables.addSymbolTable("FORM");
		SymbolTable postagTable = symbolTables.addSymbolTable("POSTAG");
		SymbolTable catTable = symbolTables.addSymbolTable("CAT");
		SymbolTable edgeLabelTable = symbolTables.addSymbolTable("EDGELABEL");
		
		PhraseStructureGraph graph = new PhraseStructureGraph(symbolTables);
		
		PhraseStructureNode node = null;
		
		// Add three terminal (token) nodes
		node = graph.addTerminalNode(1);
		node.addLabel(formTable, "Johan");
		node.addLabel(postagTable, "N");
		
		node = graph.addTerminalNode(2);
		node.addLabel(formTable, "likes");
		node.addLabel(postagTable, "V");
		
		node = graph.addTerminalNode(3);
		node.addLabel(formTable, "graphs");
		node.addLabel(postagTable, "N");
		
		// Add nonterminal node
		node = graph.addNonTerminalNode(1);
		node.addLabel(catTable, "S");
		
		node = graph.addNonTerminalNode(2);
		node.addLabel(catTable, "NP");
		
		node = graph.addNonTerminalNode(3);
		node.addLabel(catTable, "VP");
		
		node = graph.addNonTerminalNode(4);
		node.addLabel(catTable, "NP");
		
		// Add edges between nonterminal and terminals
		Edge e = null;
		e = graph.addPhraseStructureEdge(graph.getPhraseStructureRoot(), graph.getNonTerminalNode(1));
		
		e = graph.addPhraseStructureEdge(graph.getNonTerminalNode(1), graph.getNonTerminalNode(2));
		e.addLabel(edgeLabelTable, "SUB");
		
		e = graph.addPhraseStructureEdge(graph.getNonTerminalNode(1), graph.getNonTerminalNode(3));
		e.addLabel(edgeLabelTable, "HD");
		
		e = graph.addPhraseStructureEdge(graph.getNonTerminalNode(3), graph.getNonTerminalNode(4));
		e.addLabel(edgeLabelTable, "OBJ");
		
		graph.addPhraseStructureEdge(graph.getNonTerminalNode(2), graph.getTerminalNode(1));
		graph.addPhraseStructureEdge(graph.getNonTerminalNode(3), graph.getTerminalNode(2)).addLabel(edgeLabelTable, "HD");
		graph.addPhraseStructureEdge(graph.getNonTerminalNode(4), graph.getTerminalNode(3));
		return graph;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			PhraseStructureGraph graph = new CreatePhraseStructureGraph().run();
			System.out.println(graph);
		} catch (MaltChainedException e) {
			System.err.println("MaltParser exception : " + e.getMessage());
		}
	}

}
