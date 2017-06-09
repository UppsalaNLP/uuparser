package org.maltparser.parser.guide;

import java.net.MalformedURLException;
import java.net.URL;
import java.io.File;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureModel;
import org.maltparser.core.feature.FeatureVector;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.parser.AlgoritmInterface;
import org.maltparser.parser.DependencyParserConfig;
import org.maltparser.parser.guide.decision.BranchedDecisionModel;
import org.maltparser.parser.guide.decision.DecisionModel;
import org.maltparser.parser.guide.decision.OneDecisionModel;
import org.maltparser.parser.guide.decision.SeqDecisionModel;
import org.maltparser.parser.history.action.GuideDecision;
import org.maltparser.parser.history.action.MultipleDecision;
import org.maltparser.parser.history.action.SingleDecision;
import org.maltparser.parser.history.container.TableContainer.RelationToNextDecision;


/**
 * The guide is used by a parsing algorithm to predict the next parser action during parsing and to
 * add a instance to the training instance set during learning.

@author Johan Hall
*/
public class SingleGuide implements ClassifierGuide {
	private final DependencyParserConfig configuration;
	private final GuideMode guideMode;
	private final FeatureModel featureModel2;
	private DecisionModel decisionModel = null;
	private String guideName;
	
	public SingleGuide(AlgoritmInterface algorithm, GuideMode guideMode) throws MaltChainedException {
		this.configuration = algorithm.getManager();
		this.guideMode = guideMode;

		String featureModelFileName = getConfiguration().getOptionValue("guide", "features").toString().trim();
//		if (getConfiguration().isLoggerInfoEnabled()) {
//			
//			getConfiguration().logDebugMessage("  Feature model        : " + featureModelFileName+"\n");
//			if (getGuideMode() == ClassifierGuide.GuideMode.BATCH) {
//				getConfiguration().logDebugMessage("  Learner              : " + getConfiguration().getOptionValueString("guide", "learner").toString()+"\n");
//			} else {
//				getConfiguration().logDebugMessage("  Classifier           : " + getConfiguration().getOptionValueString("guide", "learner")+"\n");	
//			}
//		}
		String dataSplitColumn = getConfiguration().getOptionValue("guide", "data_split_column").toString().trim();
		String dataSplitStructure = getConfiguration().getOptionValue("guide", "data_split_structure").toString().trim();
		featureModel2 = getConfiguration().getFeatureModelManager().getFeatureModel(findURL(featureModelFileName, getConfiguration()), 0, algorithm.getParserRegistry(), dataSplitColumn, dataSplitStructure);
//		if (getGuideMode() == ClassifierGuide.GuideMode.BATCH) {
//				getConfiguration().writeInfoToConfigFile("\nFEATURE MODEL\n");
//				getConfiguration().writeInfoToConfigFile(featureModel.toString());
//		}
	}
		
	public void addInstance(FeatureModel featureModel,GuideDecision decision) throws MaltChainedException {
		if (decisionModel == null) {
			if (decision instanceof SingleDecision) {
				initDecisionModel((SingleDecision)decision);
			} else if (decision instanceof MultipleDecision && decision.numberOfDecisions() > 0) {
				initDecisionModel(((MultipleDecision)decision).getSingleDecision(0));
			}
		}
		decisionModel.addInstance(featureModel,decision);
	}
	
	public void finalizeSentence(DependencyStructure dependencyGraph) throws MaltChainedException {
		if (decisionModel != null) {
			decisionModel.finalizeSentence(dependencyGraph);
		}
	}
	
	public void noMoreInstances() throws MaltChainedException {
		if (decisionModel != null) {
			decisionModel.noMoreInstances(featureModel2);
		} else {
			configuration.logDebugMessage("The guide cannot create any models because there is no decision model. ");
		}
	}
	
	public void terminate() throws MaltChainedException {
		if (decisionModel != null) {
			decisionModel.terminate();
			decisionModel = null;
		}
	}

	public void predict(FeatureModel featureModel,GuideDecision decision) throws MaltChainedException {
		if (decisionModel == null) {
			if (decision instanceof SingleDecision) {
				initDecisionModel((SingleDecision)decision);
			} else if (decision instanceof MultipleDecision && decision.numberOfDecisions() > 0) {
				initDecisionModel(((MultipleDecision)decision).getSingleDecision(0));
			}
		}
		decisionModel.predict(featureModel,decision);
	}

	public FeatureVector predictExtract(FeatureModel featureModel,GuideDecision decision) throws MaltChainedException {
		if (decisionModel == null) {
			if (decision instanceof SingleDecision) {
				initDecisionModel((SingleDecision)decision);
			} else if (decision instanceof MultipleDecision && decision.numberOfDecisions() > 0) {
				initDecisionModel(((MultipleDecision)decision).getSingleDecision(0));
			}
		}
		return decisionModel.predictExtract(featureModel,decision);
	}
	
	public FeatureVector extract(FeatureModel featureModel) throws MaltChainedException {
		return decisionModel.extract(featureModel);
	}
	
	public boolean predictFromKBestList(FeatureModel featureModel, GuideDecision decision) throws MaltChainedException {
		if (decisionModel != null) {
			return decisionModel.predictFromKBestList(featureModel,decision);
		} else {
			throw new GuideException("The decision model cannot be found. ");
		}
	}
	
	public DecisionModel getDecisionModel() {
		return decisionModel;
	}

	public DependencyParserConfig getConfiguration() {
		return configuration;
	}
	
	public GuideMode getGuideMode() {
		return guideMode;
	}
	
	protected void initDecisionModel(SingleDecision decision) throws MaltChainedException {
		if (decision.getRelationToNextDecision() == RelationToNextDecision.SEQUANTIAL) {
			decisionModel = new SeqDecisionModel(this);
		} else if (decision.getRelationToNextDecision() == RelationToNextDecision.BRANCHED) {
			decisionModel = new BranchedDecisionModel(this);
		} else if (decision.getRelationToNextDecision() == RelationToNextDecision.NONE) {
			decisionModel = new OneDecisionModel(this);
		}
	}
	
	public String getGuideName() {
		return guideName;
	}

	public void setGuideName(String guideName) {
		this.guideName = guideName;
	}

	public static URL findURL(String specModelFileName, DependencyParserConfig config) throws MaltChainedException {
		URL url = null;
		File specFile = config.getFile(specModelFileName);
		if (specFile != null && specFile.exists()) {
			try {
				url = new URL("file:///"+specFile.getAbsolutePath());
			} catch (MalformedURLException e) {
				throw new MaltChainedException("Malformed URL: "+specFile, e);
			}
		} else {
			url = config.getConfigFileEntryURL(specModelFileName);
		}
		return url;
	}
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		return sb.toString();
	}
}
