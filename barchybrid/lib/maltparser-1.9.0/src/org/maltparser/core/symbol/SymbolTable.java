package org.maltparser.core.symbol;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.nullvalue.NullValues.NullValueId;

public interface SymbolTable extends Table {
	// Categories
	public static final int NA = -1;
	public static final int INPUT = 1;
	public static final int OUTPUT = 3;
	public static final String[] categories = { "", "INPUT", "", "OUTPUT", };
	
	// Types
	public static final int STRING = 1;
	public static final int INTEGER = 2;
	public static final int BOOLEAN = 3;
	public static final int REAL = 4;
	public static final String[] types = { "", "STRING", "INTEGER", "BOOLEAN", "REAL" };
	
	
	public void save(BufferedWriter out) throws MaltChainedException;
	public void load(BufferedReader in) throws MaltChainedException;
	public int getValueCounter();
	public int getNullValueCode(NullValueId nullValueIdentifier) throws MaltChainedException;
	public String getNullValueSymbol(NullValueId nullValueIdentifier) throws MaltChainedException;
	public boolean isNullValue(String value) throws MaltChainedException;
	public boolean isNullValue(int code) throws MaltChainedException;
}
