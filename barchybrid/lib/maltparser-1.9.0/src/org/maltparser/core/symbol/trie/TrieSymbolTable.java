package org.maltparser.core.symbol.trie;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.SymbolException;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.symbol.nullvalue.InputNullValues;
import org.maltparser.core.symbol.nullvalue.NullValues;
import org.maltparser.core.symbol.nullvalue.OutputNullValues;
import org.maltparser.core.symbol.nullvalue.NullValues.NullValueId;
/**

@author Johan Hall
@since 1.0
*/
public class TrieSymbolTable implements SymbolTable {
	private final String name;
	private final Trie trie;
	private final SortedMap<Integer, TrieNode> codeTable;
	private int category;
	private final NullValues nullValues;
	private int valueCounter;
    /** Cache the hash code for the symbol table */
    private int cachedHash;
    
    
	public TrieSymbolTable(String _name, Trie _trie, int _category, String nullValueStrategy) throws MaltChainedException { 
		this.name = _name;
		this.trie = _trie;
		this.category = _category;
		codeTable = new TreeMap<Integer, TrieNode>();
		if (this.category != SymbolTable.OUTPUT) {
			nullValues = new OutputNullValues(nullValueStrategy, this);
		} else {
			nullValues = new InputNullValues(nullValueStrategy, this);
		}
		valueCounter = nullValues.getNextCode();
	}
	
	public TrieSymbolTable(String _name, Trie trie) { 
		this.name = _name;
		this.trie = trie;
		codeTable = new TreeMap<Integer, TrieNode>();
		nullValues = new InputNullValues("one", this);
		valueCounter = 1;
	}
	
	public int addSymbol(String symbol) throws MaltChainedException {
		if (nullValues == null || !nullValues.isNullValue(symbol)) {
			if (symbol == null || symbol.length() == 0) {
				throw new SymbolException("Symbol table error: empty string cannot be added to the symbol table");
			}
			
			final TrieNode node = trie.addValue(symbol, this, -1);
			final int code = node.getEntry(this); 
			if (!codeTable.containsKey(code)) {
				codeTable.put(code, node);
			}
			return code;
		} else {
			return nullValues.symbolToCode(symbol);
		}
	}
	
	public String getSymbolCodeToString(int code) throws MaltChainedException {
		if (code >= 0) {
			if (nullValues == null || !nullValues.isNullValue(code)) {
				TrieNode node = codeTable.get(code);
				if (node != null) {
					return trie.getValue(node, this);
				} else {
					return null;
				}
			} else {
				return nullValues.codeToSymbol(code);
			}
		} else {
			throw new SymbolException("The symbol code '"+code+"' cannot be found in the symbol table. ");
		}
	}
	
	public int getSymbolStringToCode(String symbol) throws MaltChainedException {
		if (symbol != null) {
			if (nullValues == null || !nullValues.isNullValue(symbol)) {
				final Integer entry = trie.getEntry(symbol, this);
				if (entry != null) {
					return entry.intValue(); 
				} else {
					return -1;
				}
			} else {
				return nullValues.symbolToCode(symbol);
			}
		} else {
			throw new SymbolException("The symbol code '"+symbol+"' cannot be found in the symbol table. ");
		}
	}

	public double getSymbolStringToValue(String symbol) throws MaltChainedException {
		if (symbol == null) {
			throw new SymbolException("The symbol code '"+symbol+"' cannot be found in the symbol table. ");
		}

		return 1.0; 	
	}
	public void clearTmpStorage() {

	}
	
	public String getNullValueStrategy() {
		if (nullValues == null) {
			return null;
		}
		return nullValues.getNullValueStrategy();
	}
	
	
	public int getCategory() {
		return category;
	}
	
	public void saveHeader(BufferedWriter out) throws MaltChainedException  {
		try {
			out.append('\t');
			out.append(getName());
			out.append('\t');
			out.append(Integer.toString(getCategory()));
			out.append('\t');
			out.append(Integer.toString(SymbolTable.STRING));
			out.append('\t');
			out.append(getNullValueStrategy());
			out.append('\n');
		} catch (IOException e) {
			throw new SymbolException("Could not save the symbol table. ", e);
		}
	}
	
	public int size() {
		return codeTable.size();
	}
	
	
	public void save(BufferedWriter out) throws MaltChainedException  {
		try {
			out.write(name);
			out.write('\n');
			for (Integer code : codeTable.keySet()) {
				out.write(code+"");
				out.write('\t');
				out.write(trie.getValue(codeTable.get(code), this));
				out.write('\n');
			}
			out.write('\n');
		} catch (IOException e) {
			throw new SymbolException("Could not save the symbol table. ", e);
		}
	}
	
	public void load(BufferedReader in) throws MaltChainedException {
		int max = 0;
		int index = 0;
		String fileLine;
		try {
			while ((fileLine = in.readLine()) != null) {
				if (fileLine.length() == 0 || (index = fileLine.indexOf('\t')) == -1) {
					setValueCounter(max+1);
					break;
				}
				int code = Integer.parseInt(fileLine.substring(0,index));
				final String str = fileLine.substring(index+1);
				final TrieNode node = trie.addValue(str, this, code);
				codeTable.put(node.getEntry(this), node); 
				if (max < code) {
					max = code;
				}
			}
		} catch (NumberFormatException e) {
			throw new SymbolException("The symbol table file (.sym) contains a non-integer value in the first column. ", e);
		} catch (IOException e) {
			throw new SymbolException("Could not load the symbol table. ", e);
		}
	}
	
	public String getName() {
		return name;
	}

	public int getValueCounter() {
		return valueCounter;
	}

	private void setValueCounter(int valueCounter) {
		this.valueCounter = valueCounter;
	}
	
	protected void updateValueCounter(int code) {
		if (code > valueCounter) {
			valueCounter = code;
		}
	}
	
	protected int increaseValueCounter() {
		return valueCounter++;
	}
	
	public int getNullValueCode(NullValueId nullValueIdentifier) throws MaltChainedException {
		if (nullValues == null) {
			throw new SymbolException("The symbol table does not have any null-values. ");
		}
		return nullValues.nullvalueToCode(nullValueIdentifier);
	}
	
	public String getNullValueSymbol(NullValueId nullValueIdentifier) throws MaltChainedException {
		if (nullValues == null) {
			throw new SymbolException("The symbol table does not have any null-values. ");
		}
		return nullValues.nullvalueToSymbol(nullValueIdentifier);
	}
	
	public boolean isNullValue(String symbol) throws MaltChainedException {
		if (nullValues != null) {
			return nullValues.isNullValue(symbol);
		} 
		return false;
	}
	
	public boolean isNullValue(int code) throws MaltChainedException {
		if (nullValues != null) {
			return nullValues.isNullValue(code);
		} 
		return false;
	}
	
//	public void copy(SymbolTable fromTable) throws MaltChainedException {
//		final SortedMap<Integer, TrieNode> fromCodeTable =  ((TrieSymbolTable)fromTable).getCodeTable();
//		int max = getValueCounter()-1;
//		for (Integer code : fromCodeTable.keySet()) {
//			final String str = trie.getValue(fromCodeTable.get(code), this);
//			final TrieNode node = trie.addValue(str, this, code);
//			codeTable.put(node.getEntry(this), node); //.getCode(), node);
//			if (max < code) {
//				max = code;
//			}
//		}
//		setValueCounter(max+1);
//	}

	public SortedMap<Integer, TrieNode> getCodeTable() {
		return codeTable;
	}
	
	public Set<Integer> getCodes() {
		return codeTable.keySet();
	}
	
	protected Trie getTrie() {
		return trie;
	}
	
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final TrieSymbolTable other = (TrieSymbolTable)obj;
		return ((name == null) ? other.name == null : name.equals(other.name));
	}

	public int hashCode() {
		if (cachedHash == 0) {
			cachedHash = 217 + (null == name ? 0 : name.hashCode());
		}
		return cachedHash;
	}
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append(' ');
		sb.append(valueCounter);
		return sb.toString();
	}
}
