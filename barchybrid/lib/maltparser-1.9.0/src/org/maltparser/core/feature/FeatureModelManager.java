package org.maltparser.core.feature;

import java.net.URL;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.spec.SpecificationModel;
import org.maltparser.core.feature.spec.SpecificationModels;
import org.maltparser.core.feature.system.FeatureEngine;

/**
*
*
* @author Johan Hall
*/
public class FeatureModelManager {
	private final SpecificationModels specModels;
	private final FeatureEngine featureEngine;

	
	public FeatureModelManager(FeatureEngine engine) throws MaltChainedException {
		specModels = new SpecificationModels();
		this.featureEngine = engine;
	}
	
	public void loadSpecification(URL specModelURL) throws MaltChainedException {
		specModels.load(specModelURL);
	}
	
	public void loadParSpecification(URL specModelURL, String markingStrategy, String coveredRoot) throws MaltChainedException {
		specModels.loadParReader(specModelURL, markingStrategy, coveredRoot);
	}
	
	
	public FeatureModel getFeatureModel(URL specModelURL, int specModelUrlIndex, FeatureRegistry registry, String dataSplitColumn, String dataSplitStructure) throws MaltChainedException {
		return new FeatureModel(specModels.getSpecificationModel(specModelURL, specModelUrlIndex), registry, featureEngine, dataSplitColumn, dataSplitStructure);
	}
	
	public FeatureModel getFeatureModel(URL specModelURL, FeatureRegistry registry, String dataSplitColumn, String dataSplitStructure) throws MaltChainedException {
		return new FeatureModel(specModels.getSpecificationModel(specModelURL, 0), registry, featureEngine, dataSplitColumn, dataSplitStructure);
	}
	
	public FeatureModel getFeatureModel(SpecificationModel specModel, FeatureRegistry registry, String dataSplitColumn, String dataSplitStructure) throws MaltChainedException {
		return new FeatureModel(specModel, registry, featureEngine, dataSplitColumn, dataSplitStructure);
	}
	
	public SpecificationModels getSpecModels() {
		return specModels;
	}
	
	public FeatureEngine getFeatureEngine() {
		return featureEngine;
	}


	public String toString() {
		return specModels.toString();
	}
}
