package org.maltparser.ml.lib;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;

import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;

import org.maltparser.core.config.Configuration;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.helper.NoPrintStream;
import org.maltparser.core.helper.Util;
import org.maltparser.ml.lib.FeatureList;
import org.maltparser.ml.lib.MaltLiblinearModel;
import org.maltparser.ml.lib.MaltFeatureNode;
import org.maltparser.ml.lib.LibException;
import org.maltparser.parser.guide.instance.InstanceModel;

public class LibLinear extends Lib {
	
	public LibLinear(InstanceModel owner, Integer learnerMode) throws MaltChainedException {
		super(owner, learnerMode, "liblinear");
		if (learnerMode == CLASSIFY) {
			model = (MaltLibModel)getConfigFileEntryObject(".moo");
		}
	}
	
	protected void trainInternal( LinkedHashMap<String, String> libOptions) throws MaltChainedException {
		Configuration config = getConfiguration();
		
		if (config.isLoggerInfoEnabled()) {
			config.logInfoMessage("Creating Liblinear model "+getFile(".moo").getName()+"\n");
		}
		double[] wmodel = null;
		int[] labels = null;
		int nr_class = 0;
		int nr_feature = 0;
		Parameter parameter = getLiblinearParameters(libOptions);
		try {	
			Problem problem = readProblem(getInstanceInputStreamReader(".ins"));
			boolean res = checkProblem(problem);
			if (res == false) {
				throw new LibException("Abort (The number of training instances * the number of classes) > "+Integer.MAX_VALUE+" and this is not supported by LibLinear. ");
			}
			if (config.isLoggerInfoEnabled()) {
				config.logInfoMessage("- Train a parser model using LibLinear.\n");
			}
			final PrintStream out = System.out;
			final PrintStream err = System.err;
			System.setOut(NoPrintStream.NO_PRINTSTREAM);
			System.setErr(NoPrintStream.NO_PRINTSTREAM);
			Model model = Linear.train(problem, parameter);
			System.setOut(err);
			System.setOut(out);
			problem = null;
			wmodel = model.getFeatureWeights();
			labels = model.getLabels();
			nr_class = model.getNrClass();
			nr_feature = model.getNrFeature();
			boolean saveInstanceFiles = ((Boolean)getConfiguration().getOptionValue("lib", "save_instance_files")).booleanValue();
			if (!saveInstanceFiles) {
				getFile(".ins").delete();
			}
		} catch (OutOfMemoryError e) {
			throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
		} catch (IllegalArgumentException e) {
			throw new LibException("The Liblinear learner was not able to redirect Standard Error stream. ", e);
		} catch (SecurityException e) {
			throw new LibException("The Liblinear learner cannot remove the instance file. ", e);
		} catch (NegativeArraySizeException e) {
			throw new LibException("(The number of training instances * the number of classes) > "+Integer.MAX_VALUE+" and this is not supported by LibLinear.", e);
		}
		
		if (config.isLoggerInfoEnabled()) {
			config.logInfoMessage("- Optimize the memory usage\n");
		}
		MaltLiblinearModel xmodel = null;
		try {
//			System.out.println("Nr Features:" +  nr_feature);
//			System.out.println("nr_class:" + nr_class);
//			System.out.println("wmodel.length:" + wmodel.length);		
			double[][] wmatrix = convert2(wmodel, nr_class, nr_feature);
			xmodel = new MaltLiblinearModel(labels, nr_class, wmatrix.length, wmatrix, parameter.getSolverType());
			if (config.isLoggerInfoEnabled()) {
				config.logInfoMessage("- Save the Liblinear model "+getFile(".moo").getName()+"\n");
			}
		} catch (OutOfMemoryError e) {
			throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
		}			
		try {
			if (xmodel != null) {
			    ObjectOutputStream output = new ObjectOutputStream (new BufferedOutputStream(new FileOutputStream(getFile(".moo").getAbsolutePath())));
		        try{
		          output.writeObject(xmodel);
		        } finally {
		          output.close();
		        }
			}
		} catch (OutOfMemoryError e) {
			throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
		} catch (IllegalArgumentException e) {
			throw new LibException("The Liblinear learner was not able to redirect Standard Error stream. ", e);
		} catch (SecurityException e) {
			throw new LibException("The Liblinear learner cannot remove the instance file. ", e);
		} catch (IOException e) {
			throw new LibException("The Liblinear learner cannot save the model file '"+getFile(".mod").getAbsolutePath()+"'. ", e);
		}
	}
	
    private double[][] convert2(double[] w, int nr_class, int nr_feature) {
        int[] wlength = new int[nr_feature];
        int nr_nfeature = 0;
//        int ne = 0;
//        int nr = 0;
//        int no = 0;
//        int n = 0;
        
        // Identify length of new weight array for each feature
        for (int i = 0; i < nr_feature; i++) {
        	int k = nr_class;       	
        	for (int t = i * nr_class; (t + (k - 1)) >= t; k--) {
        		if (w[t + k - 1] != 0.0) {
        			break;
        		}
        	}
        	int b = k;
        	if (b != 0) {
	        	for (int t = i * nr_class; (t + (b - 1)) >= t; b--) {
	        		if (b != k) {
	        			if (w[t + b - 1] != w[t + b]) {
	        				break;
	        			}
	        		}
	        	}
        	}
        	if (k == 0 || b == 0) {
        		wlength[i] = 0;
        	} else {
        		wlength[i] = k;
        		nr_nfeature++;
        	}        	
        }
        // Allocate the weight matrix with the new number of features and
        // an array wsignature that efficient compare if weight vector can be reused by another feature. 
        double[][] wmatrix = new double[nr_nfeature][];
        double[] wsignature = new double[nr_nfeature];
        Long[] reverseMap = featureMap.reverseMap();
        int in = 0;
        for (int i = 0; i < nr_feature; i++) {
            if (wlength[i] == 0) {
            	// if the length of the weight vector is zero than eliminate the feature from the feature map.
//            	ne++;
            	featureMap.removeIndex(reverseMap[i + 1]);
            	reverseMap[i + 1] = null;
            } else {          	
            	boolean reuse = false;
            	double[] copy = new double[wlength[i]];
            	System.arraycopy(w, i * nr_class, copy, 0, wlength[i]);
            	featureMap.setIndex(reverseMap[i + 1], in + 1);
            	for (int j=0; j<copy.length; j++) wsignature[in] += copy[j];
	            for (int j = 0; j < in; j++) {
	            	if (wsignature[j] == wsignature[in]) {
	            		// if the signatures is equal then do more narrow comparison  
		            	if (Util.equals(copy, wmatrix[j])) {
		            		// if equal then reuse the weight vector
		            		wmatrix[in] = wmatrix[j];
		            		reuse = true;
//		            		nr++;
		            		break;
		            	}
	            	}
	            }
	            if (reuse == false) {
	                // if no reuse has done use the new weight vector in the weight matrix 
//	            	no++;
	            	wmatrix[in] = copy;
	            }
	            in++;
            }
//            n++;
        }
        featureMap.setFeatureCounter(nr_nfeature);
//        System.out.println("NE:"+ne);
//        System.out.println("NR:"+nr);
//        System.out.println("NO:"+no);
//        System.out.println("N :"+n);
        return wmatrix;
    }
    
    public static boolean eliminate(double[] a) {
    	if (a.length == 0) {
    		return true;
    	}
    	for (int i = 1; i < a.length; i++) {
    		if (a[i] != a[i-1]) {
    			return false;
    		}
    	}
    	return true;
    }
    
	protected void trainExternal(String pathExternalTrain, LinkedHashMap<String, String> libOptions) throws MaltChainedException {
		try {		
			Configuration config = getConfiguration();
			if (config.isLoggerInfoEnabled()) {
				config.logInfoMessage("Creating liblinear model (external) "+getFile(".mod").getName());
			}
			binariesInstances2SVMFileFormat(getInstanceInputStreamReader(".ins"), getInstanceOutputStreamWriter(".ins.tmp"));
			final String[] params = getLibParamStringArray(libOptions);
			String[] arrayCommands = new String[params.length+3];
			int i = 0;
			arrayCommands[i++] = pathExternalTrain;
			for (; i <= params.length; i++) {
				arrayCommands[i] = params[i-1];
			}
			arrayCommands[i++] = getFile(".ins.tmp").getAbsolutePath();
			arrayCommands[i++] = getFile(".mod").getAbsolutePath();
			
	        if (verbosity == Verbostity.ALL) {
	        	config.logInfoMessage('\n');
	        }
			final Process child = Runtime.getRuntime().exec(arrayCommands);
	        final InputStream in = child.getInputStream();
	        final InputStream err = child.getErrorStream();
	        int c;
	        while ((c = in.read()) != -1){
	        	if (verbosity == Verbostity.ALL) {
	        		config.logInfoMessage((char)c);
	        	}
	        }
	        while ((c = err.read()) != -1){
	        	if (verbosity == Verbostity.ALL || verbosity == Verbostity.ERROR) {
	        		config.logInfoMessage((char)c);
	        	}
	        }
            if (child.waitFor() != 0) {
            	config.logErrorMessage(" FAILED ("+child.exitValue()+")");
            }
	        in.close();
	        err.close();
			if (config.isLoggerInfoEnabled()) {
				config.logInfoMessage("\nSaving Liblinear model "+getFile(".moo").getName()+"\n");
			}
			MaltLiblinearModel xmodel = new MaltLiblinearModel(getFile(".mod"));
		    ObjectOutputStream output = new ObjectOutputStream (new BufferedOutputStream(new FileOutputStream(getFile(".moo").getAbsolutePath())));
	        try{
	          output.writeObject(xmodel);
	        } finally {
	          output.close();
	        }
	        boolean saveInstanceFiles = ((Boolean)getConfiguration().getOptionValue("lib", "save_instance_files")).booleanValue();
	        if (!saveInstanceFiles) {
				getFile(".ins").delete();
				getFile(".mod").delete();
				getFile(".ins.tmp").delete();
	        }
	        if (config.isLoggerInfoEnabled()) {
	        	config.logInfoMessage('\n');
	        }
		} catch (InterruptedException e) {
			 throw new LibException("Learner is interrupted. ", e);
		} catch (IllegalArgumentException e) {
			throw new LibException("The learner was not able to redirect Standard Error stream. ", e);
		} catch (SecurityException e) {
			throw new LibException("The learner cannot remove the instance file. ", e);
		} catch (IOException e) {
			throw new LibException("The learner cannot save the model file '"+getFile(".mod").getAbsolutePath()+"'. ", e);
		} catch (OutOfMemoryError e) {
			throw new LibException("Out of memory. Please increase the Java heap size (-Xmx<size>). ", e);
		}
	}
	
	public void terminate() throws MaltChainedException { 
		super.terminate();
	}

	public LinkedHashMap<String, String> getDefaultLibOptions() {
		LinkedHashMap<String, String> libOptions = new LinkedHashMap<String, String>();
		libOptions.put("s", "4"); // type = SolverType.MCSVM_CS (default)
		libOptions.put("c", "0.1"); // cost = 1 (default)
		libOptions.put("e", "0.1"); // epsilon = 0.1 (default)
		libOptions.put("B", "-1"); // bias = -1 (default)
		return libOptions;
	}
	
	public String getAllowedLibOptionFlags() {
		return "sceB";
	}
	
	private Problem readProblem(InputStreamReader isr) throws MaltChainedException {
		Problem problem = new Problem();
		final FeatureList featureList = new FeatureList();
		if (getConfiguration().isLoggerInfoEnabled()) {
			getConfiguration().logInfoMessage("- Read all training instances.\n");
		}
		try {
			final BufferedReader fp = new BufferedReader(isr);
			
			problem.bias = -1;
			problem.l = getNumberOfInstances();
			problem.x = new FeatureNode[problem.l][];
			problem.y = new int[problem.l];
			int i = 0;
			
			while(true) {
				String line = fp.readLine();
				if(line == null) break;
				int y = binariesInstance(line, featureList);
				if (y == -1) {
					continue;
				}
				try {
					problem.y[i] = y;
					problem.x[i] = new FeatureNode[featureList.size()];
					int p = 0;
			        for (int k=0; k < featureList.size(); k++) {
			        	MaltFeatureNode x = featureList.get(k);
						problem.x[i][p++] = new FeatureNode(x.getIndex(), x.getValue());
					}
					i++;
				} catch (ArrayIndexOutOfBoundsException e) {
					throw new LibException("Couldn't read liblinear problem from the instance file. ", e);
				}

			}
			fp.close();
			problem.n = featureMap.size();
		} catch (IOException e) {
			throw new LibException("Cannot read from the instance file. ", e);
		}
		
		return problem;
	}
	
	private boolean checkProblem(Problem problem) throws MaltChainedException {
		int max_y = problem.y[0];
		for (int i = 1; i < problem.y.length; i++) {
			if (problem.y[i] > max_y) {
				max_y = problem.y[i];
			}
		}
		if (max_y * problem.l < 0) { // max_y * problem.l > Integer.MAX_VALUE
			if (getConfiguration().isLoggerInfoEnabled()) {
				getConfiguration().logInfoMessage("*** Abort (The number of training instances * the number of classes) > Max array size: ("+problem.l+" * "+max_y+") > "+Integer.MAX_VALUE+" and this is not supported by LibLinear.\n");
			}
			return false;
		}
		return true;
	}
	
	private Parameter getLiblinearParameters(LinkedHashMap<String, String> libOptions) throws MaltChainedException {
		Parameter param = new Parameter(SolverType.MCSVM_CS, 0.1, 0.1);
		String type = libOptions.get("s");
		
		if (type.equals("0")) {
			param.setSolverType(SolverType.L2R_LR);
		} else if (type.equals("1")) {
			param.setSolverType(SolverType.L2R_L2LOSS_SVC_DUAL);
		} else if (type.equals("2")) {
			param.setSolverType(SolverType.L2R_L2LOSS_SVC);
		} else if (type.equals("3")) {
			param.setSolverType(SolverType.L2R_L1LOSS_SVC_DUAL);
		} else if (type.equals("4")) {
			param.setSolverType(SolverType.MCSVM_CS);
		} else if (type.equals("5")) {
			param.setSolverType(SolverType.L1R_L2LOSS_SVC);	
		} else if (type.equals("6")) {
			param.setSolverType(SolverType.L1R_LR);	
		} else if (type.equals("7")) {
			param.setSolverType(SolverType.L2R_LR_DUAL);	
		} else {
			throw new LibException("The liblinear type (-s) is not an integer value between 0 and 4. ");
		}
		try {
			param.setC(Double.valueOf(libOptions.get("c")).doubleValue());
		} catch (NumberFormatException e) {
			throw new LibException("The liblinear cost (-c) value is not numerical value. ", e);
		}
		try {
			param.setEps(Double.valueOf(libOptions.get("e")).doubleValue());
		} catch (NumberFormatException e) {
			throw new LibException("The liblinear epsilon (-e) value is not numerical value. ", e);
		}
		return param;
	}
}
