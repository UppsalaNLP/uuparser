package org.maltparser.examples.old;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.symbol.hash.HashSymbolTableHandler;
import org.maltparser.core.syntaxgraph.DependencyGraph;
import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.core.syntaxgraph.node.DependencyNode;

/**
 * This example creates dependency graph of the sentence "Johan likes graphs" using 
 * the syntax graph package.
 * 
 * @author Johan Hall
 */
public class CreateDependencyGraph {

	public CreateDependencyGraph()  { }
	
	public DependencyGraph run() throws MaltChainedException {
		// Creates a symbol table handler
		SymbolTableHandler symbolTables = new HashSymbolTableHandler();
		
		// Adds three symbol tables (FORM, POSTAG and DEPREL)
		SymbolTable formTable = symbolTables.addSymbolTable("FORM");
		SymbolTable postagTable = symbolTables.addSymbolTable("POSTAG");
		SymbolTable deprelTable = symbolTables.addSymbolTable("DEPREL");
		
		// Creates a dependency graph
		DependencyGraph graph = new DependencyGraph(symbolTables);
		
		
		// Adds three dependency (token) nodes
		DependencyNode node = null;

		node = graph.addDependencyNode(1);
		node.addLabel(formTable, "Johan");
		node.addLabel(postagTable, "N");
		
		node = graph.addDependencyNode(2);
		node.addLabel(formTable, "likes");
		node.addLabel(postagTable, "V");
		
		node = graph.addDependencyNode(3);
		node.addLabel(formTable, "graphs");
		node.addLabel(postagTable, "N");
		
		// Adds three dependency relations (edges)
		Edge e = null;
		e = graph.addDependencyEdge(0, 2); // The root node has index 0
		e.addLabel(deprelTable, "PRED");
		
		e = graph.addDependencyEdge(2, 1);
		e.addLabel(deprelTable, "SUB");
		
		e = graph.addDependencyEdge(2, 3);
		e.addLabel(deprelTable, "OBJ");
		
		return graph;
	}
	
	/**
	 * @param args 
	 */
	public static void main(String[] args) {
		try {
			DependencyGraph graph = new CreateDependencyGraph().run();
			System.out.println(graph);
		} catch (MaltChainedException e) {
			System.err.println("MaltParser exception : " + e.getMessage());
		}
	}
}
