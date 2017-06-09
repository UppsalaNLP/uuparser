package org.maltparser.core.lw.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.core.syntaxgraph.node.DependencyNode;

/**
* A lightweight version of pseudo projectivity and this class can only perform deprojectivizing. The class is based on 
* the more complex class org.maltparser.transform.pseudo.PseudoProjectivity.
* 
* 
* @author Johan Hall
*/
public final class LWDeprojectivizer {
	public static final int NONE = 0;
	public static final int BASELINE = 1;
	public static final int HEAD = 1;
	public static final int PATH = 1;
	public static final int HEADPATH = 1;
	public static final int TRACE = 1;

	public LWDeprojectivizer() { }
	
	public static int getMarkingStrategyInt(String markingStrategyString) {
		if (markingStrategyString.equalsIgnoreCase("none")) {
			return LWDeprojectivizer.NONE;
		} else if (markingStrategyString.equalsIgnoreCase("baseline")) {
			return LWDeprojectivizer.BASELINE;
		} else if (markingStrategyString.equalsIgnoreCase("head")) {
			return LWDeprojectivizer.HEAD;
		} else if (markingStrategyString.equalsIgnoreCase("path")) {
			return LWDeprojectivizer.PATH;
		} else if (markingStrategyString.equalsIgnoreCase("head+path")) {
			return LWDeprojectivizer.HEADPATH;
		} else if (markingStrategyString.equalsIgnoreCase("trace")) {
			return LWDeprojectivizer.TRACE;
		} 
		return LWDeprojectivizer.NONE;
	}
	
	public void deprojectivize(DependencyStructure pdg, int markingStrategy) throws MaltChainedException {
		SymbolTable deprelSymbolTable = pdg.getSymbolTables().getSymbolTable("DEPREL");
		SymbolTable ppliftedSymbolTable =  pdg.getSymbolTables().getSymbolTable("PPLIFTED"); 
		SymbolTable pppathSymbolTable =  pdg.getSymbolTables().getSymbolTable("PPPATH");
		 
		boolean[] nodeLifted = new boolean[pdg.nDependencyNode()]; Arrays.fill(nodeLifted, false);
		boolean[] nodePath = new boolean[pdg.nDependencyNode()]; Arrays.fill(nodePath, false);
		String[] synacticHeadDeprel = new String[pdg.nDependencyNode()]; Arrays.fill(synacticHeadDeprel, null);
		
		for (int index : pdg.getTokenIndices()) {
			Edge e = pdg.getDependencyNode(index).getHeadEdge();
			if (e.hasLabel(deprelSymbolTable)) {
				if (e.hasLabel(pppathSymbolTable) && pppathSymbolTable.getSymbolCodeToString(e.getLabelCode(pppathSymbolTable)).equals("#true#")) {
					nodePath[pdg.getDependencyNode(index).getIndex()] = true;
				}
				if (e.hasLabel(ppliftedSymbolTable) && !ppliftedSymbolTable.getSymbolCodeToString(e.getLabelCode(ppliftedSymbolTable)).equals("#false#")) {
					nodeLifted[index] = true;
					
					if (!ppliftedSymbolTable.getSymbolCodeToString(e.getLabelCode(ppliftedSymbolTable)).equals("#true#")) {
						synacticHeadDeprel[index] = ppliftedSymbolTable.getSymbolCodeToString(e.getLabelCode(ppliftedSymbolTable));
					}
				}
			}
		}
		deattachCoveredRootsForDeprojectivization(pdg, deprelSymbolTable);
		if (markingStrategy == LWDeprojectivizer.HEAD && needsDeprojectivizeWithHead(pdg, nodeLifted, nodePath, synacticHeadDeprel, deprelSymbolTable)) {
			deprojectivizeWithHead(pdg, pdg.getDependencyRoot(), nodeLifted, nodePath, synacticHeadDeprel, deprelSymbolTable);
		} else if (markingStrategy == LWDeprojectivizer.PATH) {
			deprojectivizeWithPath(pdg, pdg.getDependencyRoot(), nodeLifted, nodePath);
		} else if (markingStrategy == LWDeprojectivizer.HEADPATH) {
			deprojectivizeWithHeadAndPath(pdg, pdg.getDependencyRoot(), nodeLifted, nodePath, synacticHeadDeprel, deprelSymbolTable);
		}
	}

	private void deattachCoveredRootsForDeprojectivization(DependencyStructure pdg, SymbolTable deprelSymbolTable) throws MaltChainedException {
		SymbolTable ppcoveredRootSymbolTable =  pdg.getSymbolTables().getSymbolTable("PPCOVERED");
		for (int index : pdg.getTokenIndices()) {
			Edge e = pdg.getDependencyNode(index).getHeadEdge();
			if (e.hasLabel(deprelSymbolTable)) {
				if (e.hasLabel(ppcoveredRootSymbolTable) && ppcoveredRootSymbolTable.getSymbolCodeToString(e.getLabelCode(ppcoveredRootSymbolTable)).equals("#true#")) {
					pdg.moveDependencyEdge(pdg.getDependencyRoot().getIndex(), pdg.getDependencyNode(index).getIndex());
				}
			}
		}
	}

	// Check whether there is at least one node in the specified dependency structure that can be lifted.
	// If this is not the case, there is no need to call deprojectivizeWithHead.

	private boolean needsDeprojectivizeWithHead(DependencyStructure pdg, boolean[] nodeLifted, boolean[] nodePath, String[] synacticHeadDeprel, SymbolTable deprelSymbolTable) throws MaltChainedException {
		for (int index : pdg.getDependencyIndices()) {
			if (nodeLifted[index]) {
				DependencyNode node = pdg.getDependencyNode(index);
				if (breadthFirstSearchSortedByDistanceForHead(pdg, node.getHead(), node, synacticHeadDeprel[index], nodePath, deprelSymbolTable) != null) {
					return true;
				}
		    }
		}
		return false;
	}

	private boolean deprojectivizeWithHead(DependencyStructure pdg, DependencyNode node, boolean[] nodeLifted, boolean[] nodePath, String[] synacticHeadDeprel, SymbolTable deprelSymbolTable) throws MaltChainedException {
		boolean success = true, childSuccess = false;
		int i, childAttempts = 2;
		DependencyNode possibleSyntacticHead;
		String syntacticHeadDeprel;
		if (nodeLifted[node.getIndex()]) {
			syntacticHeadDeprel = synacticHeadDeprel[node.getIndex()];
			possibleSyntacticHead = breadthFirstSearchSortedByDistanceForHead(pdg, node.getHead(), node, syntacticHeadDeprel, nodePath, deprelSymbolTable);
			if (possibleSyntacticHead != null) {
				pdg.moveDependencyEdge(possibleSyntacticHead.getIndex(), node.getIndex());
				nodeLifted[node.getIndex()] = false;
			} else {
				success = false;
			}
		}
		while (!childSuccess && childAttempts > 0) {
			childSuccess = true;
			
			List<DependencyNode> children = node.getListOfDependents();
			for (i = 0; i < children.size(); i++) {
				if (!deprojectivizeWithHead(pdg, children.get(i), nodeLifted, nodePath, synacticHeadDeprel, deprelSymbolTable)) {
					childSuccess = false;
				}
			}
			childAttempts--;
		}
		return childSuccess && success;
	}

	private DependencyNode breadthFirstSearchSortedByDistanceForHead(DependencyStructure dg, DependencyNode start, DependencyNode avoid, String syntacticHeadDeprel, boolean[] nodePath, SymbolTable deprelSymbolTable)
			throws MaltChainedException {
		DependencyNode dependent;
		String dependentDeprel;
		List<DependencyNode> nodes = new ArrayList<DependencyNode>();
		nodes.addAll(findAllDependentsVectorSortedByDistanceToPProjNode(dg, start, avoid, false, nodePath));
		while (nodes.size() > 0) {
			dependent = nodes.remove(0);
			if (dependent.getHeadEdge().hasLabel(deprelSymbolTable)) {
				dependentDeprel = deprelSymbolTable.getSymbolCodeToString(dependent.getHeadEdge().getLabelCode(deprelSymbolTable));
				if (dependentDeprel.equals(syntacticHeadDeprel)) {
					return dependent;
				}
			}
			nodes.addAll(findAllDependentsVectorSortedByDistanceToPProjNode(dg, dependent, avoid, false, nodePath));
		}
		return null;
	}

	
	private List<DependencyNode> findAllDependentsVectorSortedByDistanceToPProjNode(DependencyStructure dg, DependencyNode governor, DependencyNode avoid,
			boolean percentOnly, boolean[] nodePath) {
		List<DependencyNode> output = new ArrayList<DependencyNode>();
		List<DependencyNode> dependents = governor.getListOfDependents();
//		SortedSet<DependencyNode> dependents = new TreeSet<DependencyNode>();
//		dependents.addAll(governor.getLeftDependents());
//		dependents.addAll(governor.getRightDependents());


		DependencyNode[] deps = new DependencyNode[dependents.size()];
		int[] distances = new int[dependents.size()];
		for (int i = 0; i < dependents.size(); i++) {
			distances[i] = Math.abs(dependents.get(i).getIndex() - avoid.getIndex());
			deps[i] = dependents.get(i);
		}
		if (distances.length > 1) {
			int smallest;
			int n = distances.length;
			int tmpDist;
			DependencyNode tmpDep;
			for (int i=0; i < n; i++) {
				smallest = i;
				for (int j=i; j < n; j++) {
					if (distances[j] < distances[smallest]) {
						smallest = j;
					}
				}
				if (smallest != i) {
					tmpDist = distances[smallest];
					distances[smallest] = distances[i];
					distances[i] = tmpDist;
					tmpDep = deps[smallest];
					deps[smallest] = deps[i];
					deps[i] = tmpDep;
				}
			}
		}
		for (int i=0; i<distances.length;i++) {
			if (deps[i] != avoid && (!percentOnly || (percentOnly && nodePath[deps[i].getIndex()]))) {
				output.add(deps[i]);
			}
		}
		return output;
	}
	
	private boolean deprojectivizeWithPath(DependencyStructure pdg, DependencyNode node, boolean[] nodeLifted, boolean[] nodePath) throws MaltChainedException {
		boolean success = true, childSuccess = false;
		int i, childAttempts = 2;
		DependencyNode possibleSyntacticHead;
		if (node.hasHead() && node.getHeadEdge().isLabeled() && nodeLifted[node.getIndex()] && nodePath[node.getIndex()]) {
			possibleSyntacticHead = breadthFirstSearchSortedByDistanceForPath(pdg, node.getHead(), node, nodePath);
			if (possibleSyntacticHead != null) {
				pdg.moveDependencyEdge(possibleSyntacticHead.getIndex(), node.getIndex());
				nodeLifted[node.getIndex()] = false;
			} else {
				success = false;
			}
		}
		if (node.hasHead() && node.getHeadEdge().isLabeled() && nodeLifted[node.getIndex()]) {
			possibleSyntacticHead = breadthFirstSearchSortedByDistanceForPath(pdg, node.getHead(), node, nodePath);
			if (possibleSyntacticHead != null) {
				pdg.moveDependencyEdge(possibleSyntacticHead.getIndex(), node.getIndex());
				nodeLifted[node.getIndex()] = false;
			} else {
				success = false;
			}
		}
		while (!childSuccess && childAttempts > 0) {
			childSuccess = true;
			List<DependencyNode> children = node.getListOfDependents();
			for (i = 0; i < children.size(); i++) {
				if (!deprojectivizeWithPath(pdg, children.get(i), nodeLifted, nodePath)) {
					childSuccess = false;
				}
			}
			childAttempts--;
		}
		return childSuccess && success;
	}

	private DependencyNode breadthFirstSearchSortedByDistanceForPath(DependencyStructure dg, DependencyNode start, DependencyNode avoid, boolean[] nodePath) {
		DependencyNode dependent;
		List<DependencyNode> nodes = new ArrayList<DependencyNode>(), newNodes;
		nodes.addAll(findAllDependentsVectorSortedByDistanceToPProjNode(dg, start, avoid, true, nodePath));
		while (nodes.size() > 0) {
			dependent = nodes.remove(0);
			if (((newNodes = findAllDependentsVectorSortedByDistanceToPProjNode(dg, dependent, avoid, true, nodePath)).size()) == 0) {
				return dependent;
			}
			nodes.addAll(newNodes);
		}
		return null;
	}

	private boolean deprojectivizeWithHeadAndPath(DependencyStructure pdg, DependencyNode node, boolean[] nodeLifted, boolean[] nodePath, String[] synacticHeadDeprel, SymbolTable deprelSymbolTable) throws MaltChainedException {
		boolean success = true, childSuccess = false;
		int i, childAttempts = 2;
		DependencyNode possibleSyntacticHead;
		if (node.hasHead() && node.getHeadEdge().isLabeled() && nodeLifted[node.getIndex()] && nodePath[node.getIndex()]) {
			possibleSyntacticHead = breadthFirstSearchSortedByDistanceForHeadAndPath(pdg, node.getHead(), node, synacticHeadDeprel[node.getIndex()], nodePath, deprelSymbolTable);
			if (possibleSyntacticHead != null) {
				pdg.moveDependencyEdge(possibleSyntacticHead.getIndex(), node.getIndex());
				nodeLifted[node.getIndex()] = false;
			} else {
				success = false;
			}
		}
		if (node.hasHead() && node.getHeadEdge().isLabeled() && nodeLifted[node.getIndex()]) {
			possibleSyntacticHead = breadthFirstSearchSortedByDistanceForHeadAndPath(pdg, node.getHead(), node, synacticHeadDeprel[node.getIndex()], nodePath, deprelSymbolTable);
			if (possibleSyntacticHead != null) {
				pdg.moveDependencyEdge(possibleSyntacticHead.getIndex(), node.getIndex());
				nodeLifted[node.getIndex()] = false;
			} else {
				success = false;
			}
		}
		while (!childSuccess && childAttempts > 0) {
			childSuccess = true;
			List<DependencyNode> children = node.getListOfDependents();
			for (i = 0; i < children.size(); i++) {
				if (!deprojectivizeWithHeadAndPath(pdg, children.get(i), nodeLifted, nodePath, synacticHeadDeprel, deprelSymbolTable)) {
					childSuccess = false;
				}
			}
			childAttempts--;
		}
		return childSuccess && success;
	}

	private DependencyNode breadthFirstSearchSortedByDistanceForHeadAndPath(DependencyStructure dg, DependencyNode start, DependencyNode avoid, String syntacticHeadDeprelCode, boolean[] nodePath, SymbolTable deprelSymbolTable)
			throws MaltChainedException {
		DependencyNode dependent;
		List<DependencyNode> nodes = new ArrayList<DependencyNode>(), newNodes = null, secondChance = new ArrayList<DependencyNode>();
		nodes.addAll(findAllDependentsVectorSortedByDistanceToPProjNode(dg, start, avoid, true, nodePath));
		while (nodes.size() > 0) {
			dependent = nodes.remove(0);
			if (((newNodes = findAllDependentsVectorSortedByDistanceToPProjNode(dg, dependent, avoid, true, nodePath)).size()) == 0
					&& deprelSymbolTable.getSymbolCodeToString(dependent.getHeadEdge().getLabelCode(deprelSymbolTable)).equals(syntacticHeadDeprelCode)) {
				return dependent;
			}
			nodes.addAll(newNodes);
			if (deprelSymbolTable.getSymbolCodeToString(dependent.getHeadEdge().getLabelCode(deprelSymbolTable)).equals(syntacticHeadDeprelCode)
					&& newNodes.size() != 0) {
				secondChance.add(dependent);
			}
		}
		if (secondChance.size() > 0) {
			return secondChance.get(0);
		}
		return null;
	}
}
