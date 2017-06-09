package org.maltparser.parser;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureModel;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.parser.guide.ClassifierGuide;
import org.maltparser.parser.guide.SingleGuide;
import org.maltparser.parser.history.action.GuideDecision;
import org.maltparser.parser.history.action.GuideUserAction;

public class DeterministicParserWithDiagnostics extends Parser {
	private final Diagnostics diagnostics;
	private int parseCount;
	private final FeatureModel featureModel;
	
	public DeterministicParserWithDiagnostics(DependencyParserConfig manager, SymbolTableHandler symbolTableHandler) throws MaltChainedException {
		super(manager,symbolTableHandler);
		this.diagnostics = new Diagnostics(manager.getOptionValue("singlemalt", "diafile").toString());
		registry.setAlgorithm(this);
		setGuide(new SingleGuide(this, ClassifierGuide.GuideMode.CLASSIFY));
		String featureModelFileName = manager.getOptionValue("guide", "features").toString().trim();
		if (manager.isLoggerInfoEnabled()) {
			manager.logDebugMessage("  Feature model        : " + featureModelFileName+"\n");
			manager.logDebugMessage("  Classifier           : " + manager.getOptionValueString("guide", "learner")+"\n");	
		}
		String dataSplitColumn = manager.getOptionValue("guide", "data_split_column").toString().trim();
		String dataSplitStructure = manager.getOptionValue("guide", "data_split_structure").toString().trim();
		featureModel = manager.getFeatureModelManager().getFeatureModel(SingleGuide.findURL(featureModelFileName, manager), 0, getParserRegistry(), dataSplitColumn, dataSplitStructure);

	}
	
	public DependencyStructure parse(DependencyStructure parseDependencyGraph) throws MaltChainedException {
		parserState.clear();
		parserState.initialize(parseDependencyGraph);
		currentParserConfiguration = parserState.getConfiguration();
		parseCount++;

		diagnostics.writeToDiaFile(parseCount + "");
		while (!parserState.isTerminalState()) {
			GuideUserAction action = parserState.getTransitionSystem().getDeterministicAction(parserState.getHistory(), currentParserConfiguration);
			if (action == null) {
				action = predict();
			} else {
				diagnostics.writeToDiaFile(" *");
			}

			diagnostics.writeToDiaFile(" " + parserState.getTransitionSystem().getActionString(action));

			parserState.apply(action);
		} 
		copyEdges(currentParserConfiguration.getDependencyGraph(), parseDependencyGraph);
		copyDynamicInput(currentParserConfiguration.getDependencyGraph(), parseDependencyGraph);
		parseDependencyGraph.linkAllTreesToRoot();

		diagnostics.writeToDiaFile("\n");

		return parseDependencyGraph;
	}
	
	
	private GuideUserAction predict() throws MaltChainedException {
		GuideUserAction currentAction = parserState.getHistory().getEmptyGuideUserAction();
		try {
			classifierGuide.predict(featureModel,(GuideDecision)currentAction);
			while (!parserState.permissible(currentAction)) {
				if (classifierGuide.predictFromKBestList(featureModel,(GuideDecision)currentAction) == false) {
					currentAction = getParserState().getTransitionSystem().defaultAction(parserState.getHistory(), currentParserConfiguration);
					break;
				}
			}
		} catch (NullPointerException e) {
			throw new MaltChainedException("The guide cannot be found. ", e);
		}
		return currentAction;
	}
	
	public void terminate() throws MaltChainedException {
		diagnostics.closeDiaWriter();
	}
}
