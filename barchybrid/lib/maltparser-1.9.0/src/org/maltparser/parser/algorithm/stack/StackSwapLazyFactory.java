package org.maltparser.parser.algorithm.stack;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.parser.DependencyParserConfig;
import org.maltparser.parser.TransitionSystem;
import org.maltparser.parser.guide.OracleGuide;
import org.maltparser.parser.history.GuideUserHistory;
/**
 * @author Johan Hall
 *
 */
public class StackSwapLazyFactory extends StackFactory {
	public StackSwapLazyFactory(DependencyParserConfig _manager) {
		super(_manager);
	}
	
	public TransitionSystem makeTransitionSystem() throws MaltChainedException {
		if (manager.isLoggerInfoEnabled()) {
			manager.logInfoMessage("  Transition system    : Non-Projective\n");
		}
		return new NonProjective(manager.getPropagationManager());
	}
	
	public OracleGuide makeOracleGuide(GuideUserHistory history) throws MaltChainedException {
		if (manager.isLoggerInfoEnabled()) {
			manager.logInfoMessage("  Oracle               : Swap-Lazy\n");
		}
		return new SwapLazyOracle(manager, history);
	}
}
