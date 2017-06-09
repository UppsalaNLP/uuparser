package org.maltparser.parser;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.parser.history.GuideUserHistory;
import org.maltparser.parser.history.History;
import org.maltparser.parser.history.HistoryList;
import org.maltparser.parser.history.HistoryStructure;
import org.maltparser.parser.history.action.GuideUserAction;
/**
 * @author Johan Hall
 *
 */
public class ParserState {
	private final AbstractParserFactory factory;
	private final GuideUserHistory history;
	private final TransitionSystem transitionSystem;
	private final HistoryStructure historyStructure;
	private final ParserConfiguration config;
	
	public ParserState(DependencyParserConfig manager, SymbolTableHandler symbolTableHandler, AbstractParserFactory factory) throws MaltChainedException {
		this.factory = factory;
		this.historyStructure = new HistoryList();
		this.transitionSystem = factory.makeTransitionSystem();
		String decisionSettings = manager.getOptionValue("guide", "decision_settings").toString().trim();
		getTransitionSystem().initTableHandlers(decisionSettings, symbolTableHandler);
		int kBestSize = ((Integer)manager.getOptionValue("guide", "kbest")).intValue();
		String classitem_separator = manager.getOptionValue("guide", "classitem_separator").toString();
		this.history = new History(decisionSettings, classitem_separator, getTransitionSystem().getTableHandlers(), kBestSize);
		getTransitionSystem().initTransitionSystem(history);
		this.config = factory.makeParserConfiguration();
	}
	
	
	public void clear() throws MaltChainedException {
		history.clear();
		historyStructure.clear();
	}
	
	public GuideUserHistory getHistory() {
		return history;
	}

	public TransitionSystem getTransitionSystem() {
		return transitionSystem;
	}
	
	public HistoryStructure getHistoryStructure() {
		return historyStructure;
	}
	
	public void initialize(DependencyStructure dependencyStructure) throws MaltChainedException {
		config.clear();
		config.setDependencyGraph(dependencyStructure);
		config.initialize();
	}
	
	public boolean isTerminalState() throws MaltChainedException {
		return config.isTerminalState();
	}
	
	public boolean permissible(GuideUserAction currentAction) throws MaltChainedException {
		return transitionSystem.permissible(currentAction, config); 
	}
	
	public void apply(GuideUserAction currentAction) throws MaltChainedException {
		transitionSystem.apply(currentAction, config);
	}
	
	public int nConfigurations() throws MaltChainedException {
		return 1;
	}
	
	public ParserConfiguration getConfiguration() {
		return config;
	}

	public AbstractParserFactory getFactory() {
		return factory;
	}
}
