package org.maltparser.ml.lib;

public interface MaltLibModel {
	public int[] predict(MaltFeatureNode[] x);
	public int predict_one(MaltFeatureNode[] x);
}
