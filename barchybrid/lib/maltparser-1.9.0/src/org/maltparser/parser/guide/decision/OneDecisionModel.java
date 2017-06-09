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
/**
*
* @author Johan Hall
**/
public class OneDecisionModel implements DecisionModel {
	private final ClassifierGuide guide;
	private final String modelName;
//	private final FeatureModel featureModel;
	private final int decisionIndex;
	private final DecisionModel prevDecisionModel;
	private final String branchedDecisionSymbols;
	private InstanceModel instanceModel;
	
	public OneDecisionModel(ClassifierGuide _guide) throws MaltChainedException {
		this.branchedDecisionSymbols = "";
		this.guide = _guide;
//		this.featureModel = _featureModel;
		this.decisionIndex = 0;
		if (guide.getGuideName() == null || guide.getGuideName().equals("")) {
			this.modelName = "odm"+decisionIndex;
		} else {
			this.modelName = guide.getGuideName()+".odm"+decisionIndex;
		}
		this.prevDecisionModel = null;
	}
	
	public OneDecisionModel(ClassifierGuide _guide, DecisionModel _prevDecisionModel, String _branchedDecisionSymbol) throws MaltChainedException {
		this.prevDecisionModel = _prevDecisionModel;
		this.decisionIndex = prevDecisionModel.getDecisionIndex() + 1;
		if (_branchedDecisionSymbol != null && _branchedDecisionSymbol.length() > 0) {
			this.branchedDecisionSymbols = _branchedDecisionSymbol;
			this.modelName = "odm"+decisionIndex+branchedDecisionSymbols;
		} else {
			this.branchedDecisionSymbols = "";
			this.modelName = "odm"+decisionIndex;
		}
		this.guide = _guide;
//		this.featureModel = prevDecisionModel.getFeatureModel();
	}
	
	private final void initInstanceModel(FeatureModel featureModel, String subModelName) throws MaltChainedException {
		if (featureModel.hasDivideFeatureFunction()) {
			instanceModel = new FeatureDivideModel(this);
		} else {
			instanceModel = new AtomicModel(-1, this);
		}
	}
	
//	public void updateFeatureModel() throws MaltChainedException {
//		featureModel.update();
//	}

	public void finalizeSentence(DependencyStructure dependencyGraph) throws MaltChainedException {
		if (instanceModel != null) {
			instanceModel.finalizeSentence(dependencyGraph);
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
	}

	public void terminate() throws MaltChainedException {
		if (instanceModel != null) {
			instanceModel.terminate();
			instanceModel = null;
		}
	}
	
	public void addInstance(FeatureModel featureModel, GuideDecision decision) throws MaltChainedException {
		featureModel.update();
		final SingleDecision singleDecision = (decision instanceof SingleDecision)?(SingleDecision)decision:((MultipleDecision)decision).getSingleDecision(decisionIndex);
		
		if (instanceModel == null) {
			initInstanceModel(featureModel, singleDecision.getTableContainer().getTableContainerName());
		}
		instanceModel.addInstance(featureModel.getFeatureVector(branchedDecisionSymbols, singleDecision.getTableContainer().getTableContainerName()), singleDecision);
	}
	
	public boolean predict(FeatureModel featureModel, GuideDecision decision) throws MaltChainedException {
		featureModel.update();
		final SingleDecision singleDecision = (decision instanceof SingleDecision)?(SingleDecision)decision:((MultipleDecision)decision).getSingleDecision(decisionIndex);

		if (instanceModel == null) {
			initInstanceModel(featureModel, singleDecision.getTableContainer().getTableContainerName());
		}
		return instanceModel.predict(featureModel.getFeatureVector(branchedDecisionSymbols, singleDecision.getTableContainer().getTableContainerName()), singleDecision);
	}
	
	public FeatureVector predictExtract(FeatureModel featureModel, GuideDecision decision) throws MaltChainedException {
		featureModel.update();
		final SingleDecision singleDecision = (decision instanceof SingleDecision)?(SingleDecision)decision:((MultipleDecision)decision).getSingleDecision(decisionIndex);

		if (instanceModel == null) {
			initInstanceModel(featureModel, singleDecision.getTableContainer().getTableContainerName());
		}
		return instanceModel.predictExtract(featureModel.getFeatureVector(branchedDecisionSymbols, singleDecision.getTableContainer().getTableContainerName()), singleDecision);
	}
	
	public FeatureVector extract(FeatureModel featureModel) throws MaltChainedException {
		featureModel.update();
		return null; //instanceModel.extract(featureModel.getFeatureVector(branchedDecisionSymbols, singleDecision.getTableContainer().getTableContainerName()));
	}
	
	public boolean predictFromKBestList(FeatureModel featureModel, GuideDecision decision) throws MaltChainedException {
		if (decision instanceof SingleDecision) {
			return ((SingleDecision)decision).updateFromKBestList();
		} else {
			return ((MultipleDecision)decision).getSingleDecision(decisionIndex).updateFromKBestList();
		}
	}
	
	public ClassifierGuide getGuide() {
		return guide;
	}

	public String getModelName() {
		return modelName;
	}

	public int getDecisionIndex() {
		return decisionIndex;
	}

	public DecisionModel getPrevDecisionModel() {
		return prevDecisionModel;
	}
	
	public String toString() {		
		return modelName;
	}
}
