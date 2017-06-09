package org.maltparser.parser.algorithm.nivre;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.function.AddressFunction;
import org.maltparser.core.feature.value.AddressValue;
import org.maltparser.parser.AlgoritmInterface;
import org.maltparser.parser.ParsingException;

/**
*
* @author Johan Hall
**/
public final class NivreAddressFunction extends AddressFunction {
	public final static Class<?>[] paramTypes = { java.lang.Integer.class };
	public enum NivreSubFunction {
		STACK, INPUT
	};
	private final String subFunctionName;
	private final NivreSubFunction subFunction;
	private final AlgoritmInterface parsingAlgorithm;
	private int index;
	
	public NivreAddressFunction(String _subFunctionName, AlgoritmInterface _parsingAlgorithm) {
		super();
		this.subFunctionName = _subFunctionName;
		this.subFunction = NivreSubFunction.valueOf(subFunctionName.toUpperCase());
		this.parsingAlgorithm = _parsingAlgorithm;
	}
	
	public void initialize(Object[] arguments) throws MaltChainedException {
		if (arguments.length != 1) {
			throw new ParsingException("Could not initialize "+this.getClass().getName()+": number of arguments are not correct. ");
		}
		if (!(arguments[0] instanceof Integer)) {
			throw new ParsingException("Could not initialize "+this.getClass().getName()+": the first argument is not an integer. ");
		}
		
		setIndex(((Integer)arguments[0]).intValue());
	}
	
	public Class<?>[] getParameterTypes() {
		return paramTypes; 
	}
	
	public void update() throws MaltChainedException {
		update((NivreConfig)parsingAlgorithm.getCurrentParserConfiguration());
	}
	
	public void update(Object[] arguments) throws MaltChainedException {
//		if (arguments.length != 1 || !(arguments[0] instanceof NivreConfig)) {
//			throw new ParsingException("Arguments to the Nivre address function is not correct. ");
//		}
//		update((NivreConfig)arguments[0]);
		if (subFunction == NivreSubFunction.STACK) {
			address.setAddress(((NivreConfig)arguments[0]).getStackNode(index));
		} else if (subFunction == NivreSubFunction.INPUT) {
			address.setAddress(((NivreConfig)arguments[0]).getInputNode(index));
		} else {
			address.setAddress(null);
		}
	}
	
	private void update(NivreConfig config) throws MaltChainedException {
		if (subFunction == NivreSubFunction.STACK) {
			address.setAddress(config.getStackNode(index));
		} else if (subFunction == NivreSubFunction.INPUT) {
			address.setAddress(config.getInputNode(index));
		} else {
			address.setAddress(null);
		}
	}
	
	public String getSubFunctionName() {
		return subFunctionName;
	}
	
	public NivreSubFunction getSubFunction() {
		return subFunction;
	}
	
	public AddressValue getAddressValue() {
		return address;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}
	
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		NivreAddressFunction other = (NivreAddressFunction) obj;
		if (index != other.index)
			return false;
		if (parsingAlgorithm == null) {
			if (other.parsingAlgorithm != null)
				return false;
		} else if (!parsingAlgorithm.equals(other.parsingAlgorithm))
			return false;
		if (subFunction == null) {
			if (other.subFunction != null)
				return false;
		} else if (!subFunction.equals(other.subFunction))
			return false;
		return true;
	}
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(subFunctionName);
		sb.append('[');
		sb.append(index);
		sb.append(']');
		return sb.toString();
	}
}
