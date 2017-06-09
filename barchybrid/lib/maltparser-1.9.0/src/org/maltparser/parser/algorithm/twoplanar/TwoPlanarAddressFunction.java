package org.maltparser.parser.algorithm.twoplanar;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.function.AddressFunction;
import org.maltparser.core.feature.value.AddressValue;
import org.maltparser.parser.AlgoritmInterface;
import org.maltparser.parser.ParsingException;

/**
*
* @author Carlos Gomez Rodriguez
**/
public final class TwoPlanarAddressFunction extends AddressFunction {
	public final static Class<?>[] paramTypes = { java.lang.Integer.class };
	public enum TwoPlanarSubFunction {
		ACTIVESTACK, INACTIVESTACK , INPUT
	};
	private final String subFunctionName;
	private final TwoPlanarSubFunction subFunction;
	private final AlgoritmInterface parsingAlgorithm;
	private int index;
	
	public TwoPlanarAddressFunction(String _subFunctionName, AlgoritmInterface _parsingAlgorithm) {
		super();
		this.subFunctionName = _subFunctionName;
		this.subFunction = TwoPlanarSubFunction.valueOf(subFunctionName.toUpperCase());
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
		update((TwoPlanarConfig)parsingAlgorithm.getCurrentParserConfiguration());
	}
	
	public void update(Object[] arguments) throws MaltChainedException {
		if (arguments.length != 1 || !(arguments[0] instanceof TwoPlanarConfig)) {
			throw new ParsingException("Arguments to the two-planar address function are not correct. ");
		}
		update((TwoPlanarConfig)arguments[0]);
	}
	
	private void update(TwoPlanarConfig config) throws MaltChainedException {
		if (subFunction == TwoPlanarSubFunction.ACTIVESTACK) {
			address.setAddress(config.getActiveStackNode(index));
		} else if ( subFunction == TwoPlanarSubFunction.INACTIVESTACK ) {
			address.setAddress(config.getInactiveStackNode(index));
		} else if (subFunction == TwoPlanarSubFunction.INPUT) {
			address.setAddress(config.getInputNode(index));
		} else {
			address.setAddress(null);
		}
	}
	
	public String getSubFunctionName() {
		return subFunctionName;
	}
	
	public TwoPlanarSubFunction getSubFunction() {
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
		
		TwoPlanarAddressFunction other = (TwoPlanarAddressFunction) obj;
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
		return subFunctionName + "[" + index + "]";
	}
}
