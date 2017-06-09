package org.maltparser.core.symbol.hash;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.helper.HashMap;
import org.maltparser.core.symbol.SymbolException;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.symbol.nullvalue.InputNullValues;
import org.maltparser.core.symbol.nullvalue.NullValues;
import org.maltparser.core.symbol.nullvalue.OutputNullValues;
import org.maltparser.core.symbol.nullvalue.NullValues.NullValueId;


public final class HashSymbolTable implements SymbolTable {
	private final String name;
	private final Map<String, Integer> symbolCodeMap;
	private final Map<Integer, String> codeSymbolMap;
	private final Map<String, Double> symbolValueMap;
	private final NullValues nullValues;
	private final int category;
	private final int type;
	private int valueCounter;
	
	public HashSymbolTable(String _name, int _category, int _type, String nullValueStrategy) throws MaltChainedException {
		this.name = _name;
		this.category = _category;
		this.type = _type;
		this.symbolCodeMap = new HashMap<String, Integer>();
		this.codeSymbolMap = new HashMap<Integer, String>();
		this.symbolValueMap = new HashMap<String, Double>();
		if (this.category == SymbolTable.OUTPUT) {
			this.nullValues = new OutputNullValues(nullValueStrategy, this);
		} else {
			this.nullValues = new InputNullValues(nullValueStrategy, this);
		}
		this.valueCounter = nullValues.getNextCode();
	}
	
	public HashSymbolTable(String _name) { 
		this.name = _name;
		this.category = SymbolTable.NA;
		this.type = SymbolTable.STRING;
		this.symbolCodeMap = new HashMap<String, Integer>();
		this.codeSymbolMap = new HashMap<Integer, String>();
		this.symbolValueMap = new HashMap<String, Double>();
		this.nullValues = new InputNullValues("one", this);
		this.valueCounter = 1;
	}
	
	public int addSymbol(String symbol) throws MaltChainedException {
		if (nullValues == null || !nullValues.isNullValue(symbol)) {
			if (symbol == null || symbol.length() == 0) {
				throw new SymbolException("Symbol table error: empty string cannot be added to the symbol table");
			}

			if (this.type == SymbolTable.REAL) {
				addSymbolValue(symbol);
			}
			if (!symbolCodeMap.containsKey(symbol)) {
				int code = valueCounter;
				symbolCodeMap.put(symbol, code);
				codeSymbolMap.put(code, symbol);
				valueCounter++;
				return code;
			} else {
				return symbolCodeMap.get(symbol);
			}
		} else {
			return nullValues.symbolToCode(symbol);
		}
	}
	
	public double addSymbolValue(String symbol) throws MaltChainedException {
		if (!symbolValueMap.containsKey(symbol)) {
			Double value = Double.valueOf(symbol);
			symbolValueMap.put(symbol, value);
			return value;
		} else {
			return symbolValueMap.get(symbol);
		}
	}
	
	public String getSymbolCodeToString(int code) throws MaltChainedException {
		if (code >= 0) {
			if (nullValues == null || !nullValues.isNullValue(code)) {
				return codeSymbolMap.get(code);
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
				Integer value = symbolCodeMap.get(symbol);
				return (value != null) ? value.intValue() : -1; 
			} else {
				return nullValues.symbolToCode(symbol);
			}
		} else {
			throw new SymbolException("The symbol code '"+symbol+"' cannot be found in the symbol table. ");
		}
	}
	
	public double getSymbolStringToValue(String symbol) throws MaltChainedException {
		if (symbol != null) {
			if (type == SymbolTable.REAL && nullValues == null || !nullValues.isNullValue(symbol)) {
				Double value = symbolValueMap.get(symbol);
				return (value != null) ? value.doubleValue() : Double.parseDouble(symbol); 
			} else {
				return 1.0;
			}
		} else {
			throw new SymbolException("The symbol code '"+symbol+"' cannot be found in the symbol table. ");
		}
	}
	
	public void saveHeader(BufferedWriter out) throws MaltChainedException  {
		try {
			out.append('\t');
			out.append(getName());
			out.append('\t');
			out.append(Integer.toString(getCategory()));
			out.append('\t');
			out.append(Integer.toString(getType()));
			out.append('\t');
			out.append(getNullValueStrategy());
			out.append('\n');
		} catch (IOException e) {
			throw new SymbolException("Could not save the symbol table. ", e);
		}
	}
	
	public int getCategory() {
		return category;
	}
	
	public int getType() {
		return type;
	}
	
	public String getNullValueStrategy() {
		if (nullValues == null) {
			return null;
		}
		return nullValues.getNullValueStrategy();
	}
	
	public int size() {
		return symbolCodeMap.size();
	}
	
	public void save(BufferedWriter out) throws MaltChainedException  {
		try {
			out.write(name);
			out.write('\n');
			if (this.type != SymbolTable.REAL) {
				// TODO sort codes before writing due to change from TreeMap to HashMap
				for (Integer code : codeSymbolMap.keySet()) {
					out.write(Integer.toString(code));
					out.write('\t');
					out.write(codeSymbolMap.get(code));
					out.write('\n');
				}
			} else {
				for (String symbol : symbolValueMap.keySet()) {
					out.write(1);
					out.write('\t');
					out.write(symbol);
					out.write('\n');
				}
			}
			out.write('\n');
		} catch (IOException e) {
			throw new SymbolException("Could not save the symbol table. ", e);
		}
	}

	public void load(BufferedReader in) throws MaltChainedException {	
		int max = 0;
		String fileLine;
		try {
			while ((fileLine = in.readLine()) != null) {
				int index;
				if (fileLine.length() == 0 || (index = fileLine.indexOf('\t')) == -1) {
					valueCounter = max+1;
					break;
				}
				
				if (this.type != SymbolTable.REAL) {
					int code;
				    try {
				    	code = Integer.parseInt(fileLine.substring(0,index));
					} catch (NumberFormatException e) {
						throw new SymbolException("The symbol table file (.sym) contains a non-integer value in the first column. ", e);
					}
				    final String symbol = fileLine.substring(index+1);
					symbolCodeMap.put(symbol, code);
					codeSymbolMap.put(code, symbol);
							
					if (max < code) {
						max = code;
					}
				} else {
				    final String symbol = fileLine.substring(index+1);
					symbolValueMap.put(symbol, Double.parseDouble(symbol));
					
					max = 1;
				}
			}
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
	
	public Set<Integer> getCodes() {
		return codeSymbolMap.keySet();
	}
	
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final HashSymbolTable other = (HashSymbolTable)obj;
		return ((name == null) ? other.name == null : name.equals(other.name));
	}

	public int hashCode() {
		return 217 + (null == name ? 0 : name.hashCode());
	}
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append(' ');
		sb.append(valueCounter);
		return sb.toString();
	}
}
