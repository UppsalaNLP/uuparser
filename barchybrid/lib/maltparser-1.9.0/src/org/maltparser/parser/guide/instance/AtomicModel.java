package org.maltparser.parser.guide.instance;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Formatter;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureModel;
import org.maltparser.core.feature.FeatureVector;
import org.maltparser.core.feature.function.FeatureFunction;
import org.maltparser.core.feature.function.Modifiable;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.ml.LearningMethod;
import org.maltparser.ml.lib.LibLinear;
import org.maltparser.ml.lib.LibSvm;
import org.maltparser.parser.guide.ClassifierGuide;
import org.maltparser.parser.guide.GuideException;
import org.maltparser.parser.guide.Model;
import org.maltparser.parser.history.action.SingleDecision;


/**

@author Johan Hall
*/
public class AtomicModel implements InstanceModel {
	public static final Class<?>[] argTypes = { org.maltparser.parser.guide.instance.InstanceModel.class, java.lang.Integer.class };
	private final Model parent;
	private final String modelName;
//	private final FeatureVector featureVector;
	private final int index;
	private final LearningMethod method;
	private int frequency = 0;

	
	/**
	 * Constructs an atomic model.
	 * 
	 * @param index the index of the atomic model (-1..n), where -1 is special value (used by a single model 
	 * or the master divide model) and n is number of divide models.
	 * @param parent the parent guide model.
	 * @throws MaltChainedException
	 */
	public AtomicModel(int index, Model parent) throws MaltChainedException {
		this.parent = parent;
		this.index = index;
		if (index == -1) {
			this.modelName = parent.getModelName()+".";
		} else {
			this.modelName = parent.getModelName()+"."+new Formatter().format("%03d", index)+".";
		}
//		this.featureVector = featureVector;
		this.frequency = 0;
		Integer learnerMode = null;
		if (getGuide().getGuideMode() == ClassifierGuide.GuideMode.CLASSIFY) {
			learnerMode = LearningMethod.CLASSIFY;
		} else if (getGuide().getGuideMode() == ClassifierGuide.GuideMode.BATCH) {
			learnerMode = LearningMethod.BATCH;
		}
		
		// start init learning method
		Class<?> clazz = (Class<?>)getGuide().getConfiguration().getOptionValue("guide", "learner");
		if (clazz == org.maltparser.ml.lib.LibSvm.class) {
			this.method = new LibSvm(this, learnerMode);
		} else if (clazz == org.maltparser.ml.lib.LibLinear.class) {
			this.method = new LibLinear(this, learnerMode);
		} else {
			Object[] arguments = {this, learnerMode};
			try {	
				Constructor<?> constructor = clazz.getConstructor(argTypes);
				this.method = (LearningMethod)constructor.newInstance(arguments);
			} catch (NoSuchMethodException e) {
				throw new GuideException("The learner class '"+clazz.getName()+"' cannot be initialized. ", e);
			} catch (InstantiationException e) {
				throw new GuideException("The learner class '"+clazz.getName()+"' cannot be initialized. ", e);
			} catch (IllegalAccessException e) {
				throw new GuideException("The learner class '"+clazz.getName()+"' cannot be initialized. ", e);
			} catch (InvocationTargetException e) {
				throw new GuideException("The learner class '"+clazz.getName()+"' cannot be initialized. ", e);
			}
		}
		// end init learning method
		
		if (learnerMode == LearningMethod.BATCH && index == -1 && getGuide().getConfiguration() != null) {
			getGuide().getConfiguration().writeInfoToConfigFile(method.toString());
		}
	}
	
	public void addInstance(FeatureVector featureVector, SingleDecision decision) throws MaltChainedException {
		try {
			method.addInstance(decision, featureVector);
		} catch (NullPointerException e) {
			throw new GuideException("The learner cannot be found. ", e);
		}
	}

	
	public void noMoreInstances(FeatureModel featureModel) throws MaltChainedException {
		try {
			method.noMoreInstances();
		} catch (NullPointerException e) {
			throw new GuideException("The learner cannot be found. ", e);
		}
	}
	
	public void finalizeSentence(DependencyStructure dependencyGraph) throws MaltChainedException {
		try {
			method.finalizeSentence(dependencyGraph);
		} catch (NullPointerException e) {
			throw new GuideException("The learner cannot be found. ", e);
		}
	}

	public boolean predict(FeatureVector featureVector, SingleDecision decision) throws MaltChainedException {
		try {
			return method.predict(featureVector, decision);
		} catch (NullPointerException e) {
			throw new GuideException("The learner cannot be found. ", e);
		}
	}

	public FeatureVector predictExtract(FeatureVector featureVector, SingleDecision decision) throws MaltChainedException {
		try {

			if (method.predict(featureVector, decision)) {
				return featureVector;
			}
			return null;
		} catch (NullPointerException e) {
			throw new GuideException("The learner cannot be found. ", e);
		}
	}
	
	public FeatureVector extract(FeatureVector featureVector) throws MaltChainedException {
		return featureVector;
	}
	
	public void terminate() throws MaltChainedException {
		if (method != null) {
			method.terminate();
		}
	}
	
	/**
	 * Moves all instance from this atomic model into the destination atomic model and add the divide feature.
	 * This method is used by the feature divide model to sum up all model below a certain threshold.
	 * 
	 * @param model the destination atomic model 
	 * @param divideFeature the divide feature
	 * @param divideFeatureIndexVector the divide feature index vector
	 * @throws MaltChainedException
	 */
	public void moveAllInstances(AtomicModel model, FeatureFunction divideFeature, ArrayList<Integer> divideFeatureIndexVector) throws MaltChainedException {
		if (method == null) {
			throw new GuideException("The learner cannot be found. ");
		} else if (model == null) {
			throw new GuideException("The guide model cannot be found. ");
		} else if (divideFeature == null) {
			throw new GuideException("The divide feature cannot be found. ");
		} else if (divideFeatureIndexVector == null) {
			throw new GuideException("The divide feature index vector cannot be found. ");
		}
		((Modifiable)divideFeature).setFeatureValue(index);
		method.moveAllInstances(model.getMethod(), divideFeature, divideFeatureIndexVector);
		method.terminate();
	}
	
	/**
	 * Invokes the train() of the learning method 
	 * 
	 * @throws MaltChainedException
	 */
	public void train() throws MaltChainedException {
		try {
			method.train();
			method.terminate();
		} catch (NullPointerException e) {	
			throw new GuideException("The learner cannot be found. ", e);
		}
		

	}
	
	/**
	 * Returns the parent guide model
	 * 
	 * @return the parent guide model
	 */
	public Model getParent() throws MaltChainedException {
		if (parent == null) {
			throw new GuideException("The atomic model can only be used by a parent model. ");
		}
		return parent;
	}


	public String getModelName() {
		return modelName;
	}

	/**
	 * Returns the feature vector used by this atomic model
	 * 
	 * @return a feature vector object
	 */
//	public FeatureVector getFeatures() {
//		return featureVector;
//	}

	public ClassifierGuide getGuide() {
		return parent.getGuide();
	}
	
	/**
	 * Returns the index of the atomic model
	 * 
	 * @return the index of the atomic model
	 */
	public int getIndex() {
		return index;
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
	
	/**
	 * Returns a learner object
	 * 
	 * @return a learner object
	 */
	public LearningMethod getMethod() {
		return method;
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(method.toString());
		return sb.toString();
	}
}
