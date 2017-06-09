package org.maltparser.core.lw.graph;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.maltparser.concurrent.graph.dataformat.ColumnDescription;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.syntaxgraph.LabelSet;
import org.maltparser.core.syntaxgraph.LabeledStructure;
import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.core.syntaxgraph.node.Node;

/**
* A lightweight version of org.maltparser.core.syntaxgraph.edge.GraphEdge.
* 
* @author Johan Hall
*/
public final class LWEdge implements Edge, Comparable<LWEdge> {
	private final Node source;
	private final Node target;
	private final SortedMap<ColumnDescription, String> labels;
	
	protected LWEdge(LWEdge edge) throws LWGraphException {
		this.source = edge.source;
		this.target = edge.target;
		this.labels = new TreeMap<ColumnDescription, String>(edge.labels);
	}
	
	protected LWEdge(Node _source, Node _target, SortedMap<ColumnDescription, String> _labels) throws MaltChainedException {
		if (_source.getBelongsToGraph() != _target.getBelongsToGraph()) {
		throw new LWGraphException("The source node and target node must belong to the same dependency graph.");
		}
		this.source = _source;
		this.target = _target;
		this.labels = _labels;
		SymbolTableHandler symbolTableHandler = getBelongsToGraph().getSymbolTables();
		for (ColumnDescription column : labels.keySet()) {
			SymbolTable table = symbolTableHandler.addSymbolTable(column.getName());
			table.addSymbol(labels.get(column));
		}
	}
	
	protected LWEdge(Node _source, Node _target) throws MaltChainedException {
		if (_source.getBelongsToGraph() != _target.getBelongsToGraph()) {
			throw new LWGraphException("The source node and target node must belong to the same dependency graph.");
		}
		this.source = _source;
		this.target = _target;
		this.labels = new TreeMap<ColumnDescription, String>();
	}
	
	public Node getSource() {
		return source;
	}

	public Node getTarget() {
		return target;
	}
	
	public String getLabel(ColumnDescription column) {
		if (labels.containsKey(column)) {
			return labels.get(column);
		} else if (column.getCategory() == ColumnDescription.IGNORE) {
			return column.getDefaultOutput();
		}
		return "";
	}
	
	public int nLabels() {
		return labels.size();
	}
	
	public boolean isLabeled() {
		return labels.size() > 0;
	}

	
	@Override
	public void setEdge(Node source, Node target, int type)
			throws MaltChainedException {
		throw new LWGraphException("Not implemented in light-weight dependency graph");
	}

	@Override
	public int getType() {
		return DEPENDENCY_EDGE;
	}

	
	/**
	 * Adds a label (a string value) to the symbol table and to the graph element. 
	 * 
	 * @param table the symbol table
	 * @param symbol a label symbol
	 * @throws MaltChainedException
	 */
	public void addLabel(SymbolTable table, String symbol) throws MaltChainedException {
		LWDependencyGraph graph = (LWDependencyGraph)getBelongsToGraph();
		ColumnDescription column = graph.getDataFormat().getColumnDescription(table.getName());
		table.addSymbol(symbol);
		labels.put(column, symbol);
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
	 * @param labelSet a label set.
	 * @throws MaltChainedException
	 */
	public void addLabel(LabelSet labelSet) throws MaltChainedException {
		for (SymbolTable table : labelSet.keySet()) {
			addLabel(table, labelSet.get(table));
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
		if (table == null) {
			return false;
		}
		LWDependencyGraph graph = (LWDependencyGraph)getBelongsToGraph();
		ColumnDescription column = graph.getDataFormat().getColumnDescription(table.getName());
		return labels.containsKey(column);
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
		LWDependencyGraph graph = (LWDependencyGraph)getBelongsToGraph();
		ColumnDescription column = graph.getDataFormat().getColumnDescription(table.getName());
		return labels.get(column);
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
		LWDependencyGraph graph = (LWDependencyGraph)getBelongsToGraph();
		ColumnDescription column = graph.getDataFormat().getColumnDescription(table.getName());
		return table.getSymbolStringToCode(labels.get(column));
	}
	
	/**
	 * Returns a set of symbol tables (labeling functions or label types) that labels the graph element.
	 * 
	 * @return a set of symbol tables (labeling functions or label types)
	 */
	public Set<SymbolTable> getLabelTypes() {
		Set<SymbolTable> labelTypes = new HashSet<SymbolTable>();
		SymbolTableHandler symbolTableHandler = getBelongsToGraph().getSymbolTables();
		for (ColumnDescription column : labels.keySet()) {
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
		
		for (ColumnDescription column : labels.keySet()) {
			try {
				SymbolTable table = symbolTableHandler.getSymbolTable(column.getName());
				int code = table.getSymbolStringToCode(labels.get(column));
				labelSet.put(table, code);
			} catch (MaltChainedException e) {
				e.printStackTrace();
			}
		}
		return labelSet;
	}
	
	public void removeLabel(SymbolTable table) throws MaltChainedException {
		LWDependencyGraph graph = (LWDependencyGraph)getBelongsToGraph();
		ColumnDescription column = graph.getDataFormat().getColumnDescription(table.getName());
		labels.remove(column);
	}
	
	public void removeLabels() throws MaltChainedException {
		labels.clear();
	}
	
	/**
	 * Returns the graph (structure) in which the graph element belongs to. 
	 * 
	 * @return the graph (structure) in which the graph element belongs to. 
	 */
	public LabeledStructure getBelongsToGraph() {
		return target.getBelongsToGraph();
	}
	
	public void setBelongsToGraph(LabeledStructure belongsToGraph) { }
	

	/**
	 * Resets the graph element.
	 * 
	 * @throws MaltChainedException
	 */
	public void clear() throws MaltChainedException {
		labels.clear();
	}
	
	public int compareTo(LWEdge that) {
		final int BEFORE = -1;
	    final int EQUAL = 0;
	    final int AFTER = 1;
	    
	    if (this == that) return EQUAL;
	    
	    if (this.target.getIndex() < that.target.getIndex()) return BEFORE;
	    if (this.target.getIndex() > that.target.getIndex()) return AFTER;
	    
	    if (this.source.getIndex() < that.source.getIndex()) return BEFORE;
	    if (this.source.getIndex() > that.source.getIndex()) return AFTER;
	    
	    
	    if (this.labels.equals(that.labels)) return EQUAL;
		
		Iterator<ColumnDescription> itthis = this.labels.keySet().iterator();
		Iterator<ColumnDescription> itthat = that.labels.keySet().iterator();
		while (itthis.hasNext() && itthat.hasNext()) {
			ColumnDescription keythis = itthis.next();
			ColumnDescription keythat = itthat.next();
			if (keythis.getPosition() < keythat.getPosition()) return BEFORE;
			if (keythis.getPosition() > keythat.getPosition()) return AFTER;
			if (this.labels.get(keythis).compareTo(that.labels.get(keythat)) != EQUAL) {
				return this.labels.get(keythis).compareTo(that.labels.get(keythat));
			}	
		}
		if (itthis.hasNext() == false && itthat.hasNext() == true) return BEFORE;
		if (itthis.hasNext() == true && itthat.hasNext() == false) return AFTER;

	    
		return EQUAL;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + source.getIndex();
		result = prime * result + target.getIndex();
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
		LWEdge other = (LWEdge) obj;
		if (source.getIndex() != other.source.getIndex())
			return false;
		if (target.getIndex() != other.target.getIndex())
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
		sb.append(source);
		sb.append(" -> ");
		sb.append(target);
		if (labels.size() > 0) {
			int i = 1;
			sb.append(" {");
			for (ColumnDescription column : labels.keySet()) {
				sb.append(column.getName());
				sb.append('=');
				sb.append(labels.get(column));
				if (i < labels.size()) {
					sb.append(',');
				}
				i++;
			}
			sb.append(" }");
		}
		return sb.toString();
	}
}
