package org.maltparser.parser.guide.decision;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureModel;
import org.maltparser.core.feature.FeatureVector;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.parser.guide.ClassifierGuide;
import org.maltparser.parser.guide.GuideException;
import org.maltparser.parser.guide.instance.AtomicModel;
import org.maltparser.parser.guide.instance.FeatureDivideModel;
import org.maltparser.parser.guide.instance.InstanceModel;
import org.maltparser.parser.history.action.GuideDecision;
import org.maltparser.parser.history.action.MultipleDecision;
import org.maltparser.parser.history.action.SingleDecision;
import org.maltparser.parser.history.container.TableContainer.RelationToNextDecision;
/**
*
* @author Johan Hall
* @since 1.1
**/
public class SeqDecisionModel implements DecisionModel {
	private final ClassifierGuide guide;
	private final String modelName;
//	private final FeatureModel featureModel;
	private InstanceModel instanceModel;
	private final int decisionIndex;
	private final DecisionModel prevDecisionModel;
	private DecisionModel nextDecisionModel;
	private final String branchedDecisionSymbols;
	
	public SeqDecisionModel(ClassifierGuide _guide) throws MaltChainedException {
		this.guide = _guide;
		this.branchedDecisionSymbols = "";
//		this.featureModel = _featureModel;
		this.decisionIndex = 0;
		this.modelName = "sdm"+decisionIndex;
		this.prevDecisionModel = null;
	}
	
	public SeqDecisionModel(ClassifierGuide _guide, DecisionModel _prevDecisionModel, String _branchedDecisionSymbol) throws MaltChainedException {
		this.guide = _guide;
		this.decisionIndex = _prevDecisionModel.getDecisionIndex() + 1;
		if (_branchedDecisionSymbol != null && _branchedDecisionSymbol.length() > 0) {
			this.branchedDecisionSymbols = _branchedDecisionSymbol;
			this.modelName = "sdm"+decisionIndex+branchedDecisionSymbols;
		} else {
			this.branchedDecisionSymbols = "";
			this.modelName = "sdm"+decisionIndex;
		}
//		this.featureModel = _prevDecisionModel.getFeatureModel();
		this.prevDecisionModel = _prevDecisionModel;
	}
	
//	public void updateFeatureModel() throws MaltChainedException {
//		featureModel.update();
//	}
	
	private void initInstanceModel(FeatureModel featureModel, String subModelName) throws MaltChainedException {
		if (featureModel.hasDivideFeatureFunction()) {
			instanceModel = new FeatureDivideModel(this);
		} else {
			instanceModel = new AtomicModel(-1, this);
		}
	}
	
	public void finalizeSentence(DependencyStructure dependencyGraph) throws MaltChainedException {
		if (instanceModel != null) {
			instanceModel.finalizeSentence(dependencyGraph);
		}
		if (nextDecisionModel != null) {
			nextDecisionModel.finalizeSentence(dependencyGraph);
		}
	}
	
	public void noMoreInstances(FeatureModel featureModel) throws MaltChainedException {
		if (guide.getGuideMode() == ClassifierGuide.GuideMode.CLASSIFY) {
			throw new GuideException("The decision model could not create it's model. ");
		}
		if (instanceModel != null) {
			instanceModel.noMoreInstances(featureModel);
			instanceModel.train();
		}
		if (nextDecisionModel != null) {
			nextDecisionModel.noMoreInstances(featureModel);
		}
	}

	public void terminate() throws MaltChainedException {
		if (instanceModel != null) {
			instanceModel.terminate();
			instanceModel = null;
		}
		if (nextDecisionModel != null) {
			nextDecisionModel.terminate();
			nextDecisionModel = null;
		}
	}
	
	public void addInstance(FeatureModel featureModel, GuideDecision decision) throws MaltChainedException {
		if (decision instanceof SingleDecision) {
			throw new GuideException("A sequantial decision model expect a sequence of decisions, not a single decision. ");
		}
		featureModel.update();
		final SingleDecision singleDecision = ((MultipleDecision)decision).getSingleDecision(decisionIndex);
		if (instanceModel == null) {
			initInstanceModel(featureModel, singleDecision.getTableContainer().getTableContainerName());
		}
		instanceModel.addInstance(featureModel.getFeatureVector(branchedDecisionSymbols, singleDecision.getTableContainer().getTableContainerName()), singleDecision);
		if (singleDecision.continueWithNextDecision() && decisionIndex+1 < decision.numberOfDecisions()) {
			if (nextDecisionModel == null) {
				initNextDecisionModel(((MultipleDecision)decision).getSingleDecision(decisionIndex+1), branchedDecisionSymbols);
			}
			nextDecisionModel.addInstance(featureModel, decision);
		}
	}
	
	public boolean predict(FeatureModel featureModel, GuideDecision decision) throws MaltChainedException {
		if (decision instanceof SingleDecision) {
			throw new GuideException("A sequantial decision model expect a sequence of decisions, not a single decision. ");
		}
		featureModel.update();
		final SingleDecision singleDecision = ((MultipleDecision)decision).getSingleDecision(decisionIndex);
		if (instanceModel == null) {
			initInstanceModel(featureModel, singleDecision.getTableContainer().getTableContainerName());
		}

		boolean success = instanceModel.predict(featureModel.getFeatureVector(branchedDecisionSymbols, singleDecision.getTableContainer().getTableContainerName()), singleDecision);
		if (singleDecision.continueWithNextDecision() && decisionIndex+1 < decision.numberOfDecisions()) {
			if (nextDecisionModel == null) {
				initNextDecisionModel(((MultipleDecision)decision).getSingleDecision(decisionIndex+1), branchedDecisionSymbols);
			}
			success = nextDecisionModel.predict(featureModel, decision) && success;
		}
		return success;
	}
	
	public FeatureVector predictExtract(FeatureModel featureModel, GuideDecision decision) throws MaltChainedException {
		if (decision instanceof SingleDecision) {
			throw new GuideException("A sequantial decision model expect a sequence of decisions, not a single decision. ");
		}
		featureModel.update();
		final SingleDecision singleDecision = ((MultipleDecision)decision).getSingleDecision(decisionIndex);
		if (instanceModel == null) {
			initInstanceModel(featureModel, singleDecision.getTableContainer().getTableContainerName());
		}

		FeatureVector fv = instanceModel.predictExtract(featureModel.getFeatureVector(branchedDecisionSymbols, singleDecision.getTableContainer().getTableContainerName()), singleDecision);
		if (singleDecision.continueWithNextDecision() && decisionIndex+1 < decision.numberOfDecisions()) {
			if (nextDecisionModel == null) {
				initNextDecisionModel(((MultipleDecision)decision).getSingleDecision(decisionIndex+1), branchedDecisionSymbols);
			}
			nextDecisionModel.predictExtract(featureModel, decision);
		}
		return fv;
	}
	
	public FeatureVector extract(FeatureModel featureModel) throws MaltChainedException {
		featureModel.update();
		return null ; //instanceModel.extract(); // TODO handle many feature vectors
	}
	
	public boolean predictFromKBestList(FeatureModel featureModel, GuideDecision decision) throws MaltChainedException {
		if (decision instanceof SingleDecision) {
			throw new GuideException("A sequantial decision model expect a sequence of decisions, not a single decision. ");
		}
		
		boolean success = false;
		final SingleDecision singleDecision = ((MultipleDecision)decision).getSingleDecision(decisionIndex);
		// TODO develop different strategies for resolving which kBestlist that should be used
		if (nextDecisionModel != null && singleDecision.continueWithNextDecision()) {
			success = nextDecisionModel.predictFromKBestList(featureModel, decision);
		}
		if (!success) {
			success = singleDecision.updateFromKBestList();
			if (success && singleDecision.continueWithNextDecision() && decisionIndex+1 < decision.numberOfDecisions()) {
				if (nextDecisionModel == null) {
					initNextDecisionModel(((MultipleDecision)decision).getSingleDecision(decisionIndex+1), branchedDecisionSymbols);
				}
				nextDecisionModel.predict(featureModel, decision);
			}
		}
		return success;
	}
	

	public ClassifierGuide getGuide() {
		return guide;
	}

	public String getModelName() {
		return modelName;
	}
	
//	public FeatureModel getFeatureModel() {
//		return featureModel;
//	}

	public int getDecisionIndex() {
		return decisionIndex;
	}

	public DecisionModel getPrevDecisionModel() {
		return prevDecisionModel;
	}

	public DecisionModel getNextDecisionModel() {
		return nextDecisionModel;
	}
	
	private void initNextDecisionModel(SingleDecision decision, String branchedDecisionSymbol) throws MaltChainedException {
		if (decision.getRelationToNextDecision() == RelationToNextDecision.SEQUANTIAL) {
			this.nextDecisionModel = new SeqDecisionModel(guide, this, branchedDecisionSymbol);
		} else if (decision.getRelationToNextDecision() == RelationToNextDecision.BRANCHED) {
			this.nextDecisionModel = new BranchedDecisionModel(guide, this, branchedDecisionSymbol);
		} else if (decision.getRelationToNextDecision() == RelationToNextDecision.NONE) {
			this.nextDecisionModel = new OneDecisionModel(guide, this, branchedDecisionSymbol);
		}
	}
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(modelName + ", ");
		sb.append(nextDecisionModel.toString());
		return sb.toString();
	}
}
