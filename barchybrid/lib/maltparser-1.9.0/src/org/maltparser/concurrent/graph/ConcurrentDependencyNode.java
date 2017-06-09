package org.maltparser.concurrent.graph;

import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.maltparser.concurrent.graph.dataformat.ColumnDescription;
import org.maltparser.concurrent.graph.dataformat.DataFormat;

/**
* Immutable and tread-safe dependency node implementation.
* 
* @author Johan Hall
*/
public final class ConcurrentDependencyNode implements Comparable<ConcurrentDependencyNode> {
	private final ConcurrentDependencyGraph graph;
	private final int index;
	private final SortedMap<Integer, String> labels;
	private final int headIndex;
	
	protected ConcurrentDependencyNode(ConcurrentDependencyNode node) throws ConcurrentGraphException {
		this(node.graph, node);
	}
	
	protected ConcurrentDependencyNode(ConcurrentDependencyGraph _graph, ConcurrentDependencyNode node) throws ConcurrentGraphException {
		if (_graph == null) {
			throw new ConcurrentGraphException("The graph node must belong to a dependency graph.");
		}
		this.graph = _graph;
		this.index = node.index;
		this.labels = new TreeMap<Integer, String>(node.labels);
		this.headIndex = node.headIndex;
	}
	
	protected ConcurrentDependencyNode(ConcurrentDependencyGraph _graph, int _index, SortedMap<Integer, String> _labels, int _headIndex) throws ConcurrentGraphException {
		if (_graph == null) {
			throw new ConcurrentGraphException("The graph node must belong to a dependency graph.");
		}
		if (_index < 0) {
			throw new ConcurrentGraphException("Not allowed to have negative node index");
		}
		if (_headIndex < -1) {
			throw new ConcurrentGraphException("Not allowed to have head index less than -1.");
		}
		if (_index == 0 && _headIndex != -1) {
			throw new ConcurrentGraphException("Not allowed to add head to a root node.");
		}
		if (_index == _headIndex) {
			throw new ConcurrentGraphException("Not allowed to add head to itself");
		}
		this.graph = _graph;
		this.index = _index;
		this.labels = new TreeMap<Integer, String>(_labels);
		this.headIndex = _headIndex;
	}
	
	protected ConcurrentDependencyNode(ConcurrentDependencyGraph _graph, int _index, String[] _labels) throws ConcurrentGraphException {
		if (_graph == null) {
			throw new ConcurrentGraphException("The graph node must belong to a dependency graph.");
		}
		if (_index < 0) {
			throw new ConcurrentGraphException("Not allowed to have negative node index");
		}
		this.graph = _graph;
		this.index = _index;
		this.labels = new TreeMap<Integer, String>();
		
		int tmpHeadIndex = -1;
		if (_labels != null) {
			for (int i = 0; i < _labels.length; i++) {
				int columnCategory = graph.getDataFormat().getColumnDescription(i).getCategory();
				if (columnCategory == ColumnDescription.HEAD) {
					tmpHeadIndex = Integer.parseInt(_labels[i]);
				} else if (columnCategory == ColumnDescription.INPUT || columnCategory == ColumnDescription.DEPENDENCY_EDGE_LABEL) {
					this.labels.put(i, _labels[i]);
				}
			}
		}
		this.headIndex = tmpHeadIndex;
		if (this.headIndex < -1) {
			throw new ConcurrentGraphException("Not allowed to have head index less than -1.");
		}
		if (this.index == 0 && this.headIndex != -1) {
			throw new ConcurrentGraphException("Not allowed to add head to a root node.");
		}
		if (this.index == this.headIndex) {
			throw new ConcurrentGraphException("Not allowed to add head to itself");
		}
	}
	
	/**
	 * Returns the index of the node.
	 * 
	 * @return the index of the node.
	 */
	public int getIndex() {
		return this.index;
	}
	
	/**
	 * Returns a label
	 * 
	 * @param columnPosition the column position of the column that describes the label
	 * @return a label. An empty string is returned if the label is not found.
	 */
	public String getLabel(int columnPosition) {
		if (labels.containsKey(columnPosition)) {
			return labels.get(columnPosition);
		} else if (graph.getDataFormat().getColumnDescription(columnPosition).getCategory() == ColumnDescription.IGNORE) {
			return graph.getDataFormat().getColumnDescription(columnPosition).getDefaultOutput();
		}
		return "";
	}
	
	/**
	 * Returns a label
	 * 
	 * @param columnName the name of the column that describes the label.
	 * @return a label. An empty string is returned if the label is not found.
	 */
	public String getLabel(String columnName) {
		ColumnDescription column = graph.getDataFormat().getColumnDescription(columnName);
		if (column != null) {
			return getLabel(column.getPosition());
		}
		return "";
	}
	
	/**
	 * Returns a label
	 * 
	 * @param column a column description that describes the label
	 * @return a label described by the column description. An empty string is returned if the label is not found. 
	 */
	public String getLabel(ColumnDescription column) {
		return getLabel(column.getPosition());
	}
	
	/**
	 * Returns <i>true</i> if the label exists, otherwise <i>false<i/>
	 * 
	 * @param columnPosition the column position of the column that describes the label
	 * @return <i>true</i> if the label exists, otherwise <i>false<i/>
	 */
	public boolean hasLabel(int columnPosition) {
		return labels.containsKey(columnPosition);
	}
	
	/**
	 * Returns <i>true</i> if the label exists, otherwise <i>false<i/>
	 * 
	 * @param columnName the name of the column that describes the label.
	 * @return <i>true</i> if the label exists, otherwise <i>false<i/>
	 */
	public boolean hasLabel(String columnName) {
		ColumnDescription column = graph.getDataFormat().getColumnDescription(columnName);
		if (column != null) {
			return hasLabel(column.getPosition());
		}
		return false;
	}
	
	/**
	 * Returns <i>true</i> if the label exists, otherwise <i>false<i/>
	 * 
	 * @param column a column description that describes the label
	 * @return <i>true</i> if the label exists, otherwise <i>false<i/>
	 */
	public boolean hasLabel(ColumnDescription column) {
		return labels.containsKey(column.getPosition());
	}
	
	/**
	 * Returns <i>true</i> if the node has one or more labels, otherwise <i>false</i>.
	 * 
	 * @return <i>true</i> if the node has one or more labels, otherwise <i>false</i>.
	 */
	public boolean isLabeled() {
		for (Integer key : labels.keySet()) {
			if (graph.getDataFormat().getColumnDescription(key).getCategory() == ColumnDescription.INPUT) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns <i>true</i> if the head edge has one or more labels, otherwise <i>false</i>.
	 * 
	 * @return <i>true</i> if the head edge has one or more labels, otherwise <i>false</i>.
	 */
	public boolean isHeadLabeled() {
		for (Integer key : labels.keySet()) {
			if (graph.getDataFormat().getColumnDescription(key).getCategory() == ColumnDescription.DEPENDENCY_EDGE_LABEL) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns the index of the head node
	 * 
	 * @return the index of the head node
	 */
	public int getHeadIndex() {
		return headIndex;
	}
	
	/**
	 * Returns a sorted map that maps column descriptions to node labels
	 * 
	 * @return a sorted map that maps column descriptions to node labels
	 */
	public SortedMap<ColumnDescription, String> getNodeLabels() {
		SortedMap<ColumnDescription, String> nodeLabels = Collections.synchronizedSortedMap(new TreeMap<ColumnDescription, String>());
		for (Integer key : labels.keySet()) {
			if (graph.getDataFormat().getColumnDescription(key).getCategory() == ColumnDescription.INPUT) {
				nodeLabels.put(graph.getDataFormat().getColumnDescription(key), labels.get(key));
			}
		}
		return nodeLabels;
	}
	
	/**
	 * Returns a sorted map that maps column descriptions to head edge labels
	 * 
	 * @return a sorted map that maps column descriptions to head edge labels
	 */
	public SortedMap<ColumnDescription, String> getEdgeLabels() {
		SortedMap<ColumnDescription, String> edgeLabels = Collections.synchronizedSortedMap(new TreeMap<ColumnDescription, String>());
		for (Integer key : labels.keySet()) {
			if (graph.getDataFormat().getColumnDescription(key).getCategory() == ColumnDescription.DEPENDENCY_EDGE_LABEL) {
				edgeLabels.put(graph.getDataFormat().getColumnDescription(key), labels.get(key));
			}
		}
		return edgeLabels;
	}
	
	/**
	 * Returns the predecessor dependency node in the linear order of the token nodes.
	 * 
	 * @return the predecessor dependency node in the linear order of the token nodes.
	 */
	public ConcurrentDependencyNode getPredecessor() {
		return index > 1 ? graph.getDependencyNode(index - 1) : null;
	}
	
	/**
	 * Returns the successor dependency node in the linear order of the token nodes.
	 * 
	 * @return the successor dependency node in the linear order of the token nodes.
	 */
	public ConcurrentDependencyNode getSuccessor() {
		return graph.getDependencyNode(index + 1);
	}
	
	/**
	 * Returns <i>true</i> if the node is a root node, otherwise <i>false</i>.
	 * 
	 * @return <i>true</i> if the node is a root node, otherwise <i>false</i>.
	 */
	public boolean isRoot() {
		return index == 0;
	}
	
	/**
	 * Returns <i>true</i> if the node has at most one head, otherwise <i>false</i>.
	 * 
	 * Note: this method will always return true because the concurrent dependency graph implementation only supports one or zero head.
	 * 
	 * @return <i>true</i> if the node has at most one head, otherwise <i>false</i>.
	 */
	public boolean hasAtMostOneHead() {
		return true;
	}
	
	/**
	 * Returns <i>true</i> if the node has one or more head(s), otherwise <i>false</i>.
	 * 
	 * @return <i>true</i> if the node has one or more head(s), otherwise <i>false</i>.
	 */ 
	public boolean hasHead() {
		return headIndex != -1;
	}
	
	/**
	 * Returns <i>true</i> if the node has one or more dependents, otherwise <i>false</i>.
	 * 
	 * @return <i>true</i> if the node has one or more dependents, otherwise <i>false</i>.
	 */
	public boolean hasDependent() {
		return graph.hasDependent(index);
	}
	
	/**
	 * Returns <i>true</i> if the node has one or more left dependents, otherwise <i>false</i>.
	 * 
	 * @return <i>true</i> if the node has one or more left dependents, otherwise <i>false</i>.
	 */
	public boolean hasLeftDependent() {
		return graph.hasLeftDependent(index);
	}
	
	/**
	 * Returns <i>true</i> if the node has one or more right dependents, otherwise <i>false</i>.
	 * 
	 * @return <i>true</i> if the node has one or more right dependents, otherwise <i>false</i>.
	 */
	public boolean hasRightDependent() {
		return graph.hasRightDependent(index);
	}
	

	/**
	 * Returns a sorted set of head nodes. If the head is missing a empty set is returned. 
	 * 
	 * Note: In this implementation there will at most be one head.
	 * 
	 * @return a sorted set of head nodes.
	 */
	public SortedSet<ConcurrentDependencyNode> getHeads() {
		SortedSet<ConcurrentDependencyNode> heads = Collections.synchronizedSortedSet(new TreeSet<ConcurrentDependencyNode>());
		ConcurrentDependencyNode head = getHead();
		if (head != null) {
			heads.add(head);
		}
		return heads; 
	}
	
	/**
	 * Returns the head dependency node if it exists, otherwise <i>null</i>. 
	 * 
	 * @return the head dependency node if it exists, otherwise <i>null</i>.
	 */
	public ConcurrentDependencyNode getHead() {
		return graph.getDependencyNode(headIndex);
	}

	/**
	 * Returns the left dependent at the position <i>leftDependentIndex</i>, where <i>leftDependentIndex==0</i> equals the left most dependent.
	 * 
	 * @param leftDependentIndex the index of the left dependent
	 * @return the left dependent at the position <i>leftDependentIndex</i>, where <i>leftDependentIndex==0</i> equals the left most dependent
	 */
	public ConcurrentDependencyNode getLeftDependent(int leftDependentIndex) {	
		List<ConcurrentDependencyNode> leftDependents = graph.getListOfLeftDependents(index);
		if (leftDependentIndex >= 0 && leftDependentIndex < leftDependents.size()) {
			return leftDependents.get(leftDependentIndex);
		}
		return null;
	}

	/**
	 * Return the number of left dependents
	 * 
	 * @return the number of left dependents
	 */
	public int getLeftDependentCount() {
		return graph.getListOfLeftDependents(index).size();
	}

	/**
	 * Returns a sorted set of left dependents.
	 * 
	 * @return a sorted set of left dependents.
	 */
	public SortedSet<ConcurrentDependencyNode> getLeftDependents() {
		return graph.getSortedSetOfLeftDependents(index);
	}

	/**
	 * Returns a list of left dependents.
	 * 
	 * @return a list of left dependents.
	 */
	public List<ConcurrentDependencyNode> getListOfLeftDependents() {
		return graph.getListOfLeftDependents(index);
	}
	
	/**
	 * Returns the left sibling if it exists, otherwise <code>null</code>
	 * 
	 * @return the left sibling if it exists, otherwise <code>null</code>
	 */
	public ConcurrentDependencyNode getLeftSibling() {
		if (headIndex == -1) {
			return null;
		}
		
		int nodeDepedentPosition = 0;
		List<ConcurrentDependencyNode> headDependents = getHead().getListOfDependents();
		for (int i = 0; i < headDependents.size(); i++) {
			if (headDependents.get(i).getIndex() == index) {
				nodeDepedentPosition = i;
				break;
			}
		}
		
		return (nodeDepedentPosition > 0) ? headDependents.get(nodeDepedentPosition - 1) : null;
	}

	/**
	 * Returns the left sibling at the same side of head as the node it self. If not found <code>null</code is returned
	 * 
	 * @return the left sibling at the same side of head as the node it self. If not found <code>null</code is returned
	 */
	public ConcurrentDependencyNode getSameSideLeftSibling() {
		if (headIndex == -1) {
			return null;
		}
		
		List<ConcurrentDependencyNode> headDependents;
		if (index < headIndex) {
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

	/**
	 * Returns the closest left dependent of the node
	 * 
	 * @return the closest left dependent of the node
	 */
	public ConcurrentDependencyNode getClosestLeftDependent() {
		List<ConcurrentDependencyNode> leftDependents = graph.getListOfLeftDependents(index);
		return (leftDependents.size() > 0) ? leftDependents.get(leftDependents.size() - 1) : null;
	}
	
	/**
	 * Returns the leftmost dependent
	 * 
	 * @return the leftmost dependent
	 */
	public ConcurrentDependencyNode getLeftmostDependent() {
		List<ConcurrentDependencyNode> leftDependents = graph.getListOfLeftDependents(index);
		return (leftDependents.size() > 0) ? leftDependents.get(0) : null;
	}

	/**
	 * Returns the right dependent at the position <i>rightDependentIndex</i>, where <i>rightDependentIndex==0</i> equals the right most dependent
	 * 
	 * @param rightDependentIndex the index of the right dependent
	 * @return the right dependent at the position <i>rightDependentIndex</i>, where <i>rightDependentIndex==0</i> equals the right most dependent
	 */
	public ConcurrentDependencyNode getRightDependent(int rightDependentIndex) {	
		List<ConcurrentDependencyNode> rightDependents = graph.getListOfRightDependents(index);
		if (rightDependentIndex >= 0 && rightDependentIndex < rightDependents.size()) {
			return rightDependents.get(rightDependents.size() - 1 - rightDependentIndex);
		}
		return null;
	}
	
	/**
	 * Return the number of right dependents
	 * 
	 * @return the number of right dependents
	 */
	public int getRightDependentCount() {
		return graph.getListOfRightDependents(index).size();
	}

	/**
	 * Returns a sorted set of right dependents.
	 * 
	 * @return a sorted set of right dependents.
	 */
	public SortedSet<ConcurrentDependencyNode> getRightDependents() {
		return graph.getSortedSetOfRightDependents(index);
	}

	/**
	 * Returns a list of right dependents.
	 * 
	 * @return a list of right dependents.
	 */
	public List<ConcurrentDependencyNode> getListOfRightDependents() {
		return graph.getListOfRightDependents(index);
	}
	
	/**
	 * Returns the right sibling if it exists, otherwise <code>null</code>
	 * 
	 * @return the right sibling if it exists, otherwise <code>null</code>
	 */
	public ConcurrentDependencyNode getRightSibling() {
		if (headIndex == -1) {
			return null;
		}
		
		List<ConcurrentDependencyNode> headDependents = getHead().getListOfDependents();
		int nodeDepedentPosition = headDependents.size() - 1;
		for (int i = headDependents.size() - 1; i >= 0 ; i--) {
			if (headDependents.get(i).getIndex() == index) {
				nodeDepedentPosition = i;
				break;
			}
		}
		
		return (nodeDepedentPosition < headDependents.size() - 1) ? headDependents.get(nodeDepedentPosition + 1) : null;
	}

	/**
	 * Returns the right sibling at the same side of head as the node it self. If not found <code>null</code is returned
	 * 
	 * @return the right sibling at the same side of head as the node it self. If not found <code>null</code is returned
	 */
	public ConcurrentDependencyNode getSameSideRightSibling() {
		if (headIndex == -1) {
			return null;
		}
		
		List<ConcurrentDependencyNode> headDependents;
		if (index < headIndex) {
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

	/**
	 * Returns the closest right dependent of the node
	 * 
	 * @return the closest right dependent of the node
	 */
	public ConcurrentDependencyNode getClosestRightDependent() {
		List<ConcurrentDependencyNode> rightDependents = graph.getListOfRightDependents(index);
		return (rightDependents.size() > 0) ? rightDependents.get(0) : null;
	}
	
	/**
	 * Returns the rightmost dependent
	 * 
	 * @return the rightmost dependent
	 */
	public ConcurrentDependencyNode getRightmostDependent(){
		List<ConcurrentDependencyNode> rightDependents = graph.getListOfRightDependents(index);
		return (rightDependents.size() > 0) ? rightDependents.get(rightDependents.size() - 1) : null;
	}
	
	/**
	 * Returns a sorted set of dependents.
	 * 
	 * @return a sorted set of dependents.
	 */
	public SortedSet<ConcurrentDependencyNode> getDependents() {
		return graph.getSortedSetOfDependents(index);
	}
	
	/**
	 * Returns a list of dependents.
	 * 
	 * @return a list of dependents.
	 */
	public List<ConcurrentDependencyNode> getListOfDependents() {
		return graph.getListOfDependents(index);
	}
	
	/**
	 * Returns the in degree of the node (number of incoming edges).
	 * 
	 * @return the in degree of the node (number of incoming edges).
	 */
	public int getInDegree() {
		if (hasHead()) {
			return 1;
		}
		return 0;
	}
	
	/**
	 * Returns the out degree of the node (number of outgoing edges).
	 * 
	 * @return the out degree of the node (number of outgoing edges).
	 */
	public int getOutDegree() {
		return graph.getListOfDependents(index).size();
	}
	
	public ConcurrentDependencyNode getAncestor() {
		if (!this.hasHead()) {
			return this;
		}
		
		ConcurrentDependencyNode tmp = this;
		while (tmp.hasHead()) {
			tmp = tmp.getHead();
		}
		return tmp;
	}
	
	public ConcurrentDependencyNode getProperAncestor() {
		if (!this.hasHead()) {
			return null;
		}
		
		ConcurrentDependencyNode tmp = this;
		while (tmp.hasHead() && !tmp.isRoot()) {
			tmp = tmp.getHead();
		}
		return tmp;
	}
	
	public boolean hasAncestorInside(int left, int right) {
		if (index == 0) {
			return false;
		}
		ConcurrentDependencyNode tmp = this;
		if (tmp.getHead() != null) {
			tmp = tmp.getHead();
			if (tmp.getIndex() >= left && tmp.getIndex() <= right) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns <i>true</i> if the head edge is projective, otherwise <i>false</i>. 
	 * 
	 * @return <i>true</i> if the head edge is projective, otherwise <i>false</i>.
	 */
	public boolean isProjective() {
		if (headIndex > 0) {
			final ConcurrentDependencyNode head = getHead();
			if (headIndex < index) {
				ConcurrentDependencyNode terminals = head;
				ConcurrentDependencyNode tmp = null;
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
				ConcurrentDependencyNode terminals = this;
				ConcurrentDependencyNode tmp = null;
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
	
	/**
	 * Returns the depth of the node. The root node has the depth 0.
	 * @return the depth of the node.
	 */
	public int getDependencyNodeDepth() {
		ConcurrentDependencyNode tmp = this;
		int depth = 0;
		while (tmp.hasHead()) {
			depth++;
			tmp = tmp.getHead();
		}
		return depth;
	}

	/**
	 * Returns the left-most proper terminal descendant node (excluding itself). 
	 * 
	 * @return the left-most proper terminal descendant node. 
	 */
	public ConcurrentDependencyNode getLeftmostProperDescendant() {
		ConcurrentDependencyNode candidate = null;
		List<ConcurrentDependencyNode> dependents = graph.getListOfDependents(index);
		for (int i = 0; i < dependents.size(); i++) {
			final ConcurrentDependencyNode dep = dependents.get(i);
			if (candidate == null || dep.getIndex() < candidate.getIndex()) {
				candidate = dep;
			}
			final ConcurrentDependencyNode tmp = dep.getLeftmostProperDescendant();
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

	/**
	 * Returns the right-most proper terminal descendant node (excluding itself). 
	 * 
	 * @return the right-most proper terminal descendant node. 
	 */
	public ConcurrentDependencyNode getRightmostProperDescendant() {
		ConcurrentDependencyNode candidate = null;
		List<ConcurrentDependencyNode> dependents = graph.getListOfDependents(index);
		for (int i = 0; i < dependents.size(); i++) {
			final ConcurrentDependencyNode dep = dependents.get(i);
			if (candidate == null || dep.getIndex() > candidate.getIndex()) {
				candidate = dep;
			}
			final ConcurrentDependencyNode tmp = dep.getRightmostProperDescendant();
			if (tmp == null) {
				continue;
			}
			if (candidate == null || tmp.getIndex() > candidate.getIndex()) {
				candidate = tmp;
			}
		}
		return candidate;
	}

	/**
	 * Returns the index of the left-most proper terminal descendant node (excluding itself). 
	 * 
	 * @return the index of the left-most proper terminal descendant node. 
	 */
	public int getLeftmostProperDescendantIndex() {
		ConcurrentDependencyNode node = getLeftmostProperDescendant();
		return (node != null)?node.getIndex():-1;
	}

	/**
	 * Returns the index of the right-most proper terminal descendant node (excluding itself). 
	 * 
	 * @return the index of the right-most proper terminal descendant node. 
	 */
	public int getRightmostProperDescendantIndex() {
		ConcurrentDependencyNode node = getRightmostProperDescendant();
		return (node != null)?node.getIndex():-1;
	}

	/**
	 * Returns the left-most terminal descendant node. 
	 * 
	 * @return the left-most terminal descendant node. 
	 */
	public ConcurrentDependencyNode getLeftmostDescendant() {
		ConcurrentDependencyNode candidate = this;
		List<ConcurrentDependencyNode> dependents = graph.getListOfDependents(index);
		for (int i = 0; i < dependents.size(); i++) {
			final ConcurrentDependencyNode dep = dependents.get(i);
			if (dep.getIndex() < candidate.getIndex()) {
				candidate = dep;
			}
			final ConcurrentDependencyNode tmp = dep.getLeftmostDescendant();
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

	/**
	 * Returns the right-most terminal descendant node. 
	 * 
	 * @return the right-most terminal descendant node. 
	 */
	public ConcurrentDependencyNode getRightmostDescendant() {
		ConcurrentDependencyNode candidate = this;
		List<ConcurrentDependencyNode> dependents = graph.getListOfDependents(index);
		for (int i = 0; i < dependents.size(); i++) {
			final ConcurrentDependencyNode dep = dependents.get(i);
			if (dep.getIndex() > candidate.getIndex() ) {
				candidate = dep;
			}
			final ConcurrentDependencyNode tmp = dep.getRightmostDescendant();
			if (tmp == null) {
				continue;
			}
			if (tmp.getIndex() > candidate.getIndex() ) {
				candidate = tmp;
			}
		}
		return candidate;
	}

	/**
	 * Returns the index of the left-most terminal descendant node. 
	 * 
	 * @return the index of the left-most terminal descendant node. 
	 */
	public int getLeftmostDescendantIndex() {
		ConcurrentDependencyNode node = getLeftmostDescendant();
		return (node != null)?node.getIndex():this.getIndex();
	}

	/**
	 * Returns the index of the right-most terminal descendant node. 
	 * 
	 * @return the index of the right-most terminal descendant node. 
	 */
	public int getRightmostDescendantIndex() {
		ConcurrentDependencyNode node = getRightmostDescendant();
		return (node != null)?node.getIndex():this.getIndex();
	}
	
	public ConcurrentDependencyNode findComponent() {
		return graph.findComponent(index);
	}
	
	public int getRank() {
		return graph.getRank(index);
	}

	public ConcurrentDependencyEdge getHeadEdge() throws ConcurrentGraphException {
		if (!hasHead()) {
			return null;
		}
		return new ConcurrentDependencyEdge(graph.getDataFormat(), getHead(), this, labels);
	}
	
	public SortedSet<ConcurrentDependencyEdge> getHeadEdges() throws ConcurrentGraphException {
		SortedSet<ConcurrentDependencyEdge> edges = Collections.synchronizedSortedSet(new TreeSet<ConcurrentDependencyEdge>());
		if (hasHead()) {
			edges.add(new ConcurrentDependencyEdge(graph.getDataFormat(), getHead(), this, labels));
		}
		return edges;
	}
	
	public boolean isHeadEdgeLabeled() {
		return getEdgeLabels().size() > 0;
	}
	
	public int nHeadEdgeLabels() {
		return getEdgeLabels().size();
	}
	
	public DataFormat getDataFormat() {
		return graph.getDataFormat();
	}
	
	public int compareTo(ConcurrentDependencyNode that) {
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
		result = prime * result + headIndex;
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
		ConcurrentDependencyNode other = (ConcurrentDependencyNode) obj;
		if (headIndex != other.headIndex)
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
					sb.append(headIndex);
				} else if (column.getCategory() == ColumnDescription.INPUT || column.getCategory() == ColumnDescription.DEPENDENCY_EDGE_LABEL) {
					sb.append(labels.get(column.getPosition()));
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
