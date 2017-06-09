package org.maltparser.core.syntaxgraph.feature;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.function.AddressFunction;
import org.maltparser.core.feature.value.AddressValue;
import org.maltparser.core.io.dataformat.ColumnDescription;
import org.maltparser.core.io.dataformat.DataFormatInstance;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.symbol.nullvalue.NullValues.NullValueId;
import org.maltparser.core.syntaxgraph.SyntaxGraphException;
import org.maltparser.core.syntaxgraph.node.DependencyNode;

/**
*
*
* @author Johan Hall
*/
public final class InputColumnFeature extends ColumnFeature {
	public final static Class<?>[] paramTypes = { java.lang.String.class, org.maltparser.core.feature.function.AddressFunction.class };
	private final DataFormatInstance dataFormatInstance;
	private final SymbolTableHandler tableHandler;
	private AddressFunction addressFunction;
	
	public InputColumnFeature(DataFormatInstance dataFormatInstance, SymbolTableHandler tableHandler) throws MaltChainedException {
		super();
		this.dataFormatInstance = dataFormatInstance;
		this.tableHandler = tableHandler;
	}

	public void initialize(Object[] arguments) throws MaltChainedException {
		if (arguments.length != 2) {
			throw new SyntaxGraphException("Could not initialize InputColumnFeature: number of arguments are not correct. ");
		}
		if (!(arguments[0] instanceof String)) {
			throw new SyntaxGraphException("Could not initialize InputColumnFeature: the first argument is not a string. ");
		}
		if (!(arguments[1] instanceof AddressFunction)) {
			throw new SyntaxGraphException("Could not initialize InputColumnFeature: the second argument is not an address function. ");
		}
		ColumnDescription column = dataFormatInstance.getColumnDescriptionByName((String)arguments[0]);
		if (column == null) {
			throw new SyntaxGraphException("Could not initialize InputColumnFeature: the input column type '"+(String)arguments[0]+"' could not be found in the data format specification. ' ");
		}
		setColumn(column);
		setSymbolTable(tableHandler.getSymbolTable(column.getName()));
		setAddressFunction((AddressFunction)arguments[1]);
	}
	
	public Class<?>[] getParameterTypes() {
		return paramTypes; 
	}
	
	public void update() throws MaltChainedException {
		final AddressValue a = addressFunction.getAddressValue();
		
		if (a.getAddress() == null) { 
			featureValue.update(symbolTable.getNullValueCode(NullValueId.NO_NODE), 
					symbolTable.getNullValueSymbol(NullValueId.NO_NODE), true, 1);
		} else {
			final DependencyNode node = (DependencyNode)a.getAddress();
			
			if (!node.isRoot()) { 
				int indexCode = node.getLabelCode(symbolTable);
				if (column.getType() == ColumnDescription.STRING) {
					featureValue.update(indexCode, symbolTable.getSymbolCodeToString(indexCode), false, 1);
				} else {
					castFeatureValue(symbolTable.getSymbolCodeToString(indexCode));
				}
			} else { 
				featureValue.update(symbolTable.getNullValueCode(NullValueId.ROOT_NODE), 
						symbolTable.getNullValueSymbol(NullValueId.ROOT_NODE), true, 1);
			}
		}
	}
	
	public AddressFunction getAddressFunction() {
		return addressFunction;
	}

	public void setAddressFunction(AddressFunction addressFunction) {
		this.addressFunction = addressFunction;
	}

	public DataFormatInstance getDataFormatInstance() {
		return dataFormatInstance;
	}
	
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return obj.toString().equals(toString());
	}
	
	public int hashCode() {
		return 217 + (null == toString() ? 0 : toString().hashCode());
	}
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("InputColumn(");
		sb.append(super.toString());
		sb.append(", ");
		sb.append(addressFunction.toString());
		sb.append(")");
		return sb.toString();
	}
}
