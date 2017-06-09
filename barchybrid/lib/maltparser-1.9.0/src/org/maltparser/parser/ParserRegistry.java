package org.maltparser.parser;

import org.maltparser.core.feature.AbstractFeatureFactory;
import org.maltparser.core.feature.FeatureRegistry;
import org.maltparser.core.helper.HashMap;
import org.maltparser.core.io.dataformat.DataFormatInstance;
import org.maltparser.core.symbol.SymbolTableHandler;

public class ParserRegistry implements FeatureRegistry {
	private AbstractFeatureFactory abstractParserFactory;
	private AlgoritmInterface algorithm;
	private SymbolTableHandler symbolTableHandler;
	private DataFormatInstance dataFormatInstance;
	private final HashMap<Class<?>, Object> registry;
	
	public ParserRegistry() {
		this.registry = new HashMap<Class<?>, Object>();
	}

	public Object get(Class<?> key) {
		return registry.get(key);
	}
	
	public void put(Class<?> key, Object value) {
		registry.put(key, value);
		if (key == org.maltparser.parser.AbstractParserFactory.class) {
			abstractParserFactory = (AbstractParserFactory)value;
		} else if (key == org.maltparser.parser.AlgoritmInterface.class) {
			algorithm = (AlgoritmInterface)value;
		}
	}
	
	public AbstractFeatureFactory getFactory(Class<?> clazz) {
		return abstractParserFactory;
	}
	
	public SymbolTableHandler getSymbolTableHandler() {
		return symbolTableHandler;
	}

	public void setSymbolTableHandler(SymbolTableHandler symbolTableHandler) {
		this.symbolTableHandler = symbolTableHandler;
		this.registry.put(org.maltparser.core.symbol.SymbolTableHandler.class, symbolTableHandler);
	}

	public DataFormatInstance getDataFormatInstance() {
		return dataFormatInstance;
	}

	public void setDataFormatInstance(DataFormatInstance dataFormatInstance) {
		this.dataFormatInstance = dataFormatInstance;
		this.registry.put(org.maltparser.core.io.dataformat.DataFormatInstance.class, dataFormatInstance);
	}

	public AbstractFeatureFactory getAbstractParserFeatureFactory() {
		return abstractParserFactory;
	}

	public void setAbstractParserFeatureFactory(AbstractParserFactory _abstractParserFactory) {
		this.registry.put(org.maltparser.parser.AbstractParserFactory.class, _abstractParserFactory);
		this.abstractParserFactory = _abstractParserFactory;
	}

	public AlgoritmInterface getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(AlgoritmInterface algorithm) {
		this.registry.put(org.maltparser.parser.AlgoritmInterface.class, algorithm);
		this.algorithm = algorithm;
	}
}
