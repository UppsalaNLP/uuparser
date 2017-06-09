package org.maltparser.parser.guide;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureModel;
import org.maltparser.core.feature.FeatureVector;
import org.maltparser.parser.history.action.GuideDecision;

public interface ClassifierGuide extends Guide {
	public enum GuideMode { BATCH, CLASSIFY}
	
	public void addInstance(FeatureModel featureModel, GuideDecision decision) throws MaltChainedException;
	public void noMoreInstances() throws MaltChainedException;
	public void predict(FeatureModel featureModel, GuideDecision decision) throws MaltChainedException;
	public FeatureVector predictExtract(FeatureModel featureModel, GuideDecision decision) throws MaltChainedException;
	public FeatureVector extract(FeatureModel featureModel) throws MaltChainedException;
	public boolean predictFromKBestList(FeatureModel featureModel, GuideDecision decision) throws MaltChainedException;
	
	public GuideMode getGuideMode();
}
