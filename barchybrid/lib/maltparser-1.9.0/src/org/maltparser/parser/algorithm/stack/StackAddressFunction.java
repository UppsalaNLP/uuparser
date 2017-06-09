package org.maltparser.parser.algorithm.stack;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.function.AddressFunction;
import org.maltparser.core.feature.value.AddressValue;
import org.maltparser.parser.AlgoritmInterface;
import org.maltparser.parser.ParsingException;
/**
 * @author Johan Hall
 *
 */
public final class StackAddressFunction extends AddressFunction {
	public final static Class<?>[] paramTypes = { java.lang.Integer.class };
	public enum StackSubFunction {
		STACK, INPUT, LOOKAHEAD
	};
	private final String subFunctionName;
	private final StackSubFunction subFunction;
	private final AlgoritmInterface parsingAlgorithm;
	private int index;
	
	public StackAddressFunction(String _subFunctionName, AlgoritmInterface _parsingAlgorithm) {
		super();
		this.subFunctionName = _subFunctionName;
		this.subFunction = StackSubFunction.valueOf(subFunctionName.toUpperCase());
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
		update((StackConfig)parsingAlgorithm.getCurrentParserConfiguration());
	}
	
	public void update(Object[] arguments) throws MaltChainedException {
		if (subFunction == StackSubFunction.STACK) {
			address.setAddress(((StackConfig)arguments[0]).getStackNode(index));
		} else if (subFunction == StackSubFunction.LOOKAHEAD) {
			address.setAddress(((StackConfig)arguments[0]).getLookaheadNode(index));
		} else if (subFunction == StackSubFunction.INPUT) {
			address.setAddress(((StackConfig)arguments[0]).getInputNode(index));
		} else {
			address.setAddress(null);
		}
	}
	
	private void update(StackConfig config) throws MaltChainedException {
		if (subFunction == StackSubFunction.STACK) {
			address.setAddress(config.getStackNode(index));
		} else if (subFunction == StackSubFunction.LOOKAHEAD) {
			address.setAddress(config.getLookaheadNode(index));
		} else if (subFunction == StackSubFunction.INPUT) {
			address.setAddress(config.getInputNode(index));
		} else {
			address.setAddress(null);
		}
	}
	
	public String getSubFunctionName() {
		return subFunctionName;
	}
	
	public StackSubFunction getSubFunction() {
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
		
		StackAddressFunction other = (StackAddressFunction) obj;
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
