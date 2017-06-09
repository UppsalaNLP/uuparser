package org.maltparser.parser.algorithm.stack;

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
public abstract class StackFactory implements AbstractParserFactory {
	protected final DependencyParserConfig manager;
	
	public StackFactory(DependencyParserConfig _manager) {
		this.manager = _manager;
	}
	
	public ParserConfiguration makeParserConfiguration() throws MaltChainedException {
		if (manager.isLoggerInfoEnabled()) {
			manager.logInfoMessage("  Parser configuration : Stack\n");
		}
		return new StackConfig();
	}
	
	public Function makeFunction(String subFunctionName, FeatureRegistry registry) throws MaltChainedException {
		AlgoritmInterface algorithm = ((ParserRegistry)registry).getAlgorithm();
		return new StackAddressFunction(subFunctionName, algorithm);
	}
}
