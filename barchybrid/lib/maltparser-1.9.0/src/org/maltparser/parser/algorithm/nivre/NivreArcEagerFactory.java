package org.maltparser.parser.algorithm.nivre;

import org.maltparser.core.exception.MaltChainedException;

import org.maltparser.parser.DependencyParserConfig;
import org.maltparser.parser.TransitionSystem;
import org.maltparser.parser.guide.OracleGuide;
import org.maltparser.parser.history.GuideUserHistory;
/**
 * @author Johan Hall
 *
 */
public class NivreArcEagerFactory extends NivreFactory {
	public NivreArcEagerFactory(DependencyParserConfig _manager) {
		super(_manager);
	}
	
	public TransitionSystem makeTransitionSystem() throws MaltChainedException {
		if (manager.isLoggerInfoEnabled()) {
			manager.logInfoMessage("  Transition system    : Arc-Eager\n");
		}
		return new ArcEager(manager.getPropagationManager());
	}
	
	public OracleGuide makeOracleGuide(GuideUserHistory history) throws MaltChainedException {
		if (manager.isLoggerInfoEnabled()) {
			manager.logInfoMessage("  Oracle               : Arc-Eager\n");
		}
		return new ArcEagerOracle(manager, history);
	}
}
