package org.maltparser.concurrent.graph.dataformat;


/**
 *
 * @author Johan Hall
**/
public final class ColumnDescription implements Comparable<ColumnDescription> {
	// Categories
	public static final int INPUT = 1;
	public static final int HEAD = 2;
	public static final int DEPENDENCY_EDGE_LABEL = 3;
	public static final int PHRASE_STRUCTURE_EDGE_LABEL = 4;
	public static final int PHRASE_STRUCTURE_NODE_LABEL = 5;
	public static final int SECONDARY_EDGE_LABEL = 6;
	public static final int IGNORE = 7;
	public static final String[] categories = { "", "INPUT", "HEAD", "DEPENDENCY_EDGE_LABEL", "PHRASE_STRUCTURE_EDGE_LABEL", "PHRASE_STRUCTURE_NODE_LABEL", "SECONDARY_EDGE_LABEL", "IGNORE" };
	
	// Types
	public static final int STRING = 1;
	public static final int INTEGER = 2;
	public static final int BOOLEAN = 3;
	public static final int REAL = 4;
	public static final String[] types = { "", "STRING", "INTEGER", "BOOLEAN", "REAL" };
	
	private final int position;
	private final String name;
	private final int category;
	private final int type;
	private final String defaultOutput;
	private final boolean internal;
	
	public ColumnDescription(ColumnDescription columnDescription) {
		this.position = columnDescription.position;
		this.name = columnDescription.name;
		this.category = columnDescription.category;
		this.type = columnDescription.type;
		this.defaultOutput = columnDescription.defaultOutput;
		this.internal = columnDescription.internal;
	}
	
	public ColumnDescription(int _position, String _name, int _category, int _type, String _defaultOutput, boolean _internal) { 
		this.position = _position;
		this.name = _name.toUpperCase();
		this.category = _category;
		this.type = _type;
		this.defaultOutput = _defaultOutput;
		this.internal = _internal;
	}
	
	public int getPosition() {
		return position;
	}

	public String getName() {
		return name;
	}
	
	public String getDefaultOutput() {
		return defaultOutput;
	}
	
	public int getCategory() {
		return category;
	}
	
	public String getCategoryName() {
		if (category < 1 || category > 7 ) {
			return "";
		}
		return categories[category];
	}
	
	public int getType() {
		return type;
	}

	public String getTypeName() {
		if (type < 1 || type > 4 ) {
			return "";
		}
		return types[type];
	}
	
	public boolean isInternal() {
		return internal;
	}
	
	public int compareTo(ColumnDescription that) {
		final int BEFORE = -1;
	    final int EQUAL = 0;
	    final int AFTER = 1;
	    if (this == that) return EQUAL;
	    if (this.position < that.position) return BEFORE;
	    if (this.position > that.position) return AFTER;
	    return EQUAL;
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + category;
		result = prime * result
				+ ((defaultOutput == null) ? 0 : defaultOutput.hashCode());
		result = prime * result + (internal ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + position;
		result = prime * result + type;
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
		ColumnDescription other = (ColumnDescription) obj;
		if (category != other.category)
			return false;
		if (defaultOutput == null) {
			if (other.defaultOutput != null)
				return false;
		} else if (!defaultOutput.equals(other.defaultOutput))
			return false;
		if (internal != other.internal)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (position != other.position)
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append('\t');
		sb.append(category);
		sb.append('\t');
		sb.append(type);
		if (defaultOutput != null) {
			sb.append('\t');
			sb.append(defaultOutput);
		}
		sb.append('\t');
		sb.append(internal);
		return sb.toString();
	}
	
	public static int getCategory(String categoryName)  {
		if (categoryName.equals("INPUT")) {
			return ColumnDescription.INPUT;
		} else if (categoryName.equals("HEAD")) {
			return ColumnDescription.HEAD;
		} else if (categoryName.equals("OUTPUT")) {
			return ColumnDescription.DEPENDENCY_EDGE_LABEL;
		} else if (categoryName.equals("DEPENDENCY_EDGE_LABEL")) {
			return ColumnDescription.DEPENDENCY_EDGE_LABEL;
		} else if (categoryName.equals("IGNORE")) {
			return ColumnDescription.IGNORE;
		}
		return -1;
	}
	
	public static int getType(String typeName) {
		if (typeName.equals("STRING")) {
			return ColumnDescription.STRING;
		} else if (typeName.equals("INTEGER")) {
			return ColumnDescription.INTEGER;
		} else if (typeName.equals("BOOLEAN")) {
			return ColumnDescription.BOOLEAN;
		} else if (typeName.equals("REAL")) {
			return ColumnDescription.REAL;
		} else if (typeName.equals("ECHO")) {
			// ECHO is removed, but if it occurs in the data format file it will be interpreted as an integer instead.
			return ColumnDescription.INTEGER;
		} 
		return -1;
	}
}
