package org.maltparser.concurrent.graph;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import org.maltparser.concurrent.graph.dataformat.ColumnDescription;
import org.maltparser.concurrent.graph.dataformat.DataFormat;
/**
* Immutable and tread-safe dependency edge implementation.
* 
* @author Johan Hall
*/
public final class ConcurrentDependencyEdge implements Comparable<ConcurrentDependencyEdge> {
	private final ConcurrentDependencyNode source;
	private final ConcurrentDependencyNode target;
	private final SortedMap<Integer, String> labels;
	
	protected ConcurrentDependencyEdge(ConcurrentDependencyEdge edge) throws ConcurrentGraphException {
		this.source = edge.source;
		this.target = edge.target;
		this.labels = new TreeMap<Integer, String>(edge.labels);
	}
	
	protected ConcurrentDependencyEdge(DataFormat dataFormat, ConcurrentDependencyNode _source, ConcurrentDependencyNode _target, SortedMap<Integer, String> _labels) throws ConcurrentGraphException {
		if (_source == null) {
			throw new ConcurrentGraphException("Not allowed to have an edge without a source node");
		}
		if (_target == null) {
			throw new ConcurrentGraphException("Not allowed to have an edge without a target node");
		}
		this.source = _source;
		this.target = _target;
		if (this.target.getIndex() == 0) {
			throw new ConcurrentGraphException("Not allowed to have an edge target as root node");
		}
		this.labels = new TreeMap<Integer, String>();
		if (_labels != null) {
			for (Integer i : _labels.keySet()) {
				if (dataFormat.getColumnDescription(i).getCategory() == ColumnDescription.DEPENDENCY_EDGE_LABEL) {
					this.labels.put(i, _labels.get(i));
				}
			}
		}
	}
	
	/**
	 * Returns the source node of the edge.
	 * 
	 * @return the source node of the edge.
	 */
	public ConcurrentDependencyNode getSource() {
		return source;
	}

	/**
	 * Returns the target node of the edge.
	 * 
	 * @return the target node of the edge.
	 */
	public ConcurrentDependencyNode getTarget() {
		return target;
	}
	
	/**
	 * Returns an edge label
	 * 
	 * @param column a column description that describes the label
	 * @return an edge label described by the column description. An empty string is returned if the label is not found. 
	 */
	public String getLabel(ColumnDescription column) {
		if (labels.containsKey(column.getPosition())) {
			return labels.get(column.getPosition());
		} else if (column.getCategory() == ColumnDescription.IGNORE) {
			return column.getDefaultOutput();
		}
		return "";
	}
	
	/**
	 * Returns an edge label
	 * 
	 * @param columnName the name of the column that describes the label.
	 * @return an edge label. An empty string is returned if the label is not found.
	 */
	public String getLabel(String columnName) {
		ColumnDescription column = source.getDataFormat().getColumnDescription(columnName);
		if (column != null) {
			if (labels.containsKey(column.getPosition())) {
				return labels.get(column.getPosition());
			} else if (column.getCategory() == ColumnDescription.IGNORE) {
				return column.getDefaultOutput();
			}
		}
		return "";
	}
	
	/**
	 * Returns the number of labels of the edge.
	 * 
	 * @return the number of labels of the edge.
	 */
	public int nLabels() {
		return labels.size();
	}
	
	/**
	 * Returns <i>true</i> if the edge has one or more labels, otherwise <i>false</i>.
	 * 
	 * @return <i>true</i> if the edge has one or more labels, otherwise <i>false</i>.
	 */
	public boolean isLabeled() {
		return labels.size() > 0;
	}

	public int compareTo(ConcurrentDependencyEdge that) {
		final int BEFORE = -1;
	    final int EQUAL = 0;
	    final int AFTER = 1;
	    
	    if (this == that) return EQUAL;
	    
	    if (this.target.getIndex() < that.target.getIndex()) return BEFORE;
	    if (this.target.getIndex() > that.target.getIndex()) return AFTER;
	    
	    if (this.source.getIndex() < that.source.getIndex()) return BEFORE;
	    if (this.source.getIndex() > that.source.getIndex()) return AFTER;
	    
	    
	    if (this.labels.equals(that.labels)) return EQUAL;
		
		Iterator<Integer> itthis = this.labels.keySet().iterator();
		Iterator<Integer> itthat = that.labels.keySet().iterator();
		while (itthis.hasNext() && itthat.hasNext()) {
			int keythis = itthis.next();
			int keythat = itthat.next();
			if (keythis < keythat) return BEFORE;
			if (keythis > keythat) return AFTER;
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
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
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
		ConcurrentDependencyEdge other = (ConcurrentDependencyEdge) obj;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		if (labels == null) {
			if (other.labels != null)
				return false;
		} else if (!labels.equals(other.labels))
			return false;
		return true;
	}
}
