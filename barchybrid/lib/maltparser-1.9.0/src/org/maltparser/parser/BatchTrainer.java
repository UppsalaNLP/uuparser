package org.maltparser.parser;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureModel;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.parser.guide.ClassifierGuide;
import org.maltparser.parser.guide.OracleGuide;
import org.maltparser.parser.guide.SingleGuide;
import org.maltparser.parser.history.action.GuideDecision;
import org.maltparser.parser.history.action.GuideUserAction;
/**
 * @author Johan Hall
 */
public class BatchTrainer extends Trainer {
	private final OracleGuide oracleGuide;
	private final FeatureModel featureModel;
	
	public BatchTrainer(DependencyParserConfig manager, SymbolTableHandler symbolTableHandler) throws MaltChainedException {
		super(manager,symbolTableHandler);
		registry.setAlgorithm(this);
		setGuide(new SingleGuide(this,  ClassifierGuide.GuideMode.BATCH));
		String featureModelFileName = manager.getOptionValue("guide", "features").toString().trim();
		if (manager.isLoggerInfoEnabled()) {
			manager.logDebugMessage("  Feature model        : " + featureModelFileName+"\n");
			manager.logDebugMessage("  Learner              : " + manager.getOptionValueString("guide", "learner").toString()+"\n");
		}
		String dataSplitColumn = manager.getOptionValue("guide", "data_split_column").toString().trim();
		String dataSplitStructure = manager.getOptionValue("guide", "data_split_structure").toString().trim();
		this.featureModel = manager.getFeatureModelManager().getFeatureModel(SingleGuide.findURL(featureModelFileName, manager), 0, getParserRegistry(), dataSplitColumn, dataSplitStructure);

		manager.writeInfoToConfigFile("\nFEATURE MODEL\n");
		manager.writeInfoToConfigFile(featureModel.toString());

		oracleGuide = parserState.getFactory().makeOracleGuide(parserState.getHistory());
	}
	
	public DependencyStructure parse(DependencyStructure goldDependencyGraph, DependencyStructure parseDependencyGraph) throws MaltChainedException {
		parserState.clear();
		parserState.initialize(parseDependencyGraph);
		currentParserConfiguration = parserState.getConfiguration();

		TransitionSystem transitionSystem = parserState.getTransitionSystem();
		while (!parserState.isTerminalState()) {
			GuideUserAction action = transitionSystem.getDeterministicAction(parserState.getHistory(), currentParserConfiguration);
			if (action == null) {
				action = oracleGuide.predict(goldDependencyGraph, currentParserConfiguration);
				try {
					classifierGuide.addInstance(featureModel, (GuideDecision)action);
				} catch (NullPointerException e) {
					throw new MaltChainedException("The guide cannot be found. ", e);
				}
			}
			parserState.apply(action);
		}
		copyEdges(currentParserConfiguration.getDependencyGraph(), parseDependencyGraph);
		parseDependencyGraph.linkAllTreesToRoot();
		oracleGuide.finalizeSentence(parseDependencyGraph);
		return parseDependencyGraph;
	}
	
	public OracleGuide getOracleGuide() {
		return oracleGuide;
	}
	
	public void train() throws MaltChainedException { }
	public void terminate() throws MaltChainedException {
	}
}
