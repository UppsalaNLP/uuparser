package org.maltparser.parser.algorithm.twoplanar;

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
public abstract class TwoPlanarFactory implements AbstractParserFactory {
	protected final DependencyParserConfig manager;
	
	public TwoPlanarFactory(DependencyParserConfig _manager) {
		this.manager = _manager;
	}
	
	public ParserConfiguration makeParserConfiguration() throws MaltChainedException {
		if (manager.isLoggerInfoEnabled()) {
			manager.logInfoMessage("  Parser configuration : Two-Planar with no_covered_roots = " + manager.getOptionValue("planar", "no_covered_roots").toString().toUpperCase() + ", " + "acyclicity = " + manager.getOptionValue("planar", "acyclicity").toString().toUpperCase() + ", planar root handling = " + manager.getOptionValue("2planar" , "planar_root_handling").toString().toUpperCase() + ", reduce on switch = " + manager.getOptionValue("2planar" , "reduceonswitch").toString().toUpperCase() + "\n");
		}
		return new TwoPlanarConfig(manager.getOptionValue("planar", "no_covered_roots").toString() , manager.getOptionValue("planar", "acyclicity").toString() , manager.getOptionValue("2planar" , "reduceonswitch").toString()  , manager.getOptionValue("multiplanar" , "planar_root_handling").toString() );
	}
	
	public Function makeFunction(String subFunctionName, FeatureRegistry registry) throws MaltChainedException {
		AlgoritmInterface algorithm = ((ParserRegistry)registry).getAlgorithm();
		return new TwoPlanarAddressFunction(subFunctionName, algorithm);
	}
}
