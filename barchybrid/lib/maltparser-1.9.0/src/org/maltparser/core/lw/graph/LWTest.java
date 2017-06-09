package org.maltparser.core.lw.graph;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.SortedSet;

import org.maltparser.concurrent.graph.dataformat.ColumnDescription;
import org.maltparser.concurrent.graph.dataformat.DataFormat;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.symbol.hash.HashSymbolTableHandler;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.core.syntaxgraph.node.DependencyNode;


public class LWTest {
	private static final String IGNORE_COLUMN_SIGN = "_";
    public static String[] readSentences(BufferedReader reader) throws IOException {
    	ArrayList<String> tokens = new ArrayList<String>();
    	String line;
		while ((line = reader.readLine()) != null) {
			if (line.trim().length() == 0) {
				break;
			} else {
				tokens.add(line.trim());
			}

		}
    	return tokens.toArray(new String[tokens.size()]);
    }
    
	public static DependencyStructure getOldDependencyGraph(DataFormat dataFormat, SymbolTableHandler symbolTableHandlers, String[] tokens) throws MaltChainedException {
		DependencyStructure oldGraph = new org.maltparser.core.syntaxgraph.DependencyGraph(symbolTableHandlers);
		for (int i = 0; i < tokens.length; i++) {
		    oldGraph.addDependencyNode(i+1);
		}
		for (int i = 0; i < tokens.length; i++) {
		    DependencyNode node = oldGraph.getDependencyNode(i+1);
		    String[] items = tokens[i].split("\t");
		    Edge edge = null;
		    for (int j = 0; j < items.length; j++) {
		    	ColumnDescription column = dataFormat.getColumnDescription(j);

			    if (column.getCategory() == ColumnDescription.INPUT && node != null) {
			    	oldGraph.addLabel(node, column.getName(), items[j]);
			    } else if (column.getCategory() == ColumnDescription.HEAD) {
			    	if (column.getCategory() != ColumnDescription.IGNORE && !items[j].equals(IGNORE_COLUMN_SIGN)) {
			    		edge = oldGraph.addDependencyEdge(Integer.parseInt(items[j]), i+1);
			    	}
			    } else if (column.getCategory() == ColumnDescription.DEPENDENCY_EDGE_LABEL && edge != null) {
			    	oldGraph.addLabel(edge, column.getName(), items[j]);
				}
		    }
		}

		oldGraph.setDefaultRootEdgeLabel(oldGraph.getSymbolTables().getSymbolTable("DEPREL"), "ROOT");
		return oldGraph;
	}
	
	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		String inFile = args[0];
		String charSet = "UTF-8";

    	BufferedReader reader = null;
    	
    	try {
    		DataFormat dataFormat = DataFormat.parseDataFormatXMLfile("/appdata/dataformat/conllx.xml");
    		reader = new BufferedReader(new InputStreamReader(new FileInputStream(inFile), charSet));
    		int sentenceCounter = 0;
    		while (true) {
	    		String[] goldTokens = readSentences(reader);
	    		if (goldTokens.length == 0) {
	    			break;
	    		}
	    		sentenceCounter++;
	    		SymbolTableHandler newTable = new HashSymbolTableHandler();
	    		DependencyStructure newGraph = new LWDependencyGraph(dataFormat, newTable, goldTokens, "ROOT");
//	    		SymbolTableHandler oldTable = new HashSymbolTableHandler();
//	    		DependencyStructure oldGraph = getOldDependencyGraph(dataFormat, oldTable, goldTokens);
	    		int newGraphINT;
	    		int oldGraphINT;
	    		boolean newGraphBOOL;
	    		boolean oldGraphBOOL;
	    		SortedSet<LWNode> newGraphSortedSet;
	    		SortedSet<DependencyNode> oldGraphSortedSet;
	    		
//	    		for (int i = 0; i < newGraph.nDependencyNode(); i++) {
//	    			newGraphINT = newGraph.getDependencyNode(i).getIndex();
//	    			oldGraphINT = oldGraph.getDependencyNode(i).getIndex();

	    			
//	    			newGraphINT = newGraph.getNode(i).getHeadIndex();
//	    			newGraphINT = newGraph.getDependencyNode(i).getHead() != null ? newGraph.getDependencyNode(i).getHead().getIndex() : -1;
//	    			oldGraphINT = oldGraph.getDependencyNode(i).getHead() != null ? oldGraph.getDependencyNode(i).getHead().getIndex() : -1;
	    			

//	    			newGraphINT = newGraph.getDependencyNode(i).getPredecessor() != null ? newGraph.getDependencyNode(i).getPredecessor().getIndex() : -1;
//	    			oldGraphINT = oldGraph.getDependencyNode(i).getPredecessor() != null ? oldGraph.getDependencyNode(i).getPredecessor().getIndex() : -1;
	    
//	    			newGraphINT = newGraph.getTokenNode(i).getSuccessor() != null ? newGraph.getTokenNode(i).getSuccessor().getIndex() : -1;
//	    			oldGraphINT = oldGraph.getTokenNode(i).getSuccessor() != null ? oldGraph.getTokenNode(i).getSuccessor().getIndex() : -1;
	
//	    			newGraphINT = newGraph.getDependencyNode(i).getLeftDependentCount();
//	    			oldGraphINT = oldGraph.getDependencyNode(i).getLeftDependentCount();
//
//	    			newGraphINT = newGraph.getDependencyNode(i).getRightDependentCount();
//	    			oldGraphINT = oldGraph.getDependencyNode(i).getRightDependentCount();
	    			
//	    			newGraphINT = newGraph.getDependencyNode(i).getRightmostDependent() != null ? newGraph.getNode(i).getRightmostDependent().getIndex() : -1;
//	    			oldGraphINT = oldGraph.getDependencyNode(i).getRightmostDependent() != null ? oldGraph.getDependencyNode(i).getRightmostDependent	().getIndex() : -1;
//	    			newGraphINT = newGraph.getDependencyNode(i).findComponent().getIndex();
//	    			oldGraphINT = oldGraph.getDependencyNode(i).findComponent().getIndex();
//
//	    			newGraphINT = newGraph.getDependencyNode(i).getRank();
//	    			oldGraphINT = oldGraph.getDependencyNode(i).getRank();

	    			
//	    			newGraphBOOL = newGraph.getDependencyNode(i).isRoot();
//	    			oldGraphBOOL = oldGraph.getDependencyNode(i).isRoot();
	    			
//	    			newGraphBOOL = newGraph.getDependencyNode(i).hasRightDependent();
//	    			oldGraphBOOL = oldGraph.getDependencyNode(i).hasRightDependent();
	    			
//	    			newGraphBOOL = newGraph.getDependencyNode(i).hasHead();
//	    			oldGraphBOOL = oldGraph.getDependencyNode(i).hasHead();
//	    	    	if (newGraphBOOL != oldGraphBOOL) {
//	    	    		System.out.println(newGraphBOOL + "\t" + oldGraphBOOL);
//	    	    	}
	    			
//		    		newGraphSortedSet = newGraph.getNode(i).getRightDependents();
//		    		oldGraphSortedSet = oldGraph.getDependencyNode(i).getLeftDependents();
//		    		if (newGraphSortedSet.size() != oldGraphSortedSet.size()) {
//		    			System.out.println(newGraphSortedSet + "\t" + oldGraphSortedSet);
//		    		} else {
//		    			Iterator<DependencyNode> it = oldGraphSortedSet.iterator();
//		    			for (Node n : newGraphSortedSet) {
//		    				DependencyNode o = it.next();
//		    				if (n.getIndex() != o.getIndex()) {
//		    					System.out.println(n.getIndex() + "\t" + o.getIndex());
//		    				}
//		    			}
//		    		}
//	    			if (newGraphINT != oldGraphINT) {
//	    				System.out.println(newGraphINT + "\t" + oldGraphINT);
//	    			}
//	    		}
	    		
	    		
//	    		System.out.println(oldGraph);
    		}
    	} catch (IOException e) {
			e.printStackTrace();
    	} catch (LWGraphException e) {
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
    	long elapsed = System.currentTimeMillis() - startTime;
    	System.out.println("Finished init basic   : " + new Formatter().format("%02d:%02d:%02d", elapsed/3600000, elapsed%3600000/60000, elapsed%60000/1000)+" ("+elapsed+" ms)");
	}


}
