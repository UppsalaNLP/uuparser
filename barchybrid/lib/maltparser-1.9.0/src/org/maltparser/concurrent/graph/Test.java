package org.maltparser.concurrent.graph;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Formatter;
import java.util.SortedSet;

import org.maltparser.concurrent.ConcurrentUtils;
import org.maltparser.concurrent.graph.dataformat.ColumnDescription;
import org.maltparser.concurrent.graph.dataformat.DataFormat;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.hash.HashSymbolTableHandler;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.core.syntaxgraph.node.DependencyNode;

/**
* 
* @author Johan Hall
*/
public class Test {
	private static final String IGNORE_COLUMN_SIGN = "_";
	public static DependencyStructure getOldDependencyGraph(DataFormat dataFormat, String[] tokens) throws MaltChainedException {
		DependencyStructure oldGraph = new org.maltparser.core.syntaxgraph.DependencyGraph(new HashSymbolTableHandler());
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
	    		String[] goldTokens = ConcurrentUtils.readSentence(reader);
	    		if (goldTokens.length == 0) {
	    			break;
	    		}
	    		sentenceCounter++;
	    		ConcurrentDependencyGraph newGraph = new ConcurrentDependencyGraph(dataFormat, goldTokens);
	    		DependencyStructure oldGraph = getOldDependencyGraph(dataFormat, goldTokens);
	    		int newGraphINT;
	    		int oldGraphINT;
	    		boolean newGraphBOOL;
	    		boolean oldGraphBOOL;
	    		SortedSet<ConcurrentDependencyNode> newGraphSortedSet;
	    		SortedSet<DependencyNode> oldGraphSortedSet;
	    		
	    		for (int i = 0; i < newGraph.nDependencyNodes(); i++) {
//	    			newGraphINT = newGraph.getNode(i).getIndex();
//	    			oldGraphINT = oldGraph.getDependencyNode(i).getIndex();
	    			
//	    			newGraphINT = newGraph.getNode(i).getHeadIndex();
//	    			newGraphINT = newGraph.getNode(i).getHead() != null ? newGraph.getNode(i).getHead().getIndex() : -1;
//	    			oldGraphINT = oldGraph.getDependencyNode(i).getHead() != null ? oldGraph.getDependencyNode(i).getHead().getIndex() : -1;
	    			

//	    			newGraphINT = newGraph.getNode(i).getPredecessor() != null ? newGraph.getNode(i).getPredecessor().getIndex() : -1;
//	    			oldGraphINT = oldGraph.getTokenNode(i).getPredecessor() != null ? oldGraph.getTokenNode(i).getPredecessor().getIndex() : -1;
	    
//	    			newGraphINT = newGraph.getNode(i).getSuccessor() != null ? newGraph.getNode(i).getSuccessor().getIndex() : -1;
//	    			oldGraphINT = oldGraph.getTokenNode(i).getSuccessor() != null ? oldGraph.getTokenNode(i).getSuccessor().getIndex() : -1;
	
//	    			newGraphINT = newGraph.getNode(i).getLeftDependentCount();
//	    			oldGraphINT = oldGraph.getDependencyNode(i).getLeftDependentCount();
//
//	    			newGraphINT = newGraph.getNode(i).getRightDependentCount();
//	    			oldGraphINT = oldGraph.getDependencyNode(i).getRightDependentCount();
	    			
//	    			newGraphINT = newGraph.getNode(i).getRightmostDependent() != null ? newGraph.getNode(i).getRightmostDependent().getIndex() : -1;
//	    			oldGraphINT = oldGraph.getDependencyNode(i).getRightmostDependent() != null ? oldGraph.getDependencyNode(i).getRightmostDependent	().getIndex() : -1;
	    			newGraphINT = newGraph.getDependencyNode(i).findComponent().getIndex();
	    			oldGraphINT = oldGraph.getDependencyNode(i).findComponent().getIndex();

	    			newGraphINT = newGraph.getDependencyNode(i).getRank();
	    			oldGraphINT = oldGraph.getDependencyNode(i).getRank();
	    			if (newGraphINT != oldGraphINT) {
	    				System.out.println(newGraphINT + "\t" + oldGraphINT);
	    			}
	    			
//	    			newGraphBOOL = newGraph.getNode(i).isRoot();
//	    			oldGraphBOOL = oldGraph.getDependencyNode(i).isRoot();
	    			
//	    			newGraphBOOL = newGraph.getNode(i).hasRightDependent();
//	    			oldGraphBOOL = oldGraph.getDependencyNode(i).hasRightDependent();
	    			
//	    			newGraphBOOL = newGraph.getNode(i).hasHead();
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
	    			
	    		}
	    		
	    		
//	    		System.out.println(oldGraph);
    		}
    	} catch (IOException e) {
			e.printStackTrace();
    	} catch (ConcurrentGraphException e) {
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
