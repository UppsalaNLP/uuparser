package org.maltparser.concurrent.graph.dataformat;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.maltparser.concurrent.graph.ConcurrentGraphException;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.helper.HashMap;
import org.maltparser.core.helper.URLFinder;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
*
* @author Johan Hall
**/
public class DataFormat {
	private final String name;
	private final ColumnDescription[] columns;
	private final HashMap<String, ColumnDescription> columnMap;
	
	public DataFormat(DataFormat dataFormat) {
		this.name = dataFormat.name;
		this.columns = new ColumnDescription[dataFormat.columns.length];
		this.columnMap = new HashMap<String, ColumnDescription>();
		for (int i = 0; i < dataFormat.columns.length; i++) {
			this.columns[i] = new ColumnDescription(dataFormat.columns[i]);
			this.columnMap.put(this.columns[i].getName(), this.columns[i]);
		}
	}
	
	public DataFormat(String name, ColumnDescription[] columns) {
		this.name = name;
		this.columns = new ColumnDescription[columns.length];
		this.columnMap = new HashMap<String, ColumnDescription>();
		for (int i = 0; i < columns.length; i++) {
			this.columns[i] = new ColumnDescription(columns[i]);
			this.columnMap.put(this.columns[i].getName(), this.columns[i]);
		}
	}
	
	public DataFormat(String name, ArrayList<ColumnDescription> columns) {
		this.name = name;
		this.columns = new ColumnDescription[columns.size()];
		this.columnMap = new HashMap<String, ColumnDescription>();
		for (int i = 0; i < columns.size(); i++) {
			this.columns[i] = new ColumnDescription(columns.get(i));
			this.columnMap.put(this.columns[i].getName(), this.columns[i]);
		}
	}
	
	public String getName() {
		return name;
	}
	
	public ColumnDescription getColumnDescription(int position) {
		return columns[position];
	}
	
	public ColumnDescription getColumnDescription(String columnName) {
		ColumnDescription columnDescription = columnMap.get(columnName);
		if (columnDescription != null)
			return columnDescription;
		
		for (int i = 0; i < columns.length; i++) {
			if (columns[i].getName().equals(columnName.toUpperCase())) {
				this.columnMap.put(columnName, columns[i]);
				return columns[i];
			}
		}
		return null;
	}
	
	public SortedSet<ColumnDescription> getSelectedColumnDescriptions(Set<Integer> positionSet) {
		SortedSet<ColumnDescription> selectedColumns = Collections.synchronizedSortedSet(new TreeSet<ColumnDescription>());
		for (int i = 0; i < columns.length; i++) {
			if (positionSet.contains(columns[i].getPosition())) {
				selectedColumns.add(columns[i]);
			}
		}
		return selectedColumns;
	}
	
	public Set<String> getLabelNames() {
		Set<String> labelNames = Collections.synchronizedSet(new HashSet<String>());
		for (int i = 0; i < columns.length; i++) {
			labelNames.add(columns[i].getName());
		}
		return labelNames;
	}
	
	public int numberOfColumns() {
		return columns.length;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(columns);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		DataFormat other = (DataFormat) obj;
		if (!Arrays.equals(columns, other.columns))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append('\n');
		for (int i = 1; i < columns.length; i++) {
			sb.append(columns[i]);
			sb.append('\n');
		}
		
		return sb.toString();
	}
	
	public static DataFormat parseDataFormatXMLfile(String fileName) throws MaltChainedException {
		return parseDataFormatXMLfile(new URLFinder().findURL(fileName));
	}
	
	public static DataFormat parseDataFormatXMLfile(URL url) throws ConcurrentGraphException {
		if (url == null) {
			throw new ConcurrentGraphException("The data format specification file cannot be found. ");
		}
		String dataFormatName;
		ArrayList<ColumnDescription> columns = new ArrayList<ColumnDescription>();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();

    		Element root = db.parse(url.openStream()).getDocumentElement();
    		if (root.getNodeName().equals("dataformat")) { 
    			dataFormatName = root.getAttribute("name");
    		} else {
    			throw new ConcurrentGraphException("Data format specification file must contain one 'dataformat' element. ");
    		}
    		NodeList cols = root.getElementsByTagName("column");
            Element col = null;
            int i = 0;
            for (; i < cols.getLength(); i++) {
            	col = (Element)cols.item(i);
            	ColumnDescription column = new ColumnDescription(
            			i, 
            			col.getAttribute("name"), 
            			ColumnDescription.getCategory(col.getAttribute("category")),
            			ColumnDescription.getType(col.getAttribute("type")), 
            			col.getAttribute("default"), false);
            	columns.add(column);
            }
            columns.add(new ColumnDescription(i++, "PPPATH", ColumnDescription.DEPENDENCY_EDGE_LABEL, ColumnDescription.STRING, "_", true));
            columns.add(new ColumnDescription(i++, "PPLIFTED", ColumnDescription.DEPENDENCY_EDGE_LABEL, ColumnDescription.STRING, "_", true));
            columns.add(new ColumnDescription(i++, "PPCOVERED", ColumnDescription.DEPENDENCY_EDGE_LABEL, ColumnDescription.STRING, "_", true));
        } catch (java.io.IOException e) {
        	throw new ConcurrentGraphException("Cannot find the file "+url.toString()+". ", e);
        } catch (ParserConfigurationException e) {
        	throw new ConcurrentGraphException("Problem parsing the file "+url.toString()+". ", e);
        } catch (SAXException e) {
        	throw new ConcurrentGraphException("Problem parsing the file "+url.toString()+". ", e);
        }
        return new DataFormat(dataFormatName, columns);
	}
}
