package org.maltparser.core.symbol.parse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.Map;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.helper.HashMap;
import org.maltparser.core.symbol.SymbolException;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.symbol.nullvalue.NullValues.NullValueId;


public class ParseSymbolTable implements SymbolTable {
	private final String name;
	private final SymbolTable parentSymbolTable;
	private final int type;    
	/** Special treatment during parsing */
	private final Map<String, Integer> symbolCodeMap;
	private final Map<Integer, String> codeSymbolMap;
	private final Map<String, Double> symbolValueMap;
	private int valueCounter;
    
	public ParseSymbolTable(String _name, int _category, int _type, String nullValueStrategy, SymbolTableHandler parentSymbolTableHandler) throws MaltChainedException {
		this.name = _name;
		this.type = _type;
		this.parentSymbolTable = parentSymbolTableHandler.addSymbolTable(name, _category, _type, nullValueStrategy);
		this.symbolCodeMap = new HashMap<String, Integer>();
		this.codeSymbolMap = new HashMap<Integer, String>();
		this.symbolValueMap = new HashMap<String, Double>();
		this.valueCounter = -1;
	}
	
	public ParseSymbolTable(String _name, SymbolTable parentTable, SymbolTableHandler parentSymbolTableHandler) throws MaltChainedException {
		this.name = _name;
		this.type = SymbolTable.STRING;
		this.parentSymbolTable = parentSymbolTableHandler.addSymbolTable(name, parentTable);
		this.symbolCodeMap = new HashMap<String, Integer>();
		this.codeSymbolMap = new HashMap<Integer, String>();
		this.symbolValueMap = new HashMap<String, Double>();
		this.valueCounter = -1;
	}
	
	public ParseSymbolTable(String name, SymbolTableHandler parentSymbolTableHandler) throws MaltChainedException {
		this.name = name;
		this.type = SymbolTable.STRING;
		this.parentSymbolTable = parentSymbolTableHandler.addSymbolTable(name);
		this.symbolCodeMap = new HashMap<String, Integer>();
		this.codeSymbolMap = new HashMap<Integer, String>();
		this.symbolValueMap = new HashMap<String, Double>();
		this.valueCounter = -1;
	}
	
	public int addSymbol(String symbol) throws MaltChainedException {
		if (!parentSymbolTable.isNullValue(symbol)) {
			if (symbol == null || symbol.length() == 0) {
				throw new SymbolException("Symbol table error: empty string cannot be added to the symbol table");
			}

			int code = parentSymbolTable.getSymbolStringToCode(symbol); 
			if (code > -1) {
				return code;
			}
			if (this.type == SymbolTable.REAL) {
				addSymbolValue(symbol);
			}
			if (!symbolCodeMap.containsKey(symbol)) {
//				System.out.println("!symbolCodeMap.containsKey(symbol) : " + this.getName() + ": " + symbol.toString());
				if (valueCounter == -1) {
					valueCounter = parentSymbolTable.getValueCounter() + 1;
				} else {
					valueCounter++;
				}
				symbolCodeMap.put(symbol, valueCounter);
				codeSymbolMap.put(valueCounter, symbol);
				return valueCounter;
			} else {
				return symbolCodeMap.get(symbol);
			}
		} else {
			return parentSymbolTable.getSymbolStringToCode(symbol);
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
		if (code < 0) {
			throw new SymbolException("The symbol code '"+code+"' cannot be found in the symbol table. ");
		}
		String symbol = parentSymbolTable.getSymbolCodeToString(code); 
		if (symbol != null) {
			return symbol;
		} else {
			return codeSymbolMap.get(code);
		}
	}
	
	public int getSymbolStringToCode(String symbol) throws MaltChainedException {
		if (symbol == null) {
			throw new SymbolException("The symbol code '"+symbol+"' cannot be found in the symbol table. ");
		}

		int code = parentSymbolTable.getSymbolStringToCode(symbol); 
		if (code > -1) {
			return code;
		}

		Integer item = symbolCodeMap.get(symbol);
		if (item == null) {
			throw new SymbolException("Could not find the symbol '"+symbol+"' in the symbol table. "); 
		} 
		return item.intValue();
	}

	public double getSymbolStringToValue(String symbol) throws MaltChainedException {
		if (symbol == null) {
			throw new SymbolException("The symbol code '"+symbol+"' cannot be found in the symbol table. ");
		}
		double value = parentSymbolTable.getSymbolStringToValue(symbol); 
		if (value != Double.NaN) {
			return value;
		}
		
		Double item = symbolValueMap.get(symbol);
		if (item == null) {
			throw new SymbolException("Could not find the symbol '"+symbol+"' in the symbol table. "); 
		} 
		return item.doubleValue();	
	}
	
	public void clearTmpStorage() {
		symbolCodeMap.clear();
		codeSymbolMap.clear();
		symbolValueMap.clear();
		valueCounter = -1;
	}

	public int size() {
		return parentSymbolTable.size();
	}
	
	public void save(BufferedWriter out) throws MaltChainedException  {
		parentSymbolTable.save(out);
	}

	
	public void load(BufferedReader in) throws MaltChainedException {
		parentSymbolTable.load(in);
	}
	
	public String getName() {
		return name;
	}

	public int getValueCounter() {
		return parentSymbolTable.getValueCounter();
	}

	
	public int getNullValueCode(NullValueId nullValueIdentifier) throws MaltChainedException {
		return parentSymbolTable.getNullValueCode(nullValueIdentifier);
	}
	
	public String getNullValueSymbol(NullValueId nullValueIdentifier) throws MaltChainedException {
		return parentSymbolTable.getNullValueSymbol(nullValueIdentifier);
	}
	
	public boolean isNullValue(String symbol) throws MaltChainedException {
		return parentSymbolTable.isNullValue(symbol);
	}
	
	public boolean isNullValue(int code) throws MaltChainedException {
		return parentSymbolTable.isNullValue(code);
	}
	
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ParseSymbolTable other = (ParseSymbolTable)obj;
		return ((name == null) ? other.name == null : name.equals(other.name));
	}

	public int hashCode() {
		return 217 + (null == name ? 0 : name.hashCode());
	}
	
	public String toString() {
		return parentSymbolTable.toString();
	}
}
