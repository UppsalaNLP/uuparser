package org.maltparser.parser.algorithm.covington;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureRegistry;
import org.maltparser.core.feature.function.Function;
import org.maltparser.parser.AbstractParserFactory;
import org.maltparser.parser.AlgoritmInterface;
import org.maltparser.parser.DependencyParserConfig;
import org.maltparser.parser.ParserConfiguration;
import org.maltparser.parser.ParserRegistry;
/**
 * @author Johan Hall
 *
 */
public abstract class CovingtonFactory implements AbstractParserFactory {
	protected final DependencyParserConfig manager;
	
	public CovingtonFactory(DependencyParserConfig _manager) {
		this.manager = _manager;
	}
	
	public ParserConfiguration makeParserConfiguration() throws MaltChainedException {
		boolean allowRoot = (Boolean)manager.getOptionValue("covington", "allow_root");
		boolean allowShift = (Boolean)manager.getOptionValue("covington", "allow_shift");
		if (manager.isLoggerInfoEnabled()) {
			manager.logInfoMessage("  Parser configuration : Covington with allow_root="+allowRoot+" and allow_shift="+allowShift+"\n");
		}
		CovingtonConfig config = new CovingtonConfig(allowRoot, allowShift);
		return config;
	}
	
	public Function makeFunction(String subFunctionName, FeatureRegistry registry) throws MaltChainedException {
		AlgoritmInterface algorithm = ((ParserRegistry)registry).getAlgorithm();
		return new CovingtonAddressFunction(subFunctionName, algorithm);
	}
}
