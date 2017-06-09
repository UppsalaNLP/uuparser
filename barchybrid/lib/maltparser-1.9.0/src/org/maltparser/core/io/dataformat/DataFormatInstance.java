package org.maltparser.core.io.dataformat;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.symbol.SymbolTableHandler;

/**
 *  
 *
 * @author Johan Hall
 * @since 1.0
**/
public class DataFormatInstance implements Iterable<ColumnDescription> {
	private final SortedSet<ColumnDescription> columnDescriptions;
	private SortedMap<String,ColumnDescription> headColumnDescriptions;
	private SortedMap<String,ColumnDescription> dependencyEdgeLabelColumnDescriptions;
	private SortedMap<String,ColumnDescription> phraseStructureEdgeLabelColumnDescriptions;
	private SortedMap<String,ColumnDescription> phraseStructureNodeLabelColumnDescriptions;
	private SortedMap<String,ColumnDescription> secondaryEdgeLabelColumnDescriptions;
	private SortedMap<String,ColumnDescription> inputColumnDescriptions;
	private SortedMap<String,ColumnDescription> ignoreColumnDescriptions;
	
	private SortedSet<ColumnDescription> headColumnDescriptionSet;
	private SortedSet<ColumnDescription> dependencyEdgeLabelColumnDescriptionSet;
	private SortedSet<ColumnDescription> phraseStructureEdgeLabelColumnDescriptionSet;
	private SortedSet<ColumnDescription> phraseStructureNodeLabelColumnDescriptionSet;
	private SortedSet<ColumnDescription> secondaryEdgeLabelColumnDescriptionSet;
	private SortedSet<ColumnDescription> inputColumnDescriptionSet;
	private SortedSet<ColumnDescription> ignoreColumnDescriptionSet;
	
	private SortedMap<String,SymbolTable> dependencyEdgeLabelSymbolTables;
	private SortedMap<String,SymbolTable> phraseStructureEdgeLabelSymbolTables;
	private SortedMap<String,SymbolTable> phraseStructureNodeLabelSymbolTables;
	private SortedMap<String,SymbolTable> secondaryEdgeLabelSymbolTables;
	private SortedMap<String,SymbolTable> inputSymbolTables;
	
	// Internal
	private SortedMap<String,ColumnDescription> internalColumnDescriptions;
	private SortedSet<ColumnDescription> internalColumnDescriptionSet;
	
	private final DataFormatSpecification dataFormarSpec;
	
	public DataFormatInstance(Map<String, DataFormatEntry> entries, SymbolTableHandler symbolTables, String nullValueStrategy, DataFormatSpecification dataFormarSpec) throws MaltChainedException {
		this.columnDescriptions = new TreeSet<ColumnDescription>();
		this.dataFormarSpec = dataFormarSpec;
		createColumnDescriptions(symbolTables, entries, nullValueStrategy);
		
	}

	public ColumnDescription addInternalColumnDescription(SymbolTableHandler symbolTables, String name, String category, String type,  String defaultOutput, String nullValueStrategy) throws MaltChainedException {
		if (internalColumnDescriptions == null) {
			internalColumnDescriptions = new TreeMap<String,ColumnDescription>();
			internalColumnDescriptionSet = new TreeSet<ColumnDescription>();
		}
		
		if (!internalColumnDescriptions.containsKey(name)) {
			ColumnDescription internalColumn = new ColumnDescription(name, ColumnDescription.getCategory(category), ColumnDescription.getType(type), defaultOutput,  nullValueStrategy, true);
			symbolTables.addSymbolTable(internalColumn.getName(), internalColumn.getCategory(), internalColumn.getType(), internalColumn.getNullValueStrategy());
			internalColumnDescriptions.put(name, internalColumn);
			internalColumnDescriptionSet.add(internalColumn);
			return internalColumn;
		} else {
			return internalColumnDescriptions.get(name);
		}
	}
	
	public ColumnDescription addInternalColumnDescription(SymbolTableHandler symbolTables, String name, int category, int type,  String defaultOutput, String nullValueStrategy) throws MaltChainedException {
		if (internalColumnDescriptions == null) {
			internalColumnDescriptions = new TreeMap<String,ColumnDescription>();
			internalColumnDescriptionSet = new TreeSet<ColumnDescription>();
		}
		
		if (!internalColumnDescriptions.containsKey(name)) {
			ColumnDescription internalColumn = new ColumnDescription(name, category, type, defaultOutput, nullValueStrategy, true);
			symbolTables.addSymbolTable(internalColumn.getName(), internalColumn.getCategory(), internalColumn.getType(), internalColumn.getNullValueStrategy());
			internalColumnDescriptions.put(name, internalColumn);
			internalColumnDescriptionSet.add(internalColumn);
			return internalColumn;
		} else {
			return internalColumnDescriptions.get(name);
		}
	}
	
	public ColumnDescription addInternalColumnDescription(SymbolTableHandler symbolTables, String name, ColumnDescription column) throws MaltChainedException {
		return addInternalColumnDescription(symbolTables, name, column.getCategory(), column.getType(), column.getDefaultOutput(), column.getNullValueStrategy());
	}
	
	private void createColumnDescriptions(SymbolTableHandler symbolTables, Map<String, DataFormatEntry> entries, String nullValueStrategy) throws MaltChainedException {
		for (DataFormatEntry entry : entries.values()) {
			ColumnDescription column = new ColumnDescription(entry.getDataFormatEntryName(), ColumnDescription.getCategory(entry.getCategory()), ColumnDescription.getType(entry.getType()), entry.getDefaultOutput(),  nullValueStrategy, false);
			symbolTables.addSymbolTable(column.getName(), column.getCategory(), column.getType(), column.getNullValueStrategy());
			columnDescriptions.add(column);
			
		}
	}
	
	public ColumnDescription getColumnDescriptionByName(String name) {
		for (ColumnDescription column : columnDescriptions) {
			if (column.getName().equals(name)) {
				return column;
			}
		}
		if (internalColumnDescriptionSet != null) {
			for (ColumnDescription internalColumn : internalColumnDescriptionSet) {
				if (internalColumn.getName().equals(name)) {
					return internalColumn;
				}
			}
		}
		return null;
	}

//	public int getNumberOfColumnDescriptions() {
//		return columnDescriptions.size();
//	}
	
	public Iterator<ColumnDescription> iterator() {
		return columnDescriptions.iterator();
	}
	
	public DataFormatSpecification getDataFormarSpec() {
		return dataFormarSpec;
	}

	protected void createHeadColumnDescriptions() {
		headColumnDescriptions = new TreeMap<String,ColumnDescription>();
		for (ColumnDescription column : columnDescriptions) {
			if (column.getCategory() == ColumnDescription.HEAD) {
				headColumnDescriptions.put(column.getName(), column);
			}
		}
	}
	
	public ColumnDescription getHeadColumnDescription() {
		if (headColumnDescriptions == null) {
			createHeadColumnDescriptions();
		}
		return headColumnDescriptions.get(headColumnDescriptions.firstKey());
	}
	
	public SortedMap<String,ColumnDescription> getHeadColumnDescriptions() {
		if (headColumnDescriptions == null) {
			createHeadColumnDescriptions();
		}
		return headColumnDescriptions;
	}
	
	protected void createDependencyEdgeLabelSymbolTables(SymbolTableHandler symbolTables) throws MaltChainedException {
		dependencyEdgeLabelSymbolTables = new TreeMap<String,SymbolTable>();
		for (ColumnDescription column : columnDescriptions) {
			if (column.getCategory() == ColumnDescription.DEPENDENCY_EDGE_LABEL) { 
				dependencyEdgeLabelSymbolTables.put(column.getName(), symbolTables.getSymbolTable(column.getName()));
			}
		}
	}
	
	public SortedMap<String,SymbolTable> getDependencyEdgeLabelSymbolTables(SymbolTableHandler symbolTables) throws MaltChainedException {
		if (dependencyEdgeLabelSymbolTables == null) {
			createDependencyEdgeLabelSymbolTables(symbolTables);
		}
		return dependencyEdgeLabelSymbolTables;
	}
	
	protected void createDependencyEdgeLabelColumnDescriptions() {
		dependencyEdgeLabelColumnDescriptions = new TreeMap<String,ColumnDescription>();
		for (ColumnDescription column : columnDescriptions) {
			if (column.getCategory() == ColumnDescription.DEPENDENCY_EDGE_LABEL) { 
				dependencyEdgeLabelColumnDescriptions.put(column.getName(), column);
			}
		}
	}
	
	public SortedMap<String,ColumnDescription> getDependencyEdgeLabelColumnDescriptions() {
		if (dependencyEdgeLabelColumnDescriptions == null) {
			createDependencyEdgeLabelColumnDescriptions();
		}
		return dependencyEdgeLabelColumnDescriptions;
	}


	
	protected void createPhraseStructureEdgeLabelSymbolTables(SymbolTableHandler symbolTables) throws MaltChainedException {
		phraseStructureEdgeLabelSymbolTables = new TreeMap<String, SymbolTable>();
		for (ColumnDescription column : columnDescriptions) {
			if (column.getCategory() == ColumnDescription.PHRASE_STRUCTURE_EDGE_LABEL) { 
				phraseStructureEdgeLabelSymbolTables.put(column.getName(), symbolTables.getSymbolTable(column.getName()));
			}
		}
	}
	
	public SortedMap<String,SymbolTable> getPhraseStructureEdgeLabelSymbolTables(SymbolTableHandler symbolTables) throws MaltChainedException {
		if (phraseStructureEdgeLabelSymbolTables == null) {
			createPhraseStructureEdgeLabelSymbolTables(symbolTables);
		}
		return phraseStructureEdgeLabelSymbolTables;
	}
	
	protected void createPhraseStructureEdgeLabelColumnDescriptions() {
		phraseStructureEdgeLabelColumnDescriptions = new TreeMap<String,ColumnDescription>();
		for (ColumnDescription column : columnDescriptions) {
			if (column.getCategory() == ColumnDescription.PHRASE_STRUCTURE_EDGE_LABEL) {
				phraseStructureEdgeLabelColumnDescriptions.put(column.getName(), column);
			}
		}
	}
	
	public SortedMap<String,ColumnDescription> getPhraseStructureEdgeLabelColumnDescriptions() {
		if (phraseStructureEdgeLabelColumnDescriptions == null) {
			createPhraseStructureEdgeLabelColumnDescriptions();
		}
		return phraseStructureEdgeLabelColumnDescriptions;
	}

	protected void createPhraseStructureNodeLabelSymbolTables(SymbolTableHandler symbolTables) throws MaltChainedException {
		phraseStructureNodeLabelSymbolTables = new TreeMap<String,SymbolTable>();
		for (ColumnDescription column : columnDescriptions) {
			if (column.getCategory() == ColumnDescription.PHRASE_STRUCTURE_NODE_LABEL) { 
				phraseStructureNodeLabelSymbolTables.put(column.getName(), symbolTables.getSymbolTable(column.getName()));
			}
		}
	}
	
	public SortedMap<String,SymbolTable> getPhraseStructureNodeLabelSymbolTables(SymbolTableHandler symbolTables) throws MaltChainedException {
		if (phraseStructureNodeLabelSymbolTables == null) {
			createPhraseStructureNodeLabelSymbolTables(symbolTables);
		}
		return phraseStructureNodeLabelSymbolTables;
	}
	
	protected void createPhraseStructureNodeLabelColumnDescriptions() {
		phraseStructureNodeLabelColumnDescriptions = new TreeMap<String,ColumnDescription>();
		for (ColumnDescription column : columnDescriptions) {
			if (column.getCategory() == ColumnDescription.PHRASE_STRUCTURE_NODE_LABEL) { 
				phraseStructureNodeLabelColumnDescriptions.put(column.getName(), column);
			}
		}
	}
	
	public SortedMap<String,ColumnDescription> getPhraseStructureNodeLabelColumnDescriptions() {
		if (phraseStructureNodeLabelColumnDescriptions == null) {
			createPhraseStructureNodeLabelColumnDescriptions();
		}
		return phraseStructureNodeLabelColumnDescriptions;
	}
	
	protected void createSecondaryEdgeLabelSymbolTables(SymbolTableHandler symbolTables) throws MaltChainedException {
		secondaryEdgeLabelSymbolTables = new TreeMap<String,SymbolTable>();
		for (ColumnDescription column : columnDescriptions) {
			if (column.getCategory() == ColumnDescription.PHRASE_STRUCTURE_EDGE_LABEL) { 
				secondaryEdgeLabelSymbolTables.put(column.getName(), symbolTables.getSymbolTable(column.getName()));
			}
		}
	}
	
	public SortedMap<String,SymbolTable> getSecondaryEdgeLabelSymbolTables(SymbolTableHandler symbolTables) throws MaltChainedException {
		if (secondaryEdgeLabelSymbolTables == null) {
			createSecondaryEdgeLabelSymbolTables(symbolTables);
		}
		return secondaryEdgeLabelSymbolTables;
	}
	
	protected void createSecondaryEdgeLabelColumnDescriptions() {
		secondaryEdgeLabelColumnDescriptions = new TreeMap<String,ColumnDescription>();
		for (ColumnDescription column : columnDescriptions) {
			if (column.getCategory() == ColumnDescription.PHRASE_STRUCTURE_EDGE_LABEL) {  
				secondaryEdgeLabelColumnDescriptions.put(column.getName(), column);
			}
		}
	}
	
	public SortedMap<String,ColumnDescription> getSecondaryEdgeLabelColumnDescriptions() {
		if (secondaryEdgeLabelColumnDescriptions == null) {
			createSecondaryEdgeLabelColumnDescriptions();
		}
		return secondaryEdgeLabelColumnDescriptions;
	}
	
	protected void createInputSymbolTables(SymbolTableHandler symbolTables) throws MaltChainedException {
		inputSymbolTables = new TreeMap<String,SymbolTable>();
		for (ColumnDescription column : columnDescriptions) {
			if (column.getCategory() == ColumnDescription.INPUT) { 
				inputSymbolTables.put(column.getName(), symbolTables.getSymbolTable(column.getName()));
			}
		}
	}
	
	public SortedMap<String,SymbolTable> getInputSymbolTables(SymbolTableHandler symbolTables) throws MaltChainedException {
		if (inputSymbolTables == null) {
			createInputSymbolTables(symbolTables);
		}
		return inputSymbolTables;
	}
	
	protected void createInputColumnDescriptions() {
		inputColumnDescriptions = new TreeMap<String,ColumnDescription>();
		for (ColumnDescription column : columnDescriptions) {
			if (column.getCategory() == ColumnDescription.INPUT) { 
				inputColumnDescriptions.put(column.getName(), column);
			}
		}
	}
	
	public SortedMap<String,ColumnDescription> getInputColumnDescriptions() {
		if (inputColumnDescriptions == null) {
			createInputColumnDescriptions();
		}
		return inputColumnDescriptions;
	}
	
	protected void createIgnoreColumnDescriptions() {
		ignoreColumnDescriptions = new TreeMap<String,ColumnDescription>();
		for (ColumnDescription column : columnDescriptions) {
			if (column.getCategory() == ColumnDescription.IGNORE) { 
//			if (column.getType() == ColumnDescription.IGNORE) { 
				ignoreColumnDescriptions.put(column.getName(), column);
			}
		}
	}
	
	public SortedMap<String,ColumnDescription> getIgnoreColumnDescriptions() {
		if (ignoreColumnDescriptions == null) {
			createIgnoreColumnDescriptions();
		}
		return ignoreColumnDescriptions;
	}
	
	public SortedSet<ColumnDescription> getHeadColumnDescriptionSet() {
		if (headColumnDescriptionSet == null) {
			headColumnDescriptionSet = new TreeSet<ColumnDescription>();
			for (ColumnDescription column : columnDescriptions) {
				if (column.getCategory() == ColumnDescription.HEAD) { 
					headColumnDescriptionSet.add(column);
				}
			}
		}
		return headColumnDescriptionSet;
	}
	
	public SortedSet<ColumnDescription> getDependencyEdgeLabelColumnDescriptionSet() {
		if (dependencyEdgeLabelColumnDescriptionSet == null) {
			dependencyEdgeLabelColumnDescriptionSet = new TreeSet<ColumnDescription>();
			for (ColumnDescription column : columnDescriptions) {
				if (column.getCategory() == ColumnDescription.DEPENDENCY_EDGE_LABEL) { 
					dependencyEdgeLabelColumnDescriptionSet.add(column);
				}
			}
		}
		return dependencyEdgeLabelColumnDescriptionSet;
	}
	
	public SortedSet<ColumnDescription> getPhraseStructureEdgeLabelColumnDescriptionSet() {
		if (phraseStructureEdgeLabelColumnDescriptionSet == null) {
			phraseStructureEdgeLabelColumnDescriptionSet = new TreeSet<ColumnDescription>();
			for (ColumnDescription column : columnDescriptions) {
				if (column.getCategory() == ColumnDescription.PHRASE_STRUCTURE_EDGE_LABEL) {  
					phraseStructureEdgeLabelColumnDescriptionSet.add(column);
				}
			}
		}
		return phraseStructureEdgeLabelColumnDescriptionSet;
	}
	
	public SortedSet<ColumnDescription> getPhraseStructureNodeLabelColumnDescriptionSet() {
		if (phraseStructureNodeLabelColumnDescriptionSet == null) {
			phraseStructureNodeLabelColumnDescriptionSet = new TreeSet<ColumnDescription>();
			for (ColumnDescription column : columnDescriptions) {
				if (column.getCategory() == ColumnDescription.PHRASE_STRUCTURE_NODE_LABEL) { 
					phraseStructureNodeLabelColumnDescriptionSet.add(column);
				}
			}
		}
		return phraseStructureNodeLabelColumnDescriptionSet;
	}
	
	public SortedSet<ColumnDescription> getSecondaryEdgeLabelColumnDescriptionSet() {
		if (secondaryEdgeLabelColumnDescriptionSet == null) {
			secondaryEdgeLabelColumnDescriptionSet = new TreeSet<ColumnDescription>();
			for (ColumnDescription column : columnDescriptions) {
				if (column.getCategory() == ColumnDescription.SECONDARY_EDGE_LABEL) { 
					secondaryEdgeLabelColumnDescriptionSet.add(column);
				}
			}
		}
		return secondaryEdgeLabelColumnDescriptionSet;
	}
	
	public SortedSet<ColumnDescription> getInputColumnDescriptionSet() {
		if (inputColumnDescriptionSet == null) {
			inputColumnDescriptionSet = new TreeSet<ColumnDescription>();
			for (ColumnDescription column : columnDescriptions) {
				if (column.getCategory() == ColumnDescription.INPUT) { 
					inputColumnDescriptionSet.add(column);
				}
			}
		}
		return inputColumnDescriptionSet;
	}
	
	public SortedSet<ColumnDescription> getIgnoreColumnDescriptionSet() {
		if (ignoreColumnDescriptionSet == null) {
			ignoreColumnDescriptionSet = new TreeSet<ColumnDescription>();
			for (ColumnDescription column : columnDescriptions) {
				if (column.getCategory() == ColumnDescription.IGNORE) { 
					ignoreColumnDescriptionSet.add(column);
				}
			}
		}
		return ignoreColumnDescriptionSet;
	}
	
//	public SymbolTableHandler getSymbolTables() {
//		return symbolTables;
//	}
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (ColumnDescription column : columnDescriptions) {
			sb.append(column);
			sb.append('\n');
		}
		return sb.toString();
	}
}
