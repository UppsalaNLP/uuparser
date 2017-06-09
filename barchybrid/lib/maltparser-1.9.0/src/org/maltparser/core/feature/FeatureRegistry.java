package org.maltparser.core.feature;

import org.maltparser.core.io.dataformat.DataFormatInstance;
import org.maltparser.core.symbol.SymbolTableHandler;

public interface FeatureRegistry {
	public Object get(Class<?> key);
	public void put(Class<?> key, Object value);
	public AbstractFeatureFactory getFactory(Class<?> clazz);
	public SymbolTableHandler getSymbolTableHandler();
	public DataFormatInstance getDataFormatInstance();
}
