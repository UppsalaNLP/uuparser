package org.maltparser.core.feature.map;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureException;
import org.maltparser.core.feature.function.FeatureFunction;
import org.maltparser.core.feature.function.FeatureMapFunction;
import org.maltparser.core.feature.value.FeatureValue;
import org.maltparser.core.feature.value.SingleFeatureValue;
import org.maltparser.core.io.dataformat.ColumnDescription;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.symbol.SymbolTableHandler;
/**
*
*
* @author Johan Hall
*/
public final class Merge3Feature implements FeatureMapFunction {
	public final static Class<?>[] paramTypes = { 	org.maltparser.core.feature.function.FeatureFunction.class, 
		                                            org.maltparser.core.feature.function.FeatureFunction.class, 
		                                            org.maltparser.core.feature.function.FeatureFunction.class };
	private FeatureFunction firstFeature;
	private FeatureFunction secondFeature;
	private FeatureFunction thirdFeature;
	private final SymbolTableHandler tableHandler;
	private SymbolTable table;
	private final SingleFeatureValue singleFeatureValue;
	private int type;
	
	public Merge3Feature(SymbolTableHandler tableHandler) throws MaltChainedException {
		this.tableHandler = tableHandler;
		this.singleFeatureValue = new SingleFeatureValue(this);
	}
	
	public void initialize(Object[] arguments) throws MaltChainedException {
		if (arguments.length != 3) {
			throw new FeatureException("Could not initialize Merge3Feature: number of arguments are not correct. ");
		}
		if (!(arguments[0] instanceof FeatureFunction)) {
			throw new FeatureException("Could not initialize Merge3Feature: the first argument is not a feature. ");
		}
		if (!(arguments[1] instanceof FeatureFunction)) {
			throw new FeatureException("Could not initialize Merge3Feature: the second argument is not a feature. ");
		}
		if (!(arguments[2] instanceof FeatureFunction)) {
			throw new FeatureException("Could not initialize Merge3Feature: the third argument is not a feature. ");
		}
		setFirstFeature((FeatureFunction)arguments[0]);
		setSecondFeature((FeatureFunction)arguments[1]);
		setThirdFeature((FeatureFunction)arguments[2]);
		if (firstFeature.getType() != secondFeature.getType() || firstFeature.getType() != thirdFeature.getType()) {
			throw new FeatureException("Could not initialize MergeFeature: the arguments are not of the same type.");
		}
		this.type = firstFeature.getType();
		String name = "MERGE3_"+firstFeature.getMapIdentifier()+"_"+secondFeature.getMapIdentifier()+"_"+thirdFeature.getMapIdentifier();
		setSymbolTable(tableHandler.addSymbolTable(name,ColumnDescription.INPUT, ColumnDescription.STRING, "One"));
	}
	
	public void update() throws MaltChainedException {
		singleFeatureValue.reset();
		firstFeature.update();
		secondFeature.update();
		thirdFeature.update();
		FeatureValue firstValue = firstFeature.getFeatureValue();
		FeatureValue secondValue = secondFeature.getFeatureValue();
		FeatureValue thirdValue = thirdFeature.getFeatureValue();
		if (firstValue.isMultiple() || secondValue.isMultiple() || thirdValue.isMultiple()) {
			throw new FeatureException("It is not possible to merge Split-features. ");
		}

		String firstSymbol = ((SingleFeatureValue)firstValue).getSymbol();
		if (firstValue.isNullValue() && secondValue.isNullValue() && thirdValue.isNullValue()) {
			singleFeatureValue.setIndexCode(firstFeature.getSymbolTable().getSymbolStringToCode(firstSymbol));
			singleFeatureValue.setSymbol(firstSymbol);
			singleFeatureValue.setNullValue(true);
		} else {
			if (getType() == ColumnDescription.STRING) { 
				StringBuilder mergedValue = new StringBuilder();
				mergedValue.append(((SingleFeatureValue)firstValue).getSymbol());
				mergedValue.append('~');
				mergedValue.append(((SingleFeatureValue)secondValue).getSymbol());
				mergedValue.append('~');
				mergedValue.append(((SingleFeatureValue)thirdValue).getSymbol());
				singleFeatureValue.setIndexCode(table.addSymbol(mergedValue.toString()));	
				singleFeatureValue.setSymbol(mergedValue.toString());
				singleFeatureValue.setNullValue(false);
				singleFeatureValue.setValue(1);
			} else {
				if (firstValue.isNullValue() || secondValue.isNullValue() || thirdValue.isNullValue()) {
					singleFeatureValue.setValue(0);
					table.addSymbol("#null#");
					singleFeatureValue.setSymbol("#null#");
					singleFeatureValue.setNullValue(true);
					singleFeatureValue.setIndexCode(1);
				} else {
					if (getType() == ColumnDescription.BOOLEAN) {
						boolean result = false;
						int dotIndex = firstSymbol.indexOf('.');
						result = firstSymbol.equals("1") || firstSymbol.equals("true") ||  firstSymbol.equals("#true#") || (dotIndex != -1 && firstSymbol.substring(0,dotIndex).equals("1"));
						if (result == true) {
							String secondSymbol = ((SingleFeatureValue)secondValue).getSymbol();
							dotIndex = secondSymbol.indexOf('.');
							result = secondSymbol.equals("1") || secondSymbol.equals("true") ||  secondSymbol.equals("#true#") || (dotIndex != -1 && secondSymbol.substring(0,dotIndex).equals("1"));
						}
						if (result == true) {
							String thirdSymbol = ((SingleFeatureValue)thirdValue).getSymbol();
							dotIndex = thirdSymbol.indexOf('.');
							result = thirdSymbol.equals("1") || thirdSymbol.equals("true") ||  thirdSymbol.equals("#true#") || (dotIndex != -1 && thirdSymbol.substring(0,dotIndex).equals("1"));
						}
						if (result) {
							singleFeatureValue.setValue(1);
							table.addSymbol("true");
							singleFeatureValue.setSymbol("true");
						} else {
							singleFeatureValue.setValue(0);
							table.addSymbol("false");
							singleFeatureValue.setSymbol("false");
						}
					} else if (getType() == ColumnDescription.INTEGER) {
						Integer firstInt = 0;
						Integer secondInt = 0;
						Integer thirdInt = 0;
						
						try {
							int dotIndex = firstSymbol.indexOf('.');
							if (dotIndex == -1) {
								firstInt = Integer.parseInt(firstSymbol);
							} else {
								firstInt = Integer.parseInt(firstSymbol.substring(0,dotIndex));
							}
						} catch (NumberFormatException e) {
							throw new FeatureException("Could not cast the feature value '"+firstSymbol+"' to integer value.", e);
						}
						String secondSymbol = ((SingleFeatureValue)secondValue).getSymbol();
						try {
							int dotIndex = secondSymbol.indexOf('.');
							if (dotIndex == -1) {
								secondInt = Integer.parseInt(secondSymbol);
							} else {
								secondInt = Integer.parseInt(secondSymbol.substring(0,dotIndex));
							}
						} catch (NumberFormatException e) {
							throw new FeatureException("Could not cast the feature value '"+secondSymbol+"' to integer value.", e);
						}
						String thirdSymbol = ((SingleFeatureValue)thirdValue).getSymbol();
						try {
							int dotIndex = thirdSymbol.indexOf('.');
							if (dotIndex == -1) {
								secondInt = Integer.parseInt(thirdSymbol);
							} else {
								secondInt = Integer.parseInt(thirdSymbol.substring(0,dotIndex));
							}
						} catch (NumberFormatException e) {
							throw new FeatureException("Could not cast the feature value '"+thirdSymbol+"' to integer value.", e);
						}
						Integer result = firstInt*secondInt*thirdInt;
						singleFeatureValue.setValue(result);
						table.addSymbol(result.toString());
						singleFeatureValue.setSymbol(result.toString());
					} else if (getType() == ColumnDescription.REAL) {
						Double firstReal = 0.0;
						Double secondReal = 0.0;
						Double thirdReal = 0.0;
						try {
							firstReal = Double.parseDouble(firstSymbol);
						} catch (NumberFormatException e) {
							throw new FeatureException("Could not cast the feature value '"+firstSymbol+"' to real value.", e);
						}
						String secondSymbol = ((SingleFeatureValue)secondValue).getSymbol();
						try {
							secondReal = Double.parseDouble(secondSymbol);
						} catch (NumberFormatException e) {
							throw new FeatureException("Could not cast the feature value '"+secondSymbol+"' to real value.", e);
						}
						String thirdSymbol = ((SingleFeatureValue)thirdValue).getSymbol();
						try {
							thirdReal = Double.parseDouble(thirdSymbol);
						} catch (NumberFormatException e) {
							throw new FeatureException("Could not cast the feature value '"+thirdSymbol+"' to real value.", e);
						}
						Double result = firstReal*secondReal*thirdReal;
						singleFeatureValue.setValue(result);
						table.addSymbol(result.toString());
						singleFeatureValue.setSymbol(result.toString());
					}
					singleFeatureValue.setNullValue(false);
					singleFeatureValue.setIndexCode(1);
				}
			}
		}
	}
	
	public Class<?>[] getParameterTypes() {
		return paramTypes; 
	}

	public FeatureValue getFeatureValue() {
		return singleFeatureValue;
	}

	public String getSymbol(int code) throws MaltChainedException {
		return table.getSymbolCodeToString(code);
	}
	
	public int getCode(String symbol) throws MaltChainedException {
		return table.getSymbolStringToCode(symbol);
	}
	
	public FeatureFunction getFirstFeature() {
		return firstFeature;
	}

	public void setFirstFeature(FeatureFunction firstFeature) {
		this.firstFeature = firstFeature;
	}

	public FeatureFunction getSecondFeature() {
		return secondFeature;
	}

	public void setSecondFeature(FeatureFunction secondFeature) {
		this.secondFeature = secondFeature;
	}

	public FeatureFunction getThirdFeature() {
		return thirdFeature;
	}

	public void setThirdFeature(FeatureFunction thirdFeature) {
		this.thirdFeature = thirdFeature;
	}
	
	public SymbolTableHandler getTableHandler() {
		return tableHandler;
	}

	public SymbolTable getSymbolTable() {
		return table;
	}

	public void setSymbolTable(SymbolTable table) {
		this.table = table;
	}
	
	public  int getType() {
		return type;
	}
	
	public String getMapIdentifier() {
		return getSymbolTable().getName();
	}
	
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return obj.toString().equals(this.toString());
	}
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("Merge3(");
		sb.append(firstFeature.toString());
		sb.append(", ");
		sb.append(secondFeature.toString());
		sb.append(", ");
		sb.append(thirdFeature.toString());
		sb.append(')');
		return sb.toString();
	}
}
