package org.maltparser.parser;

import org.maltparser.core.config.Configuration;
import org.maltparser.core.io.dataformat.DataFormatInstance;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureModelManager;
import org.maltparser.core.propagation.PropagationManager;
import org.maltparser.core.syntaxgraph.DependencyStructure;

/**
 * @author Johan Hall
 *
 */
public interface DependencyParserConfig extends Configuration {
	public void parse(DependencyStructure graph) throws MaltChainedException;
	public void oracleParse(DependencyStructure goldGraph, DependencyStructure oracleGraph) throws MaltChainedException;
	public DataFormatInstance getDataFormatInstance();
	public FeatureModelManager getFeatureModelManager();
	public PropagationManager getPropagationManager();
	public AbstractParserFactory getParserFactory();
}
