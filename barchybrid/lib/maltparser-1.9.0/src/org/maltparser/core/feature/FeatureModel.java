package org.maltparser.core.feature;


import java.util.ArrayList;
import java.util.Stack;
import java.util.regex.Pattern;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.function.AddressFunction;
import org.maltparser.core.feature.function.FeatureFunction;
import org.maltparser.core.feature.function.Function;
import org.maltparser.core.feature.spec.SpecificationModel;
import org.maltparser.core.feature.spec.SpecificationSubModel;
import org.maltparser.core.feature.system.FeatureEngine;
import org.maltparser.core.helper.HashMap;


/**
*
*
* @author Johan Hall
*/
public class FeatureModel extends HashMap<String, FeatureVector> {
	public final static long serialVersionUID = 3256444702936019250L;
	private final static Pattern splitPattern = Pattern.compile("\\(|\\)|\\[|\\]|,");
	private final SpecificationModel specModel;
	private final ArrayList<AddressFunction> addressFunctionCache;
	private final ArrayList<FeatureFunction> featureFunctionCache;
	private final FeatureFunction divideFeatureFunction;
	private final FeatureRegistry registry;
	private final FeatureEngine featureEngine;
	private final FeatureVector mainFeatureVector; 
	private final ArrayList<Integer> divideFeatureIndexVector;
	
	public FeatureModel(SpecificationModel _specModel, FeatureRegistry _registry, FeatureEngine _engine, String dataSplitColumn, String dataSplitStructure) throws MaltChainedException {
		this.specModel = _specModel;
		this.registry = _registry;
		this.featureEngine = _engine;
		this.addressFunctionCache = new ArrayList<AddressFunction>();
		this.featureFunctionCache = new ArrayList<FeatureFunction>();
		FeatureVector tmpMainFeatureVector = null;
		for (SpecificationSubModel subModel : specModel) {
			FeatureVector fv = new FeatureVector(this, subModel);
			if (tmpMainFeatureVector == null) {
				if (subModel.getSubModelName().equals("MAIN")) {
					tmpMainFeatureVector = fv;
				} else {
					tmpMainFeatureVector = fv;
					put(subModel.getSubModelName(), fv);
				}
			} else {
				put(subModel.getSubModelName(), fv);
			}
		}
		this.mainFeatureVector = tmpMainFeatureVector;
		if (dataSplitColumn != null && dataSplitColumn.length() > 0 && dataSplitStructure != null && dataSplitStructure.length() > 0) {
			final StringBuilder sb = new StringBuilder();
			sb.append("InputColumn(");
			sb.append(dataSplitColumn);
			sb.append(", ");
			sb.append(dataSplitStructure);
			sb.append(')');
			this.divideFeatureFunction = identifyFeature(sb.toString());
//			this.divideFeatureIndexVectorMap = new HashMap<String,ArrayList<Integer>>();
			this.divideFeatureIndexVector = new ArrayList<Integer>();

			for (int i = 0; i < mainFeatureVector.size(); i++) {
				if (mainFeatureVector.get(i).equals(divideFeatureFunction)) {
					divideFeatureIndexVector.add(i);
				}
			}
			for (SpecificationSubModel subModel : specModel) {
				FeatureVector featureVector = get(subModel.getSubModelName());
				if (featureVector == null) {
					featureVector = mainFeatureVector;	
				}
				String divideKeyName = "/"+subModel.getSubModelName();
//				divideFeatureIndexVectorMap.put(divideKeyName, divideFeatureIndexVector);
				
				FeatureVector divideFeatureVector = (FeatureVector)featureVector.clone();
				for (Integer i : divideFeatureIndexVector) {
					divideFeatureVector.remove(divideFeatureVector.get(i));
				}
				put(divideKeyName,divideFeatureVector);
			}
		} else {
			this.divideFeatureFunction = null;
//			this.divideFeatureIndexVectorMap = null;
			this.divideFeatureIndexVector = null;
		}
	}

	public SpecificationModel getSpecModel() {
		return specModel;
	}
	
	public FeatureRegistry getRegistry() {
		return registry;
	}

	public FeatureEngine getFeatureEngine() {
		return featureEngine;
	}
	
	public FeatureVector getMainFeatureVector() {
		return mainFeatureVector;
	}
	
	public FeatureVector getFeatureVector(String subModelName) {
		return get(subModelName);
	}
	
	public FeatureVector getFeatureVector(String decisionSymbol, String subModelName) {
		final StringBuilder sb = new StringBuilder();
		if (decisionSymbol.length() > 0) {
			sb.append(decisionSymbol);
			sb.append('.');
		}
		sb.append(subModelName);
		if (containsKey(sb.toString())) {
			return get(sb.toString());
		} else if (containsKey(subModelName)) {
			return get(subModelName);
		}
		return mainFeatureVector;
	}
	
	public FeatureFunction getDivideFeatureFunction() {
		return divideFeatureFunction;
	}
	
	public boolean hasDivideFeatureFunction() {
		return divideFeatureFunction != null;
	}

//	public ArrayList<Integer> getDivideFeatureIndexVectorMap(String divideSubModelName) {
//		return divideFeatureIndexVectorMap.get(divideSubModelName);
//	}
//
//	public boolean hasDivideFeatureIndexVectorMap() {
//		return divideFeatureIndexVectorMap != null;
//	}
	
	public ArrayList<Integer> getDivideFeatureIndexVector() {
		return divideFeatureIndexVector;
	}

	public boolean hasDivideFeatureIndexVector() {
		return divideFeatureIndexVector != null;
	}
	
	public void update() throws MaltChainedException {
		for (int i = 0, n = addressFunctionCache.size(); i < n; i++) {
			addressFunctionCache.get(i).update();
		}
		
		for (int i = 0, n = featureFunctionCache.size(); i < n; i++) {
			featureFunctionCache.get(i).update();
		}
	}
	
	public void update(Object[] arguments) throws MaltChainedException {
		for (int i = 0, n = addressFunctionCache.size(); i < n; i++) {
			addressFunctionCache.get(i).update(arguments);
		}
		
		for (int i = 0, n = featureFunctionCache.size(); i < n; i++) {
			featureFunctionCache.get(i).update();
		}
	}
	
	public FeatureFunction identifyFeature(String spec) throws MaltChainedException {
		String[] items =splitPattern.split(spec);
		Stack<Object> objects = new Stack<Object>();
		for (int i = items.length-1; i >= 0; i--) {
			if (items[i].trim().length() != 0) {
				objects.push(items[i].trim());
			}
		}
		identifyFeatureFunction(objects);
		if (objects.size() != 1 || !(objects.peek() instanceof FeatureFunction) || (objects.peek() instanceof AddressFunction)) {
			throw new FeatureException("The feature specification '"+spec+"' were not recognized properly. ");
		}
		return (FeatureFunction)objects.pop();
	}
	
	protected void identifyFeatureFunction(Stack<Object> objects) throws MaltChainedException {
		Function function = featureEngine.newFunction(objects.peek().toString(), registry);
		if (function != null) {
			objects.pop();
			if (!objects.isEmpty()) {
				identifyFeatureFunction(objects);
			}
			initializeFunction(function, objects);
		} else {
			if (!objects.isEmpty()) {
				Object o = objects.pop();
				if (!objects.isEmpty()) {
					identifyFeatureFunction(objects);
				}
				objects.push(o);
			}
		}
	}
	
	protected void initializeFunction(Function function, Stack<Object> objects) throws MaltChainedException {
		Class<?>[] paramTypes = function.getParameterTypes();
		Object[] arguments = new Object[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			if (paramTypes[i] == java.lang.Integer.class) {
				if (objects.peek() instanceof String) {
					String object = (String)objects.pop();
					try {
						objects.push(Integer.parseInt(object));
					} catch (NumberFormatException e) {
						throw new FeatureException("The function '"+function.getClass()+"' cannot be initialized with argument '"+object+"'" + ", expect an integer value. ", e);
					}
				} else {
					throw new FeatureException("The function '"+function.getClass()+"' cannot be initialized with argument '"+objects.peek()+"'" + ", expect an integer value. ");
				}
			} else if (paramTypes[i] == java.lang.Double.class) {
				if (objects.peek() instanceof String) {
					String object = (String)objects.pop();
					try {
						objects.push(Double.parseDouble(object));
					} catch (NumberFormatException e) {
						throw new FeatureException("The function '"+function.getClass()+"' cannot be initialized with argument '"+object+"'" + ", expect a numeric value. ", e);
					}
				} else {
					throw new FeatureException("The function '"+function.getClass()+"' cannot be initialized with argument '"+objects.peek()+"'" + ", expect a numeric value. ");
				}
			} else if (paramTypes[i] == java.lang.Boolean.class) {
				if (objects.peek() instanceof String) {
					objects.push(Boolean.parseBoolean(((String)objects.pop())));
				} else {
					throw new FeatureException("The function '"+function.getClass()+"' cannot be initialized with argument '"+objects.peek()+"'" + ", expect a boolean value. ");
					
				}
			}
			if (!paramTypes[i].isInstance(objects.peek())) {
				throw new FeatureException("The function '"+function.getClass()+"' cannot be initialized with argument '"+objects.peek()+"'");
			}
			arguments[i] = objects.pop();
		}
		function.initialize(arguments);
		if (function instanceof AddressFunction) {
			int index = addressFunctionCache.indexOf(function);
			if (index != -1) {
				function = addressFunctionCache.get(index);
			} else {
				addressFunctionCache.add((AddressFunction)function);
			}
		} else if (function instanceof FeatureFunction) {
			int index = featureFunctionCache.indexOf(function);
			if (index != -1) {
				function = featureFunctionCache.get(index);
			} else {
				featureFunctionCache.add((FeatureFunction)function);
			}
		}
		objects.push(function);
	}
	
	public String toString() {
		return specModel.toString();
	}
}
