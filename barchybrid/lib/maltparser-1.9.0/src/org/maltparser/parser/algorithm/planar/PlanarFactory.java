package org.maltparser.parser.algorithm.planar;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureRegistry;
import org.maltparser.core.feature.function.Function;
import org.maltparser.parser.AbstractParserFactory;
import org.maltparser.parser.AlgoritmInterface;
import org.maltparser.parser.DependencyParserConfig;
import org.maltparser.parser.ParserConfiguration;
import org.maltparser.parser.ParserRegistry;
/**
 * @author Carlos Gomez Rodriguez
 *
 */
public abstract class PlanarFactory implements AbstractParserFactory {
	protected final DependencyParserConfig manager;
	
	public PlanarFactory(DependencyParserConfig _manager) {
		this.manager = _manager;
	}
	
	public ParserConfiguration makeParserConfiguration() throws MaltChainedException {
		if (manager.isLoggerInfoEnabled()) {
			manager.logInfoMessage("  Parser configuration : Planar with no_covered_roots = " + manager.getOptionValue("planar", "no_covered_roots").toString().toUpperCase() + ", " + "acyclicity = " + manager.getOptionValue("planar", "acyclicity").toString().toUpperCase() + ", connectedness = " + manager.getOptionValue("planar", "connectedness").toString().toUpperCase() + ", planar root handling = " + manager.getOptionValue("2planar" , "planar_root_handling").toString().toUpperCase() + "\n");
		}
		return new PlanarConfig(manager.getOptionValue("planar", "no_covered_roots").toString() , manager.getOptionValue("planar", "acyclicity").toString() , manager.getOptionValue("planar", "connectedness").toString(), manager.getOptionValue("multiplanar" , "planar_root_handling").toString());
	}
	
	public Function makeFunction(String subFunctionName, FeatureRegistry registry) throws MaltChainedException {
		AlgoritmInterface algorithm = ((ParserRegistry)registry).getAlgorithm();
		return new PlanarAddressFunction(subFunctionName, algorithm);
	}
}
