package org.maltparser.parser.guide.decision;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureModel;
import org.maltparser.core.feature.FeatureVector;
import org.maltparser.parser.guide.Model;
import org.maltparser.parser.history.action.GuideDecision;
/**
*
* @author Johan Hall
**/
public interface DecisionModel extends Model {
//	public void updateFeatureModel() throws MaltChainedException;
	
	public void addInstance(FeatureModel featureModel,GuideDecision decision) throws MaltChainedException;
	public boolean predict(FeatureModel featureModel, GuideDecision decision) throws MaltChainedException;
	public FeatureVector predictExtract(FeatureModel featureModel, GuideDecision decision) throws MaltChainedException;
	public FeatureVector extract(FeatureModel featureModel) throws MaltChainedException;
	public boolean predictFromKBestList(FeatureModel featureModel, GuideDecision decision) throws MaltChainedException;
	
//	public FeatureModel getFeatureModel();
	public int getDecisionIndex();
}
