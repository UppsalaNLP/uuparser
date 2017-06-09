package org.maltparser.core.symbol.parse;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.helper.HashMap;
import org.maltparser.core.symbol.SymbolException;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.symbol.SymbolTableHandler;

public class ParseSymbolTableHandler implements SymbolTableHandler {
	private final SymbolTableHandler parentSymbolTableHandler;
	private final HashMap<String,  ParseSymbolTable> symbolTables;
	
	public ParseSymbolTableHandler(SymbolTableHandler parentSymbolTableHandler) throws MaltChainedException {
		this.parentSymbolTableHandler = parentSymbolTableHandler;
		this.symbolTables = new HashMap<String, ParseSymbolTable>();
		for (String tableName : parentSymbolTableHandler.getSymbolTableNames()) {
			addSymbolTable(tableName);
		}
	}

	public SymbolTable addSymbolTable(String tableName) throws MaltChainedException {
		ParseSymbolTable symbolTable = symbolTables.get(tableName);
		if (symbolTable == null) {
			symbolTable = new ParseSymbolTable(tableName, parentSymbolTableHandler);
			symbolTables.put(tableName, symbolTable);
		}
		return symbolTable;
	}
	
	public SymbolTable addSymbolTable(String tableName, SymbolTable parentTable) throws MaltChainedException {
		ParseSymbolTable symbolTable = symbolTables.get(tableName);
		if (symbolTable == null) {
			symbolTable = new ParseSymbolTable(tableName, parentTable, parentSymbolTableHandler);
			symbolTables.put(tableName, symbolTable);
		}
		return symbolTable;
	}
	
	public SymbolTable addSymbolTable(String tableName, int columnCategory, int columnType, String nullValueStrategy) throws MaltChainedException {
		ParseSymbolTable symbolTable = symbolTables.get(tableName);
		if (symbolTable == null) {
			symbolTable = new ParseSymbolTable(tableName, columnCategory, columnType, nullValueStrategy, parentSymbolTableHandler);
			symbolTables.put(tableName, symbolTable);
		}
		return symbolTable;
	}
	
	public SymbolTable getSymbolTable(String tableName) {
		return symbolTables.get(tableName);
	}
	
	public Set<String> getSymbolTableNames() {
		return symbolTables.keySet();
	}
	
	public void cleanUp() {
		for (ParseSymbolTable table : symbolTables.values()) {
			table.clearTmpStorage();
		}
	}
	
	public void save(OutputStreamWriter osw) throws MaltChainedException  {
		parentSymbolTableHandler.save(osw);	
	}
	
	public void save(String fileName, String charSet) throws MaltChainedException  {
		parentSymbolTableHandler.save(fileName, charSet);
	}
	
	public void loadHeader(BufferedReader bin) throws MaltChainedException {
		String fileLine = "";
		Pattern tabPattern = Pattern.compile("\t");
		try {
			while ((fileLine = bin.readLine()) != null) {
				if (fileLine.length() == 0 || fileLine.charAt(0) != '\t') {
					break;
				}
				String items[];
				try {
					items = tabPattern.split(fileLine.substring(1));
				} catch (PatternSyntaxException e) {
					throw new SymbolException("The header line of the symbol table  '"+fileLine.substring(1)+"' could not split into atomic parts. ", e);
				}
				if (items.length == 4)
					addSymbolTable(items[0], Integer.parseInt(items[1]), Integer.parseInt(items[2]), items[3]);
				else if (items.length == 3) 
					addSymbolTable(items[0], Integer.parseInt(items[1]), SymbolTable.STRING, items[2]);
				else
					throw new SymbolException("The header line of the symbol table  '"+fileLine.substring(1)+"' must contain three or four columns. ");

			}
		} catch (NumberFormatException e) {
			throw new SymbolException("The symbol table file (.sym) contains a non-integer value in the header. ", e);
		} catch (IOException e) {
			throw new SymbolException("Could not load the symbol table. ", e);
		}
	}
	
	public void load(InputStreamReader isr) throws MaltChainedException  {
		try {
			BufferedReader bin = new BufferedReader(isr);
			String fileLine;
			SymbolTable table = null;
			bin.mark(2);
			if (bin.read() == '\t') {
				bin.reset();
				loadHeader(bin);
			} else {
				bin.reset();
			}
			while ((fileLine = bin.readLine()) != null) {
				if (fileLine.length() > 0) {
					table = addSymbolTable(fileLine);
					table.load(bin);
				}
			}
			bin.close();
		} catch (IOException e) {
			throw new SymbolException("Could not load the symbol tables. ", e);
		}
	}
	
	public void load(String fileName, String charSet) throws MaltChainedException  {
		try {
			load(new InputStreamReader(new FileInputStream(fileName), charSet));
		} catch (FileNotFoundException e) {
			throw new SymbolException("The symbol table file '"+fileName+"' cannot be found. ", e);
		} catch (UnsupportedEncodingException e) {
			throw new SymbolException("The char set '"+charSet+"' is not supported. ", e);
		}
	}
	
	public SymbolTable loadTagset(String fileName, String tableName, String charSet, int columnCategory, int columnType, String nullValueStrategy) throws MaltChainedException {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), charSet));
			String fileLine;
			SymbolTable table = addSymbolTable(tableName, columnCategory, columnType, nullValueStrategy);

			while ((fileLine = br.readLine()) != null) {
				table.addSymbol(fileLine.trim());
			}
			return table;
		} catch (FileNotFoundException e) {
			throw new SymbolException("The tagset file '"+fileName+"' cannot be found. ", e);
		} catch (UnsupportedEncodingException e) {
			throw new SymbolException("The char set '"+charSet+"' is not supported. ", e);
		} catch (IOException e) {
			throw new SymbolException("The tagset file '"+fileName+"' cannot be loaded. ", e);
		}
	}
	
//	public String printSymbolTables() throws MaltChainedException  {
//		return parentSymbolTableHandler.printSymbolTables();
//	}
}
