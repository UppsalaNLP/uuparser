package org.maltparser.core.lw.parser;

import java.util.Set;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureModel;
import org.maltparser.core.feature.FeatureVector;
import org.maltparser.core.feature.value.SingleFeatureValue;
import org.maltparser.core.helper.HashMap;
import org.maltparser.parser.history.action.ComplexDecisionAction;
import org.maltparser.parser.history.action.SingleDecision;
import org.maltparser.parser.history.container.TableContainer.RelationToNextDecision;

/**
* A lightweight version of the decision models, the guide model and the instance models located in org.maltparser.parser.guide.{decision,instance} and
* can only be used in parsing mode. It is also limited to predict at most two decisions.
* 
* @author Johan Hall
*/
public final class LWDecisionModel {
	private final String classifierName;
	private final HashMap<String, LWClassifier> classifiers;
	
	public LWDecisionModel(McoModel mcoModel, boolean _excludeNullValues, String _classifierName) {
		this.classifierName = _classifierName;
		this.classifiers = new HashMap<String, LWClassifier>();
		Set<String> mcoEntryObjectKeys = mcoModel.getMcoEntryObjectKeys();
		for (String key : mcoEntryObjectKeys) {
			if (key.endsWith(".moo")) {
				String prefixFileName = key.substring(0,key.length()-4);
				classifiers.put(prefixFileName, new LWClassifier(mcoModel, prefixFileName, _excludeNullValues));
			}
		}
	}
	
	public boolean predict(FeatureModel featureModel, ComplexDecisionAction decision, boolean one_prediction) throws MaltChainedException {
		if (decision.numberOfDecisions() > 2) {
			throw new MaltChainedException("Number of decisions is greater than two,  which is unsupported in the light-weight parser (lw.parser)");
		}
		featureModel.update();
		boolean success = true;
		for (int i = 0; i < decision.numberOfDecisions(); i++) {
			LWClassifier classifier = null;
			final SingleDecision singleDecision = decision.getSingleDecision(i);
			final StringBuilder classifierString = new StringBuilder();
			
			final StringBuilder decisionModelString = new StringBuilder();
			if (singleDecision.getRelationToNextDecision() == RelationToNextDecision.BRANCHED) {
				decisionModelString.append("bdm");
			} else if (singleDecision.getRelationToNextDecision() == RelationToNextDecision.SEQUANTIAL) {
				decisionModelString.append("sdm");
			} else {
				decisionModelString.append("odm");
			}
			decisionModelString.append(i);
			String decisionSymbol = "";
			if (i == 1 && singleDecision.getRelationToNextDecision() == RelationToNextDecision.BRANCHED) {
				decisionSymbol = singleDecision.getDecisionSymbol();
				decisionModelString.append(decisionSymbol);
			}
			decisionModelString.append('.');
			FeatureVector featureVector = featureModel.getFeatureVector(decisionSymbol, singleDecision.getTableContainer().getTableContainerName());
			
			if (featureModel.hasDivideFeatureFunction()) {
				SingleFeatureValue featureValue =(SingleFeatureValue)featureModel.getDivideFeatureFunction().getFeatureValue();
				classifierString.append(decisionModelString);
				classifierString.append(String.format("%03d", featureValue.getIndexCode()));
				classifierString.append('.');
				classifierString.append(classifierName);
				classifier = classifiers.get(classifierString.toString());
				if (classifier != null) {
					FeatureVector dividefeatureVector = featureModel.getFeatureVector("/" + featureVector.getSpecSubModel().getSubModelName());
					success = classifier.predict(dividefeatureVector, singleDecision, one_prediction) && success;
					continue;
				} 
				classifierString.setLength(0);
			}

			classifierString.append(decisionModelString);
			classifierString.append(classifierName);
			classifier = classifiers.get(classifierString.toString());
			if (classifier != null) {
				success = classifier.predict(featureVector, singleDecision, one_prediction) && success;
			} else {
				singleDecision.addDecision(1);
			} 
			if (!singleDecision.continueWithNextDecision()) {
				break;
			}
		}
		return success;
	}
	
	public boolean predictFromKBestList(FeatureModel featureModel, ComplexDecisionAction decision) throws MaltChainedException {
		predict(featureModel, decision, false);
		if (decision.numberOfDecisions() == 1) {
			return decision.getSingleDecision(0).updateFromKBestList();
		} else if (decision.numberOfDecisions() > 2) {
			throw new MaltChainedException("Number of decisions is greater than two,  which is unsupported in the light-weight parser (lw.parser)");
		}
		boolean success = false;
		if (decision.getSingleDecision(0).continueWithNextDecision()) {
			success = decision.getSingleDecision(1).updateFromKBestList();
		}
		return success;
	}
}
