package org.maltparser.parser.guide.decision;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureModel;
import org.maltparser.core.feature.FeatureVector;
import org.maltparser.core.helper.HashMap;
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
**/
public class BranchedDecisionModel implements DecisionModel {
	private final ClassifierGuide guide;
	private final String modelName;
//	private final FeatureModel featureModel;
	private InstanceModel instanceModel;
	private final int decisionIndex;
	private final DecisionModel parentDecisionModel;
	private final HashMap<Integer,DecisionModel> children;
	private final String branchedDecisionSymbols;
	
	public BranchedDecisionModel(ClassifierGuide _guide) throws MaltChainedException {
		this.guide = _guide;
		this.branchedDecisionSymbols = "";
//		this.featureModel = _featureModel;
		this.decisionIndex = 0;
		this.modelName = "bdm0";
		this.parentDecisionModel = null;
		this.children = new HashMap<Integer,DecisionModel>();
	}
	
	public BranchedDecisionModel(ClassifierGuide _guide, DecisionModel _parentDecisionModel, String _branchedDecisionSymbol) throws MaltChainedException {
		this.guide = _guide;
		this.parentDecisionModel =_parentDecisionModel;
		this.decisionIndex = parentDecisionModel.getDecisionIndex() + 1;
		if (_branchedDecisionSymbol != null && _branchedDecisionSymbol.length() > 0) {
			this.branchedDecisionSymbols = _branchedDecisionSymbol;
			this.modelName = "bdm"+decisionIndex+branchedDecisionSymbols;
		} else {
			this.branchedDecisionSymbols = "";
			this.modelName = "bdm"+decisionIndex;
		}
//		this.featureModel = parentDecisionModel.getFeatureModel();
		this.children = new HashMap<Integer,DecisionModel>();
	}
	
	private void initInstanceModel(FeatureModel featureModel, String subModelName) throws MaltChainedException {
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
		if (children != null) {
			for (DecisionModel child : children.values()) {
				child.finalizeSentence(dependencyGraph);
			}
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
		if (children != null) {
			for (DecisionModel child : children.values()) {
				child.noMoreInstances(featureModel);
			}
		}
	}

	public void terminate() throws MaltChainedException {
		if (instanceModel != null) {
			instanceModel.terminate();
			instanceModel = null;
		}
		if (children != null) {
			for (DecisionModel child : children.values()) {
				child.terminate();
			}
		}
	}
	
	public void addInstance(FeatureModel featureModel, GuideDecision decision) throws MaltChainedException {
		if (decision instanceof SingleDecision) {
			throw new GuideException("A branched decision model expect more than one decisions. ");
		}
		featureModel.update();
		final SingleDecision singleDecision = ((MultipleDecision)decision).getSingleDecision(decisionIndex);
		if (instanceModel == null) {
			initInstanceModel(featureModel,singleDecision.getTableContainer().getTableContainerName());
		}
		
		instanceModel.addInstance(featureModel.getFeatureVector(branchedDecisionSymbols, singleDecision.getTableContainer().getTableContainerName()), singleDecision);
		if (decisionIndex+1 < decision.numberOfDecisions()) {
			if (singleDecision.continueWithNextDecision()) {
				DecisionModel child = children.get(singleDecision.getDecisionCode());
				if (child == null) {
					child = initChildDecisionModel(((MultipleDecision)decision).getSingleDecision(decisionIndex+1), 
							branchedDecisionSymbols+(branchedDecisionSymbols.length() == 0?"":"_")+singleDecision.getDecisionSymbol());
					children.put(singleDecision.getDecisionCode(), child);
				}
				child.addInstance(featureModel,decision);
			}
		}
	}
	
	public boolean predict(FeatureModel featureModel, GuideDecision decision) throws MaltChainedException {
		featureModel.update();
		final SingleDecision singleDecision = ((MultipleDecision)decision).getSingleDecision(decisionIndex);
		if (instanceModel == null) {
			initInstanceModel(featureModel, singleDecision.getTableContainer().getTableContainerName());
		}
		instanceModel.predict(featureModel.getFeatureVector(branchedDecisionSymbols, singleDecision.getTableContainer().getTableContainerName()), singleDecision);
		if (decisionIndex+1 < decision.numberOfDecisions()) {
			if (singleDecision.continueWithNextDecision()) {
				DecisionModel child = children.get(singleDecision.getDecisionCode());
				if (child == null) {
					child = initChildDecisionModel(((MultipleDecision)decision).getSingleDecision(decisionIndex+1), 
							branchedDecisionSymbols+(branchedDecisionSymbols.length() == 0?"":"_")+singleDecision.getDecisionSymbol());
					children.put(singleDecision.getDecisionCode(), child);
				}
				child.predict(featureModel, decision);
			}
		}

		return true;
	}
	
	public FeatureVector predictExtract(FeatureModel featureModel, GuideDecision decision) throws MaltChainedException {
		if (decision instanceof SingleDecision) {
			throw new GuideException("A branched decision model expect more than one decisions. ");
		}
		featureModel.update();
		final SingleDecision singleDecision = ((MultipleDecision)decision).getSingleDecision(decisionIndex);
		if (instanceModel == null) {
			initInstanceModel(featureModel, singleDecision.getTableContainer().getTableContainerName());
		}
		FeatureVector fv = instanceModel.predictExtract(featureModel.getFeatureVector(branchedDecisionSymbols, singleDecision.getTableContainer().getTableContainerName()), singleDecision);
		if (decisionIndex+1 < decision.numberOfDecisions()) {
			if (singleDecision.continueWithNextDecision()) {
				DecisionModel child = children.get(singleDecision.getDecisionCode());
				if (child == null) {
					child = initChildDecisionModel(((MultipleDecision)decision).getSingleDecision(decisionIndex+1), 
							branchedDecisionSymbols+(branchedDecisionSymbols.length() == 0?"":"_")+singleDecision.getDecisionSymbol());
					children.put(singleDecision.getDecisionCode(), child);
				}
				child.predictExtract(featureModel, decision);
			}
		}

		return fv;
	}
	
	public FeatureVector extract(FeatureModel featureModel) throws MaltChainedException {
		featureModel.update();
		return null; //instanceModel.extract(); // TODO handle many feature vectors
	}
	
	public boolean predictFromKBestList(FeatureModel featureModel, GuideDecision decision) throws MaltChainedException {
		if (decision instanceof SingleDecision) {
			throw new GuideException("A branched decision model expect more than one decisions. ");
		}
		
		boolean success = false;
		final SingleDecision singleDecision = ((MultipleDecision)decision).getSingleDecision(decisionIndex);
		if (decisionIndex+1 < decision.numberOfDecisions()) {
			if (singleDecision.continueWithNextDecision()) {
				DecisionModel child = children.get(singleDecision.getDecisionCode());
				if (child != null) {
					success = child.predictFromKBestList(featureModel, decision);
				}
				
			}
		}
		if (!success) {
			success = singleDecision.updateFromKBestList();
			if (decisionIndex+1 < decision.numberOfDecisions()) {
				if (singleDecision.continueWithNextDecision()) {
					DecisionModel child = children.get(singleDecision.getDecisionCode());
					if (child == null) {
						child = initChildDecisionModel(((MultipleDecision)decision).getSingleDecision(decisionIndex+1), 
								branchedDecisionSymbols+(branchedDecisionSymbols.length() == 0?"":"_")+singleDecision.getDecisionSymbol());
						children.put(singleDecision.getDecisionCode(), child);
					}
					child.predict(featureModel, decision);
				}
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

	public DecisionModel getParentDecisionModel() {
		return parentDecisionModel;
	}
	
	private DecisionModel initChildDecisionModel(SingleDecision decision, String branchedDecisionSymbol) throws MaltChainedException {
		if (decision.getRelationToNextDecision() == RelationToNextDecision.SEQUANTIAL) {
			return new SeqDecisionModel(guide, this, branchedDecisionSymbol);
		} else if (decision.getRelationToNextDecision() == RelationToNextDecision.BRANCHED) {
			return new BranchedDecisionModel(guide, this, branchedDecisionSymbol);
		} else if (decision.getRelationToNextDecision() == RelationToNextDecision.NONE) {
			return new OneDecisionModel(guide, this, branchedDecisionSymbol);
		}
		throw new GuideException("Could not find an appropriate decision model for the relation to the next decision"); 
	}
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(modelName + ", ");
		for (DecisionModel model : children.values()) {
			sb.append(model.toString() + ", ");
		}
		return sb.toString();
	}
}
