package org.maltparser.core.lw.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import org.maltparser.concurrent.graph.dataformat.ColumnDescription;
import org.maltparser.concurrent.graph.dataformat.DataFormat;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.helper.HashMap;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.Element;
import org.maltparser.core.syntaxgraph.LabelSet;
import org.maltparser.core.syntaxgraph.RootLabels;
import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.core.syntaxgraph.node.ComparableNode;
import org.maltparser.core.syntaxgraph.node.DependencyNode;
import org.maltparser.core.syntaxgraph.node.TokenNode;

/**
* A lightweight version of org.maltparser.core.syntaxgraph.DependencyGraph.
*
* @author Johan Hall
*/
public final class LWDependencyGraph implements DependencyStructure {
	private static final String TAB_SIGN = "\t";

	private final DataFormat dataFormat;
	private final SymbolTableHandler symbolTables;
	private final RootLabels rootLabels;
	private final List<LWNode> nodes;
	private final HashMap<Integer, ArrayList<String>> comments;

	public LWDependencyGraph(DataFormat _dataFormat, SymbolTableHandler _symbolTables) throws MaltChainedException {
		this.dataFormat = _dataFormat;
		this.symbolTables = _symbolTables;
		this.rootLabels = new RootLabels();
		this.nodes = new ArrayList<LWNode>();
		this.nodes.add(new LWNode(this, 0)); // ROOT
		this.comments = new HashMap<Integer, ArrayList<String>>();
	}

	public LWDependencyGraph(DataFormat _dataFormat, SymbolTableHandler _symbolTables, String[] inputTokens, String defaultRootLabel) throws MaltChainedException {
		this(_dataFormat, _symbolTables, inputTokens, defaultRootLabel, true);
	}

	public LWDependencyGraph(DataFormat _dataFormat, SymbolTableHandler _symbolTables, String[] inputTokens, String defaultRootLabel, boolean addEdges) throws MaltChainedException {
		this.dataFormat = _dataFormat;
		this.symbolTables = _symbolTables;
		this.rootLabels = new RootLabels();
		this.nodes = new ArrayList<LWNode>(inputTokens.length+1);
		this.comments = new HashMap<Integer, ArrayList<String>>();
		resetTokens(inputTokens, defaultRootLabel, addEdges);
	}

	public void resetTokens(String[] inputTokens, String defaultRootLabel, boolean addEdges) throws MaltChainedException {
		nodes.clear();
		comments.clear();
		symbolTables.cleanUp();
		// Add nodes
		nodes.add(new LWNode(this, 0)); // ROOT
		for (int i = 0; i < inputTokens.length; i++) {
			nodes.add(new LWNode(this, i+1));
		}

		for (int i = 0; i < inputTokens.length; i++) {
			nodes.get(i+1).addColumnLabels(inputTokens[i].split(TAB_SIGN), addEdges);
		}
		// Check graph
		for (int i = 0; i < nodes.size(); i++) {
			if (nodes.get(i).getHeadIndex() >= nodes.size()) {
				throw new LWGraphException("Not allowed to add a head node that doesn't exists");
			}
		}

		for (int i = 0; i < dataFormat.numberOfColumns(); i++) {
			ColumnDescription column = dataFormat.getColumnDescription(i);
			if (!column.isInternal() && column.getCategory() == ColumnDescription.DEPENDENCY_EDGE_LABEL) {
				rootLabels.setDefaultRootLabel(symbolTables.getSymbolTable(column.getName()), defaultRootLabel);
			}
		}
	}

	public DataFormat getDataFormat() {
		return dataFormat;
	}

	public LWNode getNode(int nodeIndex) {
		if (nodeIndex < 0 || nodeIndex >= nodes.size()) {
			return null;
		}
		return nodes.get(nodeIndex);
	}

	public int nNodes() {
		return nodes.size();
	}

	protected boolean hasDependent(int nodeIndex) {
		for (int i = 1; i < nodes.size(); i++) {
			if (nodeIndex == nodes.get(i).getHeadIndex()) {
				return true;
			}
		}
		return false;
	}

	protected boolean hasLeftDependent(int nodeIndex) {
		for (int i = 1; i < nodeIndex; i++) {
			if (nodeIndex == nodes.get(i).getHeadIndex()) {
				return true;
			}
		}
		return false;
	}

	protected boolean hasRightDependent(int nodeIndex) {
		for (int i = nodeIndex + 1; i < nodes.size(); i++) {
			if (nodeIndex == nodes.get(i).getHeadIndex()) {
				return true;
			}
		}
		return false;
	}

	protected List<DependencyNode> getListOfLeftDependents(int nodeIndex) {
		List<DependencyNode> leftDependents = Collections.synchronizedList(new ArrayList<DependencyNode>());
		for (int i = 1; i < nodeIndex; i++) {
			if (nodeIndex == nodes.get(i).getHeadIndex()) {
				leftDependents.add(nodes.get(i));
			}
		}
		return leftDependents;
	}

	protected SortedSet<DependencyNode> getSortedSetOfLeftDependents(int nodeIndex) {
		SortedSet<DependencyNode> leftDependents = Collections.synchronizedSortedSet(new TreeSet<DependencyNode>());
		for (int i = 1; i < nodeIndex; i++) {
			if (nodeIndex == nodes.get(i).getHeadIndex()) {
				leftDependents.add(nodes.get(i));
			}
		}
		return leftDependents;
	}

	protected List<DependencyNode> getListOfRightDependents(int nodeIndex) {
		List<DependencyNode> rightDependents = Collections.synchronizedList(new ArrayList<DependencyNode>());
		for (int i = nodeIndex + 1; i < nodes.size(); i++) {
			if (nodeIndex == nodes.get(i).getHeadIndex()) {
				rightDependents.add(nodes.get(i));
			}
		}
		return rightDependents;
	}

	protected SortedSet<DependencyNode> getSortedSetOfRightDependents(int nodeIndex) {
		SortedSet<DependencyNode> rightDependents = Collections.synchronizedSortedSet(new TreeSet<DependencyNode>());
		for (int i = nodeIndex + 1; i < nodes.size(); i++) {
			if (nodeIndex == nodes.get(i).getHeadIndex()) {
				rightDependents.add(nodes.get(i));
			}
		}
		return rightDependents;
	}

	protected List<DependencyNode> getListOfDependents(int nodeIndex) {
		List<DependencyNode> dependents = Collections.synchronizedList(new ArrayList<DependencyNode>());
		for (int i = 1; i < nodes.size(); i++) {
			if (nodeIndex == nodes.get(i).getHeadIndex()) {
				dependents.add(nodes.get(i));
			}
		}
		return dependents;
	}

	protected SortedSet<DependencyNode> getSortedSetOfDependents(int nodeIndex) {
		SortedSet<DependencyNode> dependents = Collections.synchronizedSortedSet(new TreeSet<DependencyNode>());
		for (int i = 1; i < nodes.size(); i++) {
			if (nodeIndex == nodes.get(i).getHeadIndex()) {
				dependents.add(nodes.get(i));
			}
		}
		return dependents;
	}

	protected int getRank(int nodeIndex) {
		int[] components = new int[nodes.size()];
		int[] ranks = new int[nodes.size()];
		for (int i = 0; i < components.length; i++) {
			components[i] = i;
			ranks[i] = 0;
		}
		for (int i = 1; i < nodes.size(); i++) {
			if (nodes.get(i).hasHead()) {
				int hcIndex = findComponent(nodes.get(i).getHead().getIndex(), components);
				int dcIndex = findComponent(nodes.get(i).getIndex(), components);
				if (hcIndex != dcIndex) {
					link(hcIndex, dcIndex, components, ranks);
				}
			}
		}
		return ranks[nodeIndex];
	}

	protected DependencyNode findComponent(int nodeIndex) {
		int[] components = new int[nodes.size()];
		int[] ranks = new int[nodes.size()];
		for (int i = 0; i < components.length; i++) {
			components[i] = i;
			ranks[i] = 0;
		}
		for (int i = 1; i < nodes.size(); i++) {
			if (nodes.get(i).hasHead()) {
				int hcIndex = findComponent(nodes.get(i).getHead().getIndex(), components);
				int dcIndex = findComponent(nodes.get(i).getIndex(), components);
				if (hcIndex != dcIndex) {
					link(hcIndex, dcIndex, components, ranks);
				}
			}
		}
		return nodes.get(findComponent(nodeIndex, components));
	}

	private int[] findComponents() {
		int[] components = new int[nodes.size()];
		int[] ranks = new int[nodes.size()];
		for (int i = 0; i < components.length; i++) {
			components[i] = i;
			ranks[i] = 0;
		}
		for (int i = 1; i < nodes.size(); i++) {
			if (nodes.get(i).hasHead()) {
				int hcIndex = findComponent(nodes.get(i).getHead().getIndex(), components);
				int dcIndex = findComponent(nodes.get(i).getIndex(), components);
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
	public TokenNode addTokenNode() throws MaltChainedException {
		throw new LWGraphException("Not implemented in the light-weight dependency graph package");
	}

	@Override
	public TokenNode addTokenNode(int index) throws MaltChainedException {
		throw new LWGraphException("Not implemented in the light-weight dependency graph package");
	}

	@Override
	public TokenNode getTokenNode(int index) {
//		throw new LWGraphException("Not implemented in the light-weight dependency graph package");
		return null;
	}


	@Override
	public void addComment(String comment, int at_index) {
		ArrayList<String> commentList = comments.get(at_index);
		if (commentList == null) {
			commentList = comments.put(at_index, new ArrayList<String>());
		}
		commentList.add(comment);
	}

	@Override
	public ArrayList<String> getComment(int at_index) {
		return comments.get(at_index);
	}

	@Override
	public boolean hasComments() {
		return comments.size() > 0;
	}

	@Override
	public int nTokenNode() {
		return nodes.size()-1;
	}

	@Override
	public SortedSet<Integer> getTokenIndices() {
		SortedSet<Integer> indices = Collections.synchronizedSortedSet(new TreeSet<Integer>());
		for (int i = 1; i < nodes.size(); i++) {
			indices.add(i);
		}
		return indices;
	}

	@Override
	public int getHighestTokenIndex() {
		return nodes.size()-1;
	}

	@Override
	public boolean hasTokens() {
		return nodes.size() > 1;
	}

	@Override
	public int getSentenceID() {
		return 0;
	}

	@Override
	public void setSentenceID(int sentenceID) {	}

	@Override
	public void clear() throws MaltChainedException {
		nodes.clear();
	}

	@Override
	public SymbolTableHandler getSymbolTables() {
		return symbolTables;
	}

	@Override
	public void setSymbolTables(SymbolTableHandler symbolTables) { }

	@Override
	public void addLabel(Element element, String labelFunction, String label) throws MaltChainedException {
		element.addLabel(symbolTables.addSymbolTable(labelFunction), label);
	}

	@Override
	public LabelSet checkOutNewLabelSet() throws MaltChainedException {
		throw new LWGraphException("Not implemented in light-weight dependency graph");
	}

	@Override
	public void checkInLabelSet(LabelSet labelSet) throws MaltChainedException {
		throw new LWGraphException("Not implemented in light-weight dependency graph");
	}

	@Override
	public Edge addSecondaryEdge(ComparableNode source, ComparableNode target) throws MaltChainedException {
		throw new LWGraphException("Not implemented in light-weight dependency graph");
	}

	@Override
	public void removeSecondaryEdge(ComparableNode source, ComparableNode target)
			throws MaltChainedException {
		throw new LWGraphException("Not implemented in light-weight dependency graph");
	}

	@Override
	public DependencyNode addDependencyNode() throws MaltChainedException {
		LWNode node = new LWNode(this, nodes.size());
		nodes.add(node);
		return node;
	}

	@Override
	public DependencyNode addDependencyNode(int index) throws MaltChainedException {
		if (index == 0) {
			return nodes.get(0);
		} else if (index == nodes.size()) {
			return addDependencyNode();
		}
		throw new LWGraphException("Not implemented in light-weight dependency graph");
	}

	@Override
	public DependencyNode getDependencyNode(int index) throws MaltChainedException {
		if (index < 0 || index >= nodes.size()) {
			return null;
		}
		return nodes.get(index);
	}

	@Override
	public int nDependencyNode() {
		return nodes.size();
	}

	@Override
	public int getHighestDependencyNodeIndex() {
		return nodes.size()-1;
	}

	@Override
	public Edge addDependencyEdge(int headIndex, int dependentIndex) throws MaltChainedException {
		if (headIndex < 0 && headIndex >= nodes.size()) {
			throw new LWGraphException("The head doesn't exists");
		}
		if (dependentIndex < 0 && dependentIndex >= nodes.size()) {
			throw new LWGraphException("The dependent doesn't exists");
		}
		LWNode head = nodes.get(headIndex);
		LWNode dependent = nodes.get(dependentIndex);
		Edge headEdge = new LWEdge(head, dependent);
		dependent.addIncomingEdge(headEdge);
		return headEdge;
	}

	@Override
	public Edge moveDependencyEdge(int newHeadIndex, int dependentIndex) throws MaltChainedException {
		if (newHeadIndex < 0 && newHeadIndex >= nodes.size()) {
			throw new LWGraphException("The head doesn't exists");
		}
		if (dependentIndex < 0 && dependentIndex >= nodes.size()) {
			throw new LWGraphException("The dependent doesn't exists");
		}

		LWNode head = nodes.get(newHeadIndex);
		LWNode dependent = nodes.get(dependentIndex);
		Edge oldheadEdge = dependent.getHeadEdge();
		Edge headEdge = new LWEdge(head, dependent);
		headEdge.addLabel(oldheadEdge.getLabelSet());
		dependent.addIncomingEdge(headEdge);
		return headEdge;
	}

	@Override
	public void removeDependencyEdge(int headIndex, int dependentIndex) throws MaltChainedException {
		if (headIndex < 0 && headIndex >= nodes.size()) {
			throw new LWGraphException("The head doesn't exists");
		}
		if (dependentIndex < 0 && dependentIndex >= nodes.size()) {
			throw new LWGraphException("The dependent doesn't exists");
		}
		LWNode head = nodes.get(headIndex);
		LWNode dependent = nodes.get(dependentIndex);
		Edge headEdge = new LWEdge(head, dependent);
		dependent.removeIncomingEdge(headEdge);
	}

	@Override
	public void linkAllTreesToRoot() throws MaltChainedException {
		for (int i = 0; i < nodes.size(); i++) {
			if (!nodes.get(i).hasHead()) {
				LWNode head = nodes.get(0);
				LWNode dependent = nodes.get(i);
				Edge headEdge = new LWEdge(head, dependent);
				headEdge.addLabel(getDefaultRootEdgeLabels());
				dependent.addIncomingEdge(headEdge);
			}
		}
	}

	@Override
	public int nEdges() {
		int n = 0;
		for (int i = 1; i < nodes.size(); i++) {
			if (nodes.get(i).hasHead()) {
				n++;
			}
		}
		return n;
	}

	@Override
	public SortedSet<Edge> getEdges() {
		SortedSet<Edge> edges = Collections.synchronizedSortedSet(new TreeSet<Edge>());
		for (int i = 1; i < nodes.size(); i++) {
			if (nodes.get(i).hasHead()) {
				edges.add(nodes.get(i).getHeadEdge());
			}
		}
		return edges;
	}

	@Override
	public SortedSet<Integer> getDependencyIndices() {
		SortedSet<Integer> indices = Collections.synchronizedSortedSet(new TreeSet<Integer>());
		for (int i = 0; i < nodes.size(); i++) {
			indices.add(i);
		}
		return indices;
	}

	@Override
	public DependencyNode getDependencyRoot() {
		return nodes.get(0);
	}

	@Override
	public boolean hasLabeledDependency(int index) {
		if (index < 0 || index >= nodes.size()) {
			return false;
		}
		if (!nodes.get(index).hasHead()) {
			return false;
		}
		return nodes.get(index).isHeadLabeled();
	}

	@Override
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

	@Override
	public boolean isProjective() throws MaltChainedException {
		for (int i = 1; i < nodes.size(); i++) {
			if (!nodes.get(i).isProjective()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isSingleHeaded() {
		return true;
	}

	@Override
	public boolean isTree() {
		return isConnected() && isSingleHeaded();
	}

	@Override
	public int nNonProjectiveEdges() throws MaltChainedException {
		int c = 0;
		for (int i = 1; i < nodes.size(); i++) {
			if (!nodes.get(i).isProjective()) {
				c++;
			}
		}
		return c;
	}

	@Override
	public LabelSet getDefaultRootEdgeLabels() throws MaltChainedException {
		return rootLabels.getDefaultRootLabels();
	}

	@Override
	public String getDefaultRootEdgeLabelSymbol(SymbolTable table) throws MaltChainedException {
		return rootLabels.getDefaultRootLabelSymbol(table);
	}

	@Override
	public int getDefaultRootEdgeLabelCode(SymbolTable table) throws MaltChainedException {
		return rootLabels.getDefaultRootLabelCode(table);
	}

	@Override
	public void setDefaultRootEdgeLabel(SymbolTable table, String defaultRootSymbol) throws MaltChainedException {
		rootLabels.setDefaultRootLabel(table, defaultRootSymbol);
	}

	@Override
	public void setDefaultRootEdgeLabels(String rootLabelOption, SortedMap<String, SymbolTable> edgeSymbolTables) throws MaltChainedException {
		rootLabels.setRootLabels(rootLabelOption, edgeSymbolTables);
	}

	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (LWNode node : nodes) {
			sb.append(node.toString().trim());
			sb.append('\n');
		}
		sb.append('\n');
		return sb.toString();
	}
}
