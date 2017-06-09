package org.maltparser.parser.guide.instance;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.SortedMap;

import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureModel;
import org.maltparser.core.feature.FeatureVector;
import org.maltparser.core.feature.value.SingleFeatureValue;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.parser.guide.ClassifierGuide;
import org.maltparser.parser.guide.GuideException;
import org.maltparser.parser.guide.Model;
import org.maltparser.parser.history.action.SingleDecision;

/**
The feature divide model is used for divide the training instances into several models according to
a divide feature. Usually this strategy decrease the training and classification time, but can also decrease 
the accuracy of the parser.  

@author Johan Hall
*/
public class FeatureDivideModel implements InstanceModel {
	private final Model parent;
	private final SortedMap<Integer,AtomicModel> divideModels;
//	private FeatureVector masterFeatureVector;
	private int frequency = 0;
	private final int divideThreshold;
	private AtomicModel masterModel;
	
	/**
	 * Constructs a feature divide model.
	 * 
	 * @param parent the parent guide model.
	 * @throws MaltChainedException
	 */
	public FeatureDivideModel(Model parent) throws MaltChainedException {
		this.parent = parent;
		setFrequency(0);
//		this.masterFeatureVector = featureVector;

		String data_split_threshold = getGuide().getConfiguration().getOptionValue("guide", "data_split_threshold").toString().trim();
		if (data_split_threshold != null) {
			try {
				divideThreshold = Integer.parseInt(data_split_threshold);
			} catch (NumberFormatException e) {
				throw new GuideException("The --guide-data_split_threshold option is not an integer value. ", e);
			}
		} else {
			divideThreshold = 0;
		}
		divideModels = new TreeMap<Integer,AtomicModel>();
		if (getGuide().getGuideMode() == ClassifierGuide.GuideMode.BATCH) {
			masterModel = new AtomicModel(-1, this);
		} else if (getGuide().getGuideMode() == ClassifierGuide.GuideMode.CLASSIFY) {
			load();
		}
	}
	
	public void addInstance(FeatureVector featureVector, SingleDecision decision) throws MaltChainedException {
//		featureVector.getFeatureModel().getDivideFeatureFunction().update();
		SingleFeatureValue featureValue = (SingleFeatureValue)featureVector.getFeatureModel().getDivideFeatureFunction().getFeatureValue();
		if (!divideModels.containsKey(featureValue.getIndexCode())) {
			divideModels.put(featureValue.getIndexCode(), new AtomicModel(featureValue.getIndexCode(), this));
		}
		FeatureVector divideFeatureVector = featureVector.getFeatureModel().getFeatureVector("/" + featureVector.getSpecSubModel().getSubModelName());
		divideModels.get(featureValue.getIndexCode()).addInstance(divideFeatureVector, decision);
	}
	
	public void noMoreInstances(FeatureModel featureModel) throws MaltChainedException {
		for (Integer index : divideModels.keySet()) {
			divideModels.get(index).noMoreInstances(featureModel);
		}
		final TreeSet<Integer> removeSet = new TreeSet<Integer>();
		for (Integer index : divideModels.keySet()) {
			if (divideModels.get(index).getFrequency() <= divideThreshold) {
				divideModels.get(index).moveAllInstances(masterModel, featureModel.getDivideFeatureFunction(), featureModel.getDivideFeatureIndexVector());
				removeSet.add(index);
			}
		}
		for (Integer index : removeSet) {
			divideModels.remove(index);
		}
		masterModel.noMoreInstances(featureModel);
	}

	public void finalizeSentence(DependencyStructure dependencyGraph) throws MaltChainedException {
		if (divideModels != null) { 
			for (AtomicModel divideModel : divideModels.values()) {
				divideModel.finalizeSentence(dependencyGraph);
			}
		} else {
			throw new GuideException("The feature divide models cannot be found. ");
		}
	}

	public boolean predict(FeatureVector featureVector, SingleDecision decision) throws MaltChainedException {
		AtomicModel model = getAtomicModel((SingleFeatureValue)featureVector.getFeatureModel().getDivideFeatureFunction().getFeatureValue());
		if (model == null) {
			if (getGuide().getConfiguration().isLoggerInfoEnabled()) {
				getGuide().getConfiguration().logInfoMessage("Could not predict the next parser decision because there is " +
						"no divide or master model that covers the divide value '"+((SingleFeatureValue)featureVector.getFeatureModel().getDivideFeatureFunction().getFeatureValue()).getIndexCode()+"', as default" +
								" class code '1' is used. ");
			}
			decision.addDecision(1); // default prediction
			return true;
		}
		return model.predict(getModelFeatureVector(model, featureVector), decision);
	}

	public FeatureVector predictExtract(FeatureVector featureVector, SingleDecision decision) throws MaltChainedException {
		AtomicModel model = getAtomicModel((SingleFeatureValue)featureVector.getFeatureModel().getDivideFeatureFunction().getFeatureValue());
		if (model == null) {
			return null;
		}
		return model.predictExtract(getModelFeatureVector(model, featureVector), decision);
	}
	
	public FeatureVector extract(FeatureVector featureVector) throws MaltChainedException {
		AtomicModel model = getAtomicModel((SingleFeatureValue)featureVector.getFeatureModel().getDivideFeatureFunction().getFeatureValue());
		if (model == null) {
			return featureVector;
		}
		return model.extract(getModelFeatureVector(model, featureVector));
	}
	
	private FeatureVector getModelFeatureVector(AtomicModel model, FeatureVector featureVector) {
		if (model.getIndex() == -1) {
			return featureVector;
		} else {
			return featureVector.getFeatureModel().getFeatureVector("/" + featureVector.getSpecSubModel().getSubModelName());
		}
	}
	
	private AtomicModel getAtomicModel(SingleFeatureValue featureValue) throws MaltChainedException {
		//((SingleFeatureValue)masterFeatureVector.getFeatureModel().getDivideFeatureFunction().getFeatureValue()).getIndexCode()
		if (divideModels != null && divideModels.containsKey(featureValue.getIndexCode())) {
			return divideModels.get(featureValue.getIndexCode());
		} else if (masterModel != null && masterModel.getFrequency() > 0) {
			return masterModel;
		} 
		return null;
	}
	
	public void terminate() throws MaltChainedException {
		if (divideModels != null) {
			for (AtomicModel divideModel : divideModels.values()) {	
				divideModel.terminate();
			}
		}
		if (masterModel != null) {
			masterModel.terminate();
		}
	}
	
	public void train() throws MaltChainedException {
		for (AtomicModel divideModel : divideModels.values()) {
			divideModel.train();
		}
		masterModel.train();
		save();
		for (AtomicModel divideModel : divideModels.values()) {
			divideModel.terminate();
		}
		masterModel.terminate();
	}
	
	/**
	 * Saves the feature divide model settings .dsm file.
	 * 
	 * @throws MaltChainedException
	 */
	protected void save() throws MaltChainedException {
		try {
			final BufferedWriter out = new BufferedWriter(getGuide().getConfiguration().getOutputStreamWriter(getModelName()+".dsm"));
			out.write(masterModel.getIndex() + "\t" + masterModel.getFrequency() + "\n");

			if (divideModels != null) {
				for (AtomicModel divideModel : divideModels.values()) {
					out.write(divideModel.getIndex() + "\t" + divideModel.getFrequency() + "\n");
	        	}
			}
			out.close();
		} catch (IOException e) {
			throw new GuideException("Could not write to the guide model settings file '"+getModelName()+".dsm"+"', when " +
					"saving the guide model settings to file. ", e);
		}
	}
	
	protected void load() throws MaltChainedException {
		String dsmString = getGuide().getConfiguration().getConfigFileEntryString(getModelName()+".dsm");
		String[] lines = dsmString.split("\n");
		Pattern tabPattern = Pattern.compile("\t");
//		FeatureVector divideFeatureVector = featureVector.getFeatureModel().getFeatureVector("/" + featureVector.getSpecSubModel().getSubModelName());
		for (int i = 0; i < lines.length; i++) {
			String[] cols = tabPattern.split(lines[i]);
			if (cols.length != 2) { 
				throw new GuideException("");
			}
			int code = -1;
			int freq = 0;
			try {
				code = Integer.parseInt(cols[0]);
				freq = Integer.parseInt(cols[1]);
			} catch (NumberFormatException e) {
				throw new GuideException("Could not convert a string value into an integer value when loading the feature divide model settings (.dsm). ", e);
			}
			if (code == -1) { 
				masterModel = new AtomicModel(-1, this);
				masterModel.setFrequency(freq);
			} else if (divideModels != null) {
				divideModels.put(code, new AtomicModel(code, this));
				divideModels.get(code).setFrequency(freq);
			}
			setFrequency(getFrequency()+freq);
		}
	}
	
	/**
	 * Returns the parent model
	 * 
	 * @return the parent model
	 */
	public Model getParent() {
		return parent;
	}

	public ClassifierGuide getGuide() {
		return parent.getGuide();
	}
	
	public String getModelName() throws MaltChainedException {
		try {
			return parent.getModelName();
		} catch (NullPointerException e) {
			throw new GuideException("The parent guide model cannot be found. ", e);
		}
	}
	
	/**
	 * Returns the frequency (number of instances)
	 * 
	 * @return the frequency (number of instances)
	 */
	public int getFrequency() {
		return frequency;
	}

	/**
	 * Increase the frequency by 1
	 */
	public void increaseFrequency() {
		if (parent instanceof InstanceModel) {
			((InstanceModel)parent).increaseFrequency();
		}
		frequency++;
	}
	
	public void decreaseFrequency() {
		if (parent instanceof InstanceModel) {
			((InstanceModel)parent).decreaseFrequency();
		}
		frequency--;
	}
	
	/**
	 * Sets the frequency (number of instances)
	 * 
	 * @param frequency (number of instances)
	 */
	protected void setFrequency(int frequency) {
		this.frequency = frequency;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		//TODO
		return sb.toString();
	}
}
