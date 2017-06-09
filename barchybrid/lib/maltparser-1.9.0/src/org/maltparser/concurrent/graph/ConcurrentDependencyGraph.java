package org.maltparser.concurrent.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.maltparser.concurrent.graph.dataformat.ColumnDescription;
import org.maltparser.concurrent.graph.dataformat.DataFormat;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.node.DependencyNode;

/**
* Immutable and tread-safe dependency graph implementation.
* 
* @author Johan Hall
*/
public final class ConcurrentDependencyGraph {
	private static final String TAB_SIGN = "\t";
	private final DataFormat dataFormat;
	private final ConcurrentDependencyNode[] nodes;
	
	/**
	 * Creates a copy of a dependency graph
	 * 
	 * @param graph a dependency graph
	 * @throws ConcurrentGraphException
	 */
	public ConcurrentDependencyGraph(ConcurrentDependencyGraph graph) throws ConcurrentGraphException {
		this.dataFormat = graph.dataFormat;
		this.nodes = new ConcurrentDependencyNode[graph.nodes.length+1];
		
		for (int i = 0; i < graph.nodes.length; i++) {
			nodes[i] = new ConcurrentDependencyNode(this, (ConcurrentDependencyNode)graph.nodes[i]);
		}
	}
	
	/**
	 * Creates a immutable dependency graph
	 * 
	 * @param dataFormat a data format that describes the label types (or the columns in the input and output)
	 * @param inputTokens a string array of tokens. Each label is separated by a tab-character and must follow the order in the data format
	 * @throws ConcurrentGraphException
	 */
	public ConcurrentDependencyGraph(DataFormat dataFormat, String[] inputTokens) throws ConcurrentGraphException {
		this.dataFormat = dataFormat;
		this.nodes = new ConcurrentDependencyNode[inputTokens.length+1];
		
		// Add nodes
		nodes[0] = new ConcurrentDependencyNode(this, 0, null); // ROOT
		for (int i = 0; i < inputTokens.length; i++) {
			String[] columns = inputTokens[i].split(TAB_SIGN);
			nodes[i+1] = new ConcurrentDependencyNode(this, i+1, columns);
		}
		
		// Check graph
		for (int i = 0; i < nodes.length; i++) {
			if (nodes[i].getHeadIndex() >= nodes.length) {
				throw new ConcurrentGraphException("Not allowed to add a head node that doesn't exists");
			}
		}
	}
	
	/**
	 * Creates a immutable dependency graph
	 * 
	 * @param dataFormat a data format that describes the label types (or the columns in the input and output)
	 * @param sourceGraph a dependency graph that implements the interface org.maltparser.core.syntaxgraph.DependencyStructure
	 * @param defaultRootLabel the default root label
	 * @throws MaltChainedException
	 */
	public ConcurrentDependencyGraph(DataFormat dataFormat, DependencyStructure sourceGraph, String defaultRootLabel) throws MaltChainedException {
		this.dataFormat = dataFormat;
		this.nodes = new ConcurrentDependencyNode[sourceGraph.nDependencyNode()];
		
		// Add nodes
		nodes[0] = new ConcurrentDependencyNode(this, 0, null); // ROOT
		
		for (int index : sourceGraph.getTokenIndices()) {
			final DependencyNode gnode = sourceGraph.getDependencyNode(index);
			String[] columns = new String[dataFormat.numberOfColumns()];
			for (int i = 0; i < dataFormat.numberOfColumns(); i++) {
				ColumnDescription column = dataFormat.getColumnDescription(i);
				if (!column.isInternal()) {
					if (column.getCategory() == ColumnDescription.INPUT) {
						columns[i] = gnode.getLabelSymbol(sourceGraph.getSymbolTables().getSymbolTable(column.getName()));
					} else if (column.getCategory() == ColumnDescription.HEAD) {
						if (gnode.hasHead()) {
							columns[i] = Integer.toString(gnode.getHeadEdge().getSource().getIndex());
						} else {
							columns[i] = Integer.toString(-1);
						}
					} else if (column.getCategory() == ColumnDescription.DEPENDENCY_EDGE_LABEL) {
						SymbolTable sourceTable = sourceGraph.getSymbolTables().getSymbolTable(column.getName());
						if (gnode.getHeadEdge().hasLabel(sourceTable)) {
							columns[i] = gnode.getHeadEdge().getLabelSymbol(sourceTable);
						} else {
							columns[i] = defaultRootLabel;
						}
					} else {
						columns[i] = "_";
					}
				}
			}
			nodes[index] = new ConcurrentDependencyNode(this, index, columns);
		}
	}

	protected ConcurrentDependencyGraph(DataFormat dataFormat, ConcurrentDependencyNode[] inputNodes) throws ConcurrentGraphException {
		this.dataFormat = dataFormat;
		this.nodes = new ConcurrentDependencyNode[inputNodes.length];
		
		// Add nodes
		for (int i = 0; i < inputNodes.length; i++) {
			nodes[i] = inputNodes[i];
		}
		
		// Check graph
		for (int i = 0; i < nodes.length; i++) {
			if (nodes[i].getHeadIndex() >= nodes.length) {
				throw new ConcurrentGraphException("Not allowed to add a head node that doesn't exists");
			}
		}
	}
	
	/**
	 * Returns the data format that describes the label types (or the columns in the input and output)
	 * 
	 * @return the data format that describes the label types
	 */
	public DataFormat getDataFormat() {
		return dataFormat;
	}
	
	/**
	 * Returns the root node
	 * 
	 * @return the root node
	 */
	public ConcurrentDependencyNode getRoot() {
		return nodes[0];
	}
	
	/**
	 * Returns a dependency node specified by the node index
	 * 
	 * @param nodeIndex the index of the node
	 * @return a dependency node specified by the node index
	 */
//	public ConcurrentDependencyNode getNode(int nodeIndex) {
//		if (nodeIndex < 0 || nodeIndex >= nodes.length) {
//			return null;
//		}
//		return nodes[nodeIndex];
//	}
	
	/**
	 * Returns a dependency node specified by the node index. Index 0 equals the root node
	 * 
	 * @param index the index of the node
	 * @return a dependency node specified by the node index, if out of range <i>null</i> is returned.
	 */
	public ConcurrentDependencyNode getDependencyNode(int index) {
		if (index < 0 || index >= nodes.length) {
			return null;
		}
		return nodes[index];
	}
	
	/**
	 * Returns a dependency node specified by the node index. If index is equals to 0 (the root node) then null will be returned because this node
	 * is not a token node.
	 * 
	 * @param index the index of the node
	 * @return a dependency node specified by the node index, if out of range and root node then <i>null</i> is returned.
	 */
	public ConcurrentDependencyNode getTokenNode(int index) {
		if (index <= 0 || index >= nodes.length) {
			return null;
		}
		return nodes[index];
	}
	
	/**
	 * Returns the number of dependency nodes (including the root node)
	 * 
	 * @return the number of dependency nodes
	 */
	public int nDependencyNodes() {
		return nodes.length;
	}
	
	/**
	 * Returns the number of token nodes in the dependency graph (Number of dependency nodes - the root node).
	 * 
	 * @return the number of token nodes in the dependency graph.
	 */
	public int nTokenNodes() {
		return nodes.length - 1;
	}
	
	/**
	 * Returns the index of the last dependency node.
	 * 
	 * @return the index of the last dependency node.
	 */
	public int getHighestDependencyNodeIndex() {
		return nodes.length - 1;
	}
	
	/**
	 * Returns the index of the last token node.
	 * 
	 * @return the index of the last token node. If there are no token nodes then -1 is returned.
	 */
	public int getHighestTokenIndex() {
		if (nodes.length == 1) {
			return - 1;
		}
		return nodes.length - 1;
	}
	
	
	/**
	 *  Returns <i>true</i> if the dependency graph has any token nodes, otherwise <i>false</i>.
	 * 
	 * @return <i>true</i> if the dependency graph has any token nodes, otherwise <i>false</i>.
	 */
	public boolean hasTokens() {
		return nodes.length > 1;	
	}
	
	/**
	 * Returns the number of edges
	 * 
	 * @return the number of edges
	 */
	public int nEdges() {
		int n = 0;
		for (int i = 1; i < nodes.length; i++) {
			if (nodes[i].hasHead()) {
				n++;
			}
		}
		return n;
	}
	/**
	 * Returns a sorted set of edges. If no edges are found an empty set is returned
	 * 
	 * @return a sorted set of edges.
	 * @throws ConcurrentGraphException
	 */
	public SortedSet<ConcurrentDependencyEdge> getEdges() throws ConcurrentGraphException {
		SortedSet<ConcurrentDependencyEdge> edges = Collections.synchronizedSortedSet(new TreeSet<ConcurrentDependencyEdge>());
		for (int i = 1; i < nodes.length; i++) {
			ConcurrentDependencyEdge edge = nodes[i].getHeadEdge();
			if (edge != null) {
				edges.add(edge);
			}
		}
		return edges;
	}
	
	/**
	 * Returns a sorted set of integers {0,s,..n} , where each index i identifies a dependency node. Index 0
	 * should always be the root dependency node and index s is the first terminal node and index n is the
	 * last terminal node.  
	 * 
	 * @return a sorted set of integers
	 */
	public SortedSet<Integer> getDependencyIndices() {
		SortedSet<Integer> indices = Collections.synchronizedSortedSet(new TreeSet<Integer>());
		for (int i = 0; i < nodes.length; i++) {
			indices.add(i);
		}
		return indices;
	}
	
	/**
	 * Returns a sorted set of integers {s,...,n}, where each index i identifies a token node. Index <i>s</i> 
	 * is the first token node and index <i>n</i> is the last token node. 
	 * 
	 * @return a sorted set of integers {s,...,n}. If there are no token nodes then an empty set is returned.
	 */
	public SortedSet<Integer> getTokenIndices() {
		SortedSet<Integer> indices = Collections.synchronizedSortedSet(new TreeSet<Integer>());
		for (int i = 1; i < nodes.length; i++) {
			indices.add(i);
		}
		return indices;
	}
	
	/**
	 * Returns <i>true</i> if the head edge of the dependency node with <i>index</i> is labeled, otherwise <i>false</i>.
	 * 
	 * @param index the index of the dependency node
	 * @return <i>true</i> if the head edge of the dependency node with <i>index</i> is labeled, otherwise <i>false</i>.
	 */
	public boolean hasLabeledDependency(int index) {
		if (index < 0 || index >= nodes.length) {
			return false;
		}
		if (!nodes[index].hasHead()) {
			return false;
		}
		return nodes[index].isHeadLabeled();
	}
	
	/**
	 * Returns <i>true</i> if all nodes in the dependency structure are connected, otherwise <i>false</i>.
	 * 
	 * @return <i>true</i> if all nodes in the dependency structure are connected, otherwise <i>false</i>.
	 */
	public boolean isConnected() {
		int[] components = findComponents();
		int tmp = components[0];
		for (int i = 1; i < components.length; i++) {
			if (tmp != components[i]) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Returns <i>true</i> if all edges in the dependency structure are projective, otherwise <i>false</i>.
	 * 
	 * @return <i>true</i> if all edges in the dependency structure are projective, otherwise <i>false</i>.
	 */
	public boolean isProjective() {
		for (int i = 1; i < nodes.length; i++) {
			if (!nodes[i].isProjective()) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Returns <i>true</i> if all dependency nodes have at most one incoming edge, otherwise <i>false</i>.
	 * 
	 * Note: In this implementation this will always be <i>true</i>
	 * 
	 * @return  <i>true</i> if all dependency nodes have at most one incoming edge, otherwise <i>false</i>.
	 */
	public boolean isSingleHeaded() {
		return true;
	}
	
	/**
	 * Returns <i>true</i> if the dependency structure are a tree (isConnected() && isSingleHeaded()), otherwise <i>false</i>.
	 * 
	 * @return <i>true</i> if the dependency structure are a tree (isConnected() && isSingleHeaded()), otherwise <i>false</i>.
	 */ 
	public boolean isTree() {
		return isConnected() && isSingleHeaded();
	}
	
	/**
	 * Returns the number of non-projective edges in the dependency structure.
	 * 
	 * @return the number of non-projective edges in the dependency structure.
	 */
	public int nNonProjectiveEdges() {
		int c = 0;
		for (int i = 1; i < nodes.length; i++) {
			if (!nodes[i].isProjective()) {
				c++;
			}
		}
		return c;
	}
	
	protected boolean hasDependent(int nodeIndex) {
		for (int i = 1; i < nodes.length; i++) {
			if (nodeIndex == nodes[i].getHeadIndex()) {
				return true;
			}
		}
		return false;
	}
	
	protected boolean hasLeftDependent(int nodeIndex) {
		for (int i = 1; i < nodeIndex; i++) {
			if (nodeIndex == nodes[i].getHeadIndex()) {
				return true;
			}
		}
		return false;
	}
	
	protected boolean hasRightDependent(int nodeIndex) {
		for (int i = nodeIndex + 1; i < nodes.length; i++) {
			if (nodeIndex == nodes[i].getHeadIndex()) {
				return true;
			}
		}
		return false;
	}
	
	protected List<ConcurrentDependencyNode> getListOfLeftDependents(int nodeIndex) {
		List<ConcurrentDependencyNode> leftDependents = Collections.synchronizedList(new ArrayList<ConcurrentDependencyNode>());
		for (int i = 1; i < nodeIndex; i++) {
			if (nodeIndex == nodes[i].getHeadIndex()) {
				leftDependents.add(nodes[i]);
			}
		}
		return leftDependents;
	}
	
	protected SortedSet<ConcurrentDependencyNode> getSortedSetOfLeftDependents(int nodeIndex) {
		SortedSet<ConcurrentDependencyNode> leftDependents = Collections.synchronizedSortedSet(new TreeSet<ConcurrentDependencyNode>());
		for (int i = 1; i < nodeIndex; i++) {
			if (nodeIndex == nodes[i].getHeadIndex()) {
				leftDependents.add(nodes[i]);
			}
		}
		return leftDependents;
	}
	
	protected List<ConcurrentDependencyNode> getListOfRightDependents(int nodeIndex) {
		List<ConcurrentDependencyNode> rightDependents = Collections.synchronizedList(new ArrayList<ConcurrentDependencyNode>());
		for (int i = nodeIndex + 1; i < nodes.length; i++) {
			if (nodeIndex == nodes[i].getHeadIndex()) {
				rightDependents.add(nodes[i]);
			}
		}
		return rightDependents;
	}
	
	protected SortedSet<ConcurrentDependencyNode> getSortedSetOfRightDependents(int nodeIndex) {
		SortedSet<ConcurrentDependencyNode> rightDependents = Collections.synchronizedSortedSet(new TreeSet<ConcurrentDependencyNode>());
		for (int i = nodeIndex + 1; i < nodes.length; i++) {
			if (nodeIndex == nodes[i].getHeadIndex()) {
				rightDependents.add(nodes[i]);
			}
		}
		return rightDependents;
	}
	
	protected List<ConcurrentDependencyNode> getListOfDependents(int nodeIndex) {
		List<ConcurrentDependencyNode> dependents = Collections.synchronizedList(new ArrayList<ConcurrentDependencyNode>());
		for (int i = 1; i < nodes.length; i++) {
			if (nodeIndex == nodes[i].getHeadIndex()) {
				dependents.add(nodes[i]);
			}
		}
		return dependents;
	}
	
	protected SortedSet<ConcurrentDependencyNode> getSortedSetOfDependents(int nodeIndex) {
		SortedSet<ConcurrentDependencyNode> dependents = Collections.synchronizedSortedSet(new TreeSet<ConcurrentDependencyNode>());
		for (int i = 1; i < nodes.length; i++) {
			if (nodeIndex == nodes[i].getHeadIndex()) {
				dependents.add(nodes[i]);
			}
		}
		return dependents;
	}
	
	protected int getRank(int nodeIndex) {
		int[] components = new int[nodes.length];
		int[] ranks = new int[nodes.length];
		for (int i = 0; i < components.length; i++) {
			components[i] = i;
			ranks[i] = 0;
		}
		for (int i = 1; i < nodes.length; i++) {
			if (nodes[i].hasHead()) {
				int hcIndex = findComponent(nodes[i].getHead().getIndex(), components);
				int dcIndex = findComponent(nodes[i].getIndex(), components);
				if (hcIndex != dcIndex) {
					link(hcIndex, dcIndex, components, ranks);		
				}
			}
		}
		return ranks[nodeIndex];
	}
	
	protected ConcurrentDependencyNode findComponent(int nodeIndex) {
		int[] components = new int[nodes.length];
		int[] ranks = new int[nodes.length];
		for (int i = 0; i < components.length; i++) {
			components[i] = i;
			ranks[i] = 0;
		}
		for (int i = 1; i < nodes.length; i++) {
			if (nodes[i].hasHead()) {
				int hcIndex = findComponent(nodes[i].getHead().getIndex(), components);
				int dcIndex = findComponent(nodes[i].getIndex(), components);
				if (hcIndex != dcIndex) {
					link(hcIndex, dcIndex, components, ranks);		
				}
			}
		}
		return nodes[findComponent(nodeIndex, components)];
	}
	
	private int[] findComponents() {
		int[] components = new int[nodes.length];
		int[] ranks = new int[nodes.length];
		for (int i = 0; i < components.length; i++) {
			components[i] = i;
			ranks[i] = 0;
		}
		for (int i = 1; i < nodes.length; i++) {
			if (nodes[i].hasHead()) {
				int hcIndex = findComponent(nodes[i].getHead().getIndex(), components);
				int dcIndex = findComponent(nodes[i].getIndex(), components);
				if (hcIndex != dcIndex) {
					link(hcIndex, dcIndex, components, ranks);		
				}
			}
		}
		return components;
	}
	
	private int findComponent(int xIndex, int[] components) {
		if (xIndex != components[xIndex]) {
			components[xIndex] = findComponent(components[xIndex], components);
		}
		return components[xIndex];
	}
	
	private int link(int xIndex, int yIndex, int[] components, int[] ranks) {
		if (ranks[xIndex] > ranks[yIndex]) {  
			components[yIndex] = xIndex;
		} else {
			components[xIndex] = yIndex;

			if (ranks[xIndex] == ranks[yIndex]) {
				ranks[yIndex]++;
			}
			return yIndex;
		}
		return xIndex;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dataFormat == null) ? 0 : dataFormat.hashCode());
		result = prime * result + Arrays.hashCode(nodes);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConcurrentDependencyGraph other = (ConcurrentDependencyGraph) obj;
		if (dataFormat == null) {
			if (other.dataFormat != null)
				return false;
		} else if (!dataFormat.equals(other.dataFormat))
			return false;
		if (!Arrays.equals(nodes, other.nodes))
			return false;
		return true;
	}

	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (int i = 1; i < nodes.length; i++) {
			sb.append(nodes[i]);
			sb.append('\n');
		}
		return sb.toString();
	}
}
