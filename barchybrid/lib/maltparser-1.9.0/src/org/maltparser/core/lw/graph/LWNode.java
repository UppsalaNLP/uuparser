package org.maltparser.core.lw.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.maltparser.concurrent.graph.dataformat.ColumnDescription;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.helper.HashMap;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.LabelSet;
import org.maltparser.core.syntaxgraph.LabeledStructure;
import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.core.syntaxgraph.node.ComparableNode;
import org.maltparser.core.syntaxgraph.node.DependencyNode;
import org.maltparser.core.syntaxgraph.node.Node;

/**
* A lightweight version of org.maltparser.core.syntaxgraph.node.{Token,Root}
* 
* @author Johan Hall
*/
public final class LWNode implements DependencyNode, Node {
	private final LWDependencyGraph graph;
	private int index;
//	private final SortedMap<Integer, String> labels;
	private final Map<Integer, String> labels;
	private Edge headEdge;
	
	protected LWNode(LWNode node) throws LWGraphException {
		this(node.graph, node);
	}
	
	protected LWNode(LWDependencyGraph _graph, LWNode node) throws LWGraphException {
		if (_graph == null) {
			throw new LWGraphException("The graph node must belong to a dependency graph.");
		}
		this.graph = _graph;
		this.index = node.index;
//		this.labels = new TreeMap<Integer, String>(node.labels);
		this.labels = new HashMap<Integer, String>(node.labels);
		this.headEdge = node.headEdge;
	}
	
	protected LWNode(LWDependencyGraph _graph, int _index) throws LWGraphException {
		if (_graph == null) {
			throw new LWGraphException("The graph node must belong to a dependency graph.");
		}
		if (_index < 0) {
			throw new LWGraphException("Not allowed to have negative node index");
		}
		this.graph = _graph;
		this.index = _index;
//		this.labels = new TreeMap<Integer, String>();
		this.labels = new HashMap<Integer, String>();
		this.headEdge = null;
	}
	
//	public void setHeadIndex(int _headIndex) throws LWGraphException {
//		if (this.index == 0 && _headIndex != -1) {
//			throw new LWGraphException("Not allowed to add head to a root node.");
//		}
//		if (this.index == _headIndex) {
//			throw new LWGraphException("Not allowed to add head to itself");
//		}
//		this.headIndex = _headIndex;
//	}
//	
//	public void removeHeadIndex() throws LWGraphException {
//		this.headIndex = -1;
//		for (Integer i : labels.keySet()) {
//			if (graph.getDataFormat().getColumnDescription(i).getCategory() == ColumnDescription.DEPENDENCY_EDGE_LABEL) {
//				this.labels.remove(i);
//			}
//		}
//	}
	
	protected DependencyStructure getGraph() {
		return graph;
	}
	
	public int getIndex() {
		return this.index;
	}
	
	@Override
	public void setIndex(int index) throws MaltChainedException {
		this.index = index;
	}

	public String getLabel(int columnPosition) {
		if (labels.containsKey(columnPosition)) {
			return labels.get(columnPosition);
		} else if (graph.getDataFormat().getColumnDescription(columnPosition).getCategory() == ColumnDescription.IGNORE) {
			return graph.getDataFormat().getColumnDescription(columnPosition).getDefaultOutput();
		}
		return "";
	}
	
	public String getLabel(String columnName) {
		ColumnDescription column = graph.getDataFormat().getColumnDescription(columnName);
		if (column != null) {
			return getLabel(column.getPosition());
		}
		return "";
	}
	
	public String getLabel(ColumnDescription column) {
		return getLabel(column.getPosition());
	}
	
	public boolean hasLabel(int columnPosition) {
		return labels.containsKey(columnPosition);
	}
	
	public boolean hasLabel(String columnName) {
		ColumnDescription column = graph.getDataFormat().getColumnDescription(columnName);
		if (column != null) {
			return hasLabel(column.getPosition());
		}
		return false;
	}
	
	public boolean hasLabel(ColumnDescription column) {
		return labels.containsKey(column.getPosition());
	}
	
	public boolean isLabeled() {
		for (Integer key : labels.keySet()) {
			if (graph.getDataFormat().getColumnDescription(key).getCategory() == ColumnDescription.INPUT) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isHeadLabeled() {
		if (headEdge == null) {
			return false;
		}
		return headEdge.isLabeled();
	}
	
	public int getHeadIndex() {
		if (headEdge == null) {
			return -1;
		}
		return headEdge.getSource().getIndex();
	}
	
	public SortedMap<ColumnDescription, String> getLabels() {
		SortedMap<ColumnDescription, String> nodeLabels = Collections.synchronizedSortedMap(new TreeMap<ColumnDescription, String>());
		for (Integer key : labels.keySet()) {
			nodeLabels.put(graph.getDataFormat().getColumnDescription(key), labels.get(key));
		}
		return nodeLabels;
	}
	
//	public SortedMap<ColumnDescription, String> getEdgeLabels() {
//		SortedMap<ColumnDescription, String> edgeLabels = Collections.synchronizedSortedMap(new TreeMap<ColumnDescription, String>());
//		for (Integer key : labels.keySet()) {
//			if (graph.getDataFormat().getColumnDescription(key).getCategory() == ColumnDescription.DEPENDENCY_EDGE_LABEL) {
//				edgeLabels.put(graph.getDataFormat().getColumnDescription(key), labels.get(key));
//			}
//		}
//		return edgeLabels;
//	}
	
	public DependencyNode getPredecessor() {
		return index > 1 ? graph.getNode(index - 1) : null;
	}
	
	public DependencyNode getSuccessor() {
		return graph.getNode(index + 1);
	}
	
	public boolean isRoot() {
		return index == 0;
	}
	
	public boolean hasAtMostOneHead() {
		return true;
	}
	
	public boolean hasHead() {
		return headEdge != null;
	}
	
	public boolean hasDependent() {
		return graph.hasDependent(index);
	}
	
	public boolean hasLeftDependent() {
		return graph.hasLeftDependent(index);
	}
	
	public boolean hasRightDependent() {
		return graph.hasRightDependent(index);
	}
	
	public SortedSet<DependencyNode> getHeads() {
		SortedSet<DependencyNode> heads = Collections.synchronizedSortedSet(new TreeSet<DependencyNode>());
		DependencyNode head = getHead();
		if (head != null) {
			heads.add(head);
		}
		return heads; 
	}
	
	public DependencyNode getHead() {
		if (headEdge == null) {
			return null;
		}
		return graph.getNode(getHeadIndex());
	}

	public DependencyNode getLeftDependent(int leftDependentIndex) {	
		List<DependencyNode> leftDependents = graph.getListOfLeftDependents(index);
		if (leftDependentIndex >= 0 && leftDependentIndex < leftDependents.size()) {
			return leftDependents.get(leftDependentIndex);
		}
		return null;
	}

	public int getLeftDependentCount() {
		return graph.getListOfLeftDependents(index).size();
	}

	public SortedSet<DependencyNode> getLeftDependents() {
		return graph.getSortedSetOfLeftDependents(index);
	}

	public List<DependencyNode> getListOfLeftDependents() {
		return graph.getListOfLeftDependents(index);
	}
	
	public DependencyNode getLeftSibling() {
		if (headEdge == null) {
			return null;
		}
		
		int nodeDepedentPosition = 0;
		List<DependencyNode> headDependents = getHead().getListOfDependents();
		for (int i = 0; i < headDependents.size(); i++) {
			if (headDependents.get(i).getIndex() == index) {
				nodeDepedentPosition = i;
				break;
			}
		}
		
		return (nodeDepedentPosition > 0) ? headDependents.get(nodeDepedentPosition - 1) : null;
	}

	public DependencyNode getSameSideLeftSibling() {
		if (headEdge == null) {
			return null;
		}
		
		List<DependencyNode> headDependents;
		if (index < getHeadIndex()) {
			headDependents = getHead().getListOfLeftDependents();
		} else { //(index > headIndex)
			headDependents = getHead().getListOfRightDependents();
		}
		int nodeDepedentPosition = 0;
		for (int i = 0; i < headDependents.size(); i++) {
			if (headDependents.get(i).getIndex() == index) {
				nodeDepedentPosition = i;
				break;
			}
		}
		return (nodeDepedentPosition > 0) ? headDependents.get(nodeDepedentPosition - 1) : null;
	}

	public DependencyNode getClosestLeftDependent() {
		List<DependencyNode> leftDependents = graph.getListOfLeftDependents(index);
		return (leftDependents.size() > 0) ? leftDependents.get(leftDependents.size() - 1) : null;
	}
	
	public DependencyNode getLeftmostDependent() {
		List<DependencyNode> leftDependents = graph.getListOfLeftDependents(index);
		return (leftDependents.size() > 0) ? leftDependents.get(0) : null;
	}
	
	public DependencyNode getRightDependent(int rightDependentIndex) {	
		List<DependencyNode> rightDependents = graph.getListOfRightDependents(index);
		if (rightDependentIndex >= 0 && rightDependentIndex < rightDependents.size()) {
			return rightDependents.get(rightDependents.size() - 1 - rightDependentIndex);
		}
		return null;
	}
	
	public int getRightDependentCount() {
		return graph.getListOfRightDependents(index).size();
	}

	public SortedSet<DependencyNode> getRightDependents() {
		return graph.getSortedSetOfRightDependents(index);
	}

	public List<DependencyNode> getListOfRightDependents() {
		return graph.getListOfRightDependents(index);
	}
	
	public DependencyNode getRightSibling() {
		if (headEdge == null) {
			return null;
		}
		
		List<DependencyNode> headDependents = getHead().getListOfDependents();
		int nodeDepedentPosition = headDependents.size() - 1;
		for (int i = headDependents.size() - 1; i >= 0 ; i--) {
			if (headDependents.get(i).getIndex() == index) {
				nodeDepedentPosition = i;
				break;
			}
		}
		
		return (nodeDepedentPosition < headDependents.size() - 1) ? headDependents.get(nodeDepedentPosition + 1) : null;
	}

	public DependencyNode getSameSideRightSibling() {
		if (headEdge == null) {
			return null;
		}
		
		List<DependencyNode> headDependents;
		if (index < getHeadIndex()) {
			headDependents = getHead().getListOfLeftDependents();
		} else {
			headDependents = getHead().getListOfRightDependents();
		}
		int nodeDepedentPosition = headDependents.size() - 1;
		for (int i = headDependents.size() - 1; i >= 0 ; i--) {
			if (headDependents.get(i).getIndex() == index) {
				nodeDepedentPosition = i;
				break;
			}
		}
		
		return (nodeDepedentPosition < headDependents.size() - 1) ? headDependents.get(nodeDepedentPosition + 1) : null;	
	}

	public DependencyNode getClosestRightDependent() {
		List<DependencyNode> rightDependents = graph.getListOfRightDependents(index);
		return (rightDependents.size() > 0) ? rightDependents.get(0) : null;
	}
	
	public DependencyNode getRightmostDependent(){
		List<DependencyNode> rightDependents = graph.getListOfRightDependents(index);
		return (rightDependents.size() > 0) ? rightDependents.get(rightDependents.size() - 1) : null;
	}
	
	public SortedSet<DependencyNode> getDependents() {
		return graph.getSortedSetOfDependents(index);
	}
	
	public List<DependencyNode> getListOfDependents() {
		return graph.getListOfDependents(index);
	}
	
	public int getInDegree() {
		if (hasHead()) {
			return 1;
		}
		return 0;
	}
	
	public int getOutDegree() {
		return graph.getListOfDependents(index).size();
	}
	
	public DependencyNode getAncestor() throws MaltChainedException {
		if (!this.hasHead()) {
			return this;
		}
		
		DependencyNode tmp = this;
		while (tmp.hasHead()) {
			tmp = tmp.getHead();
		}
		return tmp;
	}
	
	public DependencyNode getProperAncestor() throws MaltChainedException {
		if (!this.hasHead()) {
			return null;
		}
		
		DependencyNode tmp = this;
		while (tmp.hasHead() && !tmp.isRoot()) {
			tmp = tmp.getHead();
		}
		return tmp;
	}
	
	public boolean hasAncestorInside(int left, int right) throws MaltChainedException {
		if (index == 0) {
			return false;
		}
		DependencyNode tmp = this;
		if (tmp.getHead() != null) {
			tmp = tmp.getHead();
			if (tmp.getIndex() >= left && tmp.getIndex() <= right) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isProjective() throws MaltChainedException {
		int headIndex = getHeadIndex();
		if (headIndex > 0) {
			final DependencyNode head = getHead();
			if (headIndex < index) {
				DependencyNode terminals = head;
				DependencyNode tmp = null;
				while (true) {
					if (terminals == null || terminals.getSuccessor() == null) {
						return false;
					}
					if (terminals.getSuccessor() == this) {
						break;
					}
					tmp = terminals = terminals.getSuccessor();
					while (tmp != this && tmp != head) {
						if (!tmp.hasHead()) {
							return false;
						}
						tmp = tmp.getHead();
					}
				}
			} else {
				DependencyNode terminals = this;
				DependencyNode tmp = null;
				while (true) {
					if (terminals == null || terminals.getSuccessor() == null) {
						return false;
					}
					if (terminals.getSuccessor() == head) {
						break;
					}
					tmp = terminals = terminals.getSuccessor();
					while (tmp != this && tmp != head) {
						if (!tmp.hasHead()) {
							return false;
						}
						tmp = tmp.getHead();
					}
				}
			}
		}
		return true;
	}
	
	public int getDependencyNodeDepth() throws MaltChainedException {
		DependencyNode tmp = this;
		int depth = 0;
		while (tmp.hasHead()) {
			depth++;
			tmp = tmp.getHead();
		}
		return depth;
	}

	@Override
	public int getCompareToIndex() {
		return index;
	}

	@Override
	public ComparableNode getLeftmostProperDescendant() throws MaltChainedException {
		ComparableNode candidate = null;
		List<DependencyNode> dependents = graph.getListOfDependents(index);
		for (int i = 0; i < dependents.size(); i++) {
			final DependencyNode dep = dependents.get(i);
			if (candidate == null || dep.getIndex() < candidate.getIndex()) {
				candidate = dep;
			}
			final ComparableNode tmp = dep.getLeftmostProperDescendant();
			if (tmp == null) {
				continue;
			}
			if (candidate == null || tmp.getIndex() < candidate.getIndex()) {
				candidate = tmp;
			}
			if (candidate.getIndex() == 1) {
				return candidate;
			}
		}
		return candidate;
	}

	@Override
	public ComparableNode getRightmostProperDescendant() throws MaltChainedException {
		ComparableNode candidate = null;
		List<DependencyNode> dependents = graph.getListOfDependents(index);
		for (int i = 0; i < dependents.size(); i++) {
			final DependencyNode dep = dependents.get(i);
			if (candidate == null || dep.getIndex() > candidate.getIndex()) {
				candidate = dep;
			}
			final ComparableNode tmp = dep.getRightmostProperDescendant();
			if (tmp == null) {
				continue;
			}
			if (candidate == null || tmp.getIndex() > candidate.getIndex()) {
				candidate = tmp;
			}
		}
		return candidate;
	}
	@Override
	public int getLeftmostProperDescendantIndex() throws MaltChainedException {
		ComparableNode node = getLeftmostProperDescendant();
		return (node != null)?node.getIndex():-1;
	}
	@Override
	public int getRightmostProperDescendantIndex() throws MaltChainedException {
		ComparableNode node = getRightmostProperDescendant();
		return (node != null)?node.getIndex():-1;
	}

	@Override
	public ComparableNode getLeftmostDescendant() throws MaltChainedException {
		ComparableNode candidate = this;
		List<DependencyNode> dependents = graph.getListOfDependents(index);
		for (int i = 0; i < dependents.size(); i++) {
			final DependencyNode dep = dependents.get(i);
			if (dep.getIndex() < candidate.getIndex()) {
				candidate = dep;
			}
			final ComparableNode tmp = dep.getLeftmostDescendant();
			if (tmp == null) {
				continue;
			}
			if (tmp.getIndex() < candidate.getIndex()) {
				candidate = tmp;
			}
			if (candidate.getIndex() == 1) {
				return candidate;
			}
		}
		return candidate;
	}

	@Override
	public ComparableNode getRightmostDescendant() throws MaltChainedException {
		ComparableNode candidate = this;
		List<DependencyNode> dependents = graph.getListOfDependents(index);
		for (int i = 0; i < dependents.size(); i++) {
			final DependencyNode dep = dependents.get(i);
			if (dep.getIndex() > candidate.getIndex() ) {
				candidate = dep;
			}
			final ComparableNode tmp = dep.getRightmostDescendant();
			if (tmp == null) {
				continue;
			}
			if (tmp.getIndex() > candidate.getIndex() ) {
				candidate = tmp;
			}
		}
		return candidate;
	}

	@Override
	public int getLeftmostDescendantIndex() throws MaltChainedException {
		ComparableNode node = getLeftmostDescendant();
		return (node != null)?node.getIndex():this.getIndex();
	}
	@Override
	public int getRightmostDescendantIndex() throws MaltChainedException {
		ComparableNode node = getRightmostDescendant();
		return (node != null)?node.getIndex():this.getIndex();
	}

	@Override
	public SortedSet<Edge> getIncomingSecondaryEdges() throws MaltChainedException {
		throw new LWGraphException("Not implemented in the light-weight dependency graph package");
	}

	@Override
	public SortedSet<Edge> getOutgoingSecondaryEdges() throws MaltChainedException {
		throw new LWGraphException("Not implemented in the light-weight dependency graph package");
	}
	
	@Override
	public Set<Edge> getHeadEdges()  {
		SortedSet<Edge> edges = Collections.synchronizedSortedSet(new TreeSet<Edge>());
		if (hasHead()) {
			edges.add(headEdge);
		}
		return edges;
	}

	@Override
	public Edge getHeadEdge() {
		if (!hasHead()) {
			return null;
		}
		return headEdge;
	}
	
	@Override
	public void addHeadEdgeLabel(SymbolTable table, String symbol)
			throws MaltChainedException {
		if (headEdge != null) {
			headEdge.addLabel(table, symbol);
		}
	}

	@Override
	public void addHeadEdgeLabel(SymbolTable table, int code) throws MaltChainedException {
		if (headEdge != null) {
			headEdge.addLabel(table, code);
		}
	}

	@Override
	public void addHeadEdgeLabel(LabelSet labelSet) throws MaltChainedException {
		if (headEdge != null) {
			headEdge.addLabel(labelSet);
		}
	}

	@Override
	public boolean hasHeadEdgeLabel(SymbolTable table)
			throws MaltChainedException {
		if (headEdge != null) {
			return headEdge.hasLabel(table);
		}
		return false;
	}

	@Override
	public String getHeadEdgeLabelSymbol(SymbolTable table) throws MaltChainedException {
		if (headEdge != null) {
			return headEdge.getLabelSymbol(table);
		}
		return null;
	}

	@Override
	public int getHeadEdgeLabelCode(SymbolTable table)
			throws MaltChainedException {
		if (headEdge != null) {
			return headEdge.getLabelCode(table);
		}
		return 0;
	}

	@Override
	public Set<SymbolTable> getHeadEdgeLabelTypes() throws MaltChainedException {
		if (headEdge != null) {
			return headEdge.getLabelTypes();
		}
		return new HashSet<SymbolTable>();
	}

	@Override
	public LabelSet getHeadEdgeLabelSet() throws MaltChainedException {
		if (headEdge != null) {
			return headEdge.getLabelSet();
		}
		return new LabelSet();
	}

	
	@Override
	public void addIncomingEdge(Edge in) throws MaltChainedException {
		headEdge = in;
	}

	@Override
	public void addOutgoingEdge(Edge out) throws MaltChainedException {
		throw new LWGraphException("Not implemented in the light-weight dependency graph package");
		
	}

	@Override
	public void removeIncomingEdge(Edge in) throws MaltChainedException {
		if (headEdge.equals(in)) {
			headEdge = null;
		}
	}

	@Override
	public void removeOutgoingEdge(Edge out) throws MaltChainedException {
		throw new LWGraphException("Not implemented in the light-weight dependency graph package");
	}

	@Override
	public Iterator<Edge> getIncomingEdgeIterator() {
		return getHeadEdges().iterator();
	}

	@Override
	public Iterator<Edge> getOutgoingEdgeIterator() {
		List<DependencyNode> dependents = getListOfDependents();
		List<Edge> outEdges = new ArrayList<Edge>(dependents.size());
		for (int i = 0; i < dependents.size(); i++) {
			try {
				outEdges.add(dependents.get(i).getHeadEdge());
			} catch (MaltChainedException e) {
				e.printStackTrace();
			}
		}
		return outEdges.iterator();
	}

	@Override
	public void setRank(int r) {}

	@Override
	public DependencyNode getComponent() {
		return null;
	}

	@Override
	public void setComponent(DependencyNode x) {}

	public DependencyNode findComponent() {
		return graph.findComponent(index);
	}
	
	public int getRank() {
		return graph.getRank(index);
	}
	
	public boolean isHeadEdgeLabeled() {
		if (headEdge != null) {
			return headEdge.isLabeled();
		}
		return false;
	}
	
	public int nHeadEdgeLabels() {
		if (headEdge != null) {
			return headEdge.nLabels();
		}
		return 0;
	}
	
	public void addColumnLabels(String[] columnLabels) throws MaltChainedException {
		this.addColumnLabels(columnLabels, true);
	}
	
	public void addColumnLabels(String[] columnLabels, boolean addEdges) throws MaltChainedException {
		if (addEdges == true) {
			SortedMap<ColumnDescription,String> edgeLabels = new TreeMap<ColumnDescription,String>();
			int tmpHeadIndex = -1;
			if (columnLabels != null) {
				for (int i = 0; i < columnLabels.length; i++) {
					ColumnDescription column = graph.getDataFormat().getColumnDescription(i);
					if (column.getCategory() == ColumnDescription.HEAD) {
						tmpHeadIndex = Integer.parseInt(columnLabels[i]);
					} else if (column.getCategory() == ColumnDescription.INPUT) {
						addLabel(graph.getSymbolTables().addSymbolTable(column.getName()), columnLabels[i]);
					} else if (column.getCategory() == ColumnDescription.DEPENDENCY_EDGE_LABEL) {
						edgeLabels.put(column, columnLabels[i]);
					}
				}
			}
			if (tmpHeadIndex == -1) {
				this.headEdge = null;
			} else {
				if (tmpHeadIndex < -1) {
					throw new LWGraphException("Not allowed to have head index less than -1.");
				}
				if (this.index == 0 && tmpHeadIndex != -1) {
					throw new LWGraphException("Not allowed to add head to a root node.");
				}
				if (this.index == tmpHeadIndex) {
					throw new LWGraphException("Not allowed to add head to itself");
				}
				this.headEdge = new LWEdge(this.graph.getNode(tmpHeadIndex), this, edgeLabels);
			}
		} else {
			if (columnLabels != null) {
				for (int i = 0; i < columnLabels.length; i++) {
					ColumnDescription column = graph.getDataFormat().getColumnDescription(i);
					if (column.getCategory() == ColumnDescription.INPUT) {
						addLabel(graph.getSymbolTables().addSymbolTable(column.getName()), columnLabels[i]);
					} 
				}
			}
			this.headEdge = null;
		}
	}
	
	/**
	 * Adds a label (a string value) to the symbol table and to the graph element. 
	 * 
	 * @param table the symbol table
	 * @param symbol a label symbol
	 * @throws MaltChainedException
	 */
	public void addLabel(SymbolTable table, String symbol) throws MaltChainedException {
		ColumnDescription column = graph.getDataFormat().getColumnDescription(table.getName());
		table.addSymbol(symbol);
		labels.put(column.getPosition(), symbol);
	}
	
	/**
	 * Adds a label (an integer value) to the symbol table and to the graph element.
	 * 
	 * @param table the symbol table
	 * @param code a label code
	 * @throws MaltChainedException
	 */
	public void addLabel(SymbolTable table, int code) throws MaltChainedException {
		addLabel(table, table.getSymbolCodeToString(code));
	}
	
	/**
	 * Adds the labels of the label set to the label set of the graph element.
	 * 
	 * @param labels a label set.
	 * @throws MaltChainedException
	 */
	public void addLabel(LabelSet labels) throws MaltChainedException {
		for (SymbolTable table : labels.keySet()) {
			addLabel(table, labels.get(table));
		}
	}
	
	/**
	 * Returns <i>true</i> if the graph element has a label for the symbol table, otherwise <i>false</i>.
	 * 
	 * @param table the symbol table
	 * @return <i>true</i> if the graph element has a label for the symbol table, otherwise <i>false</i>.
	 * @throws MaltChainedException
	 */
	public boolean hasLabel(SymbolTable table) throws MaltChainedException {
		ColumnDescription column = graph.getDataFormat().getColumnDescription(table.getName());
		return labels.containsKey(column.getPosition());
	}
	
	/**
	 * Returns the label symbol(a string representation) of the symbol table if it exists, otherwise 
	 * an exception is thrown.
	 * 
	 * @param table the symbol table
	 * @return the label (a string representation) of the symbol table if it exists.
	 * @throws MaltChainedException
	 */
	public String getLabelSymbol(SymbolTable table) throws MaltChainedException {
		ColumnDescription column = graph.getDataFormat().getColumnDescription(table.getName());
		return labels.get(column.getPosition());
	}
	
	/**
	 * Returns the label code (an integer representation) of the symbol table if it exists, otherwise 
	 * an exception is thrown.
	 * 
	 * @param table the symbol table
	 * @return the label code (an integer representation) of the symbol table if it exists
	 * @throws MaltChainedException
	 */
	public int getLabelCode(SymbolTable table) throws MaltChainedException {
		ColumnDescription column = graph.getDataFormat().getColumnDescription(table.getName());
		return table.getSymbolStringToCode(labels.get(column.getPosition()));
	}
	
	/**
	 * Returns the number of labels of the graph element.
	 * 
	 * @return the number of labels of the graph element.
	 */
	public int nLabels() {
		return labels.size();
	}
	
	/**
	 * Returns a set of symbol tables (labeling functions or label types) that labels the graph element.
	 * 
	 * @return a set of symbol tables (labeling functions or label types)
	 */
	public Set<SymbolTable> getLabelTypes() {
		Set<SymbolTable> labelTypes = new HashSet<SymbolTable>();
		SymbolTableHandler symbolTableHandler = getBelongsToGraph().getSymbolTables();
		SortedSet<ColumnDescription> selectedColumns = graph.getDataFormat().getSelectedColumnDescriptions(labels.keySet());
		for (ColumnDescription column : selectedColumns) {
			try {
				labelTypes.add(symbolTableHandler.getSymbolTable(column.getName()));
			} catch (MaltChainedException e) {
				e.printStackTrace();
			}
		}
		return labelTypes;
	}
	
	/**
	 * Returns the label set.
	 * 
	 * @return the label set.
	 */
	public LabelSet getLabelSet() {
		SymbolTableHandler symbolTableHandler = getBelongsToGraph().getSymbolTables();
		LabelSet labelSet = new LabelSet();
		SortedSet<ColumnDescription> selectedColumns = graph.getDataFormat().getSelectedColumnDescriptions(labels.keySet());
		for (ColumnDescription column : selectedColumns) {
			try {
				SymbolTable table = symbolTableHandler.getSymbolTable(column.getName());
				int code = table.getSymbolStringToCode(labels.get(column.getPosition()));
				labelSet.put(table, code);
			} catch (MaltChainedException e) {
				e.printStackTrace();
			}
		}
		return labelSet;
	}
	
	public void removeLabel(SymbolTable table) throws MaltChainedException {
		ColumnDescription column = graph.getDataFormat().getColumnDescription(table.getName());
		labels.remove(column.getPosition());
	}
	
	public void removeLabels() throws MaltChainedException {
		labels.clear();
	}
	
	/**
	 * Returns the graph (structure) in which the graph element belongs to. 
	 * 
	 * @return the graph (structure) in which the graph element belongs to. 
	 */
	public LabeledStructure getBelongsToGraph()  {
		return graph;
	}
	
	public void setBelongsToGraph(LabeledStructure belongsToGraph)  {}
	

	/**
	 * Resets the graph element.
	 * 
	 * @throws MaltChainedException
	 */
	public void clear() throws MaltChainedException {
		labels.clear();
	}
	
	@Override
	public int compareTo(ComparableNode that) {
		final int BEFORE = -1;
	    final int EQUAL = 0;
	    final int AFTER = 1;
	    if (this == that) return EQUAL;
	    if (this.index < that.getIndex()) return BEFORE;
	    if (this.index > that.getIndex()) return AFTER;
	    return EQUAL;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((headEdge == null) ? 0 : headEdge.hashCode());
		result = prime * result + index;
		result = prime * result + ((labels == null) ? 0 : labels.hashCode());
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
		LWNode other = (LWNode) obj;
		if (headEdge == null) {
			if (other.headEdge != null)
				return false;
		} else if (!headEdge.equals(other.headEdge))
			return false;
		if (index != other.index)
			return false;
		if (labels == null) {
			if (other.labels != null)
				return false;
		} else if (!labels.equals(other.labels))
			return false;
		return true;
	}

	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < graph.getDataFormat().numberOfColumns(); i++) {
			ColumnDescription column = graph.getDataFormat().getColumnDescription(i);
			if (!column.isInternal()) {
				if (column.getCategory() == ColumnDescription.HEAD) {
					sb.append(getHeadIndex());
				} else if (column.getCategory() == ColumnDescription.INPUT) {
					sb.append(labels.get(column.getPosition()));
				} else if (column.getCategory() == ColumnDescription.DEPENDENCY_EDGE_LABEL) {
					if (headEdge != null) {
						sb.append(((LWEdge)headEdge).getLabel(column));
					} else {
						sb.append(column.getDefaultOutput());
					}
				} else if (column.getCategory() == ColumnDescription.IGNORE) {
					sb.append(column.getDefaultOutput());
				}
				sb.append('\t');
			}
		}
		sb.setLength((sb.length() > 0)?sb.length()-1:0);
		return sb.toString();
	}
}
