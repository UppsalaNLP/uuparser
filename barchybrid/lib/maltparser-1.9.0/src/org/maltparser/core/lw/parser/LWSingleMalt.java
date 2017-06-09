package org.maltparser.core.lw.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.maltparser.concurrent.graph.dataformat.DataFormat;
import org.maltparser.core.config.ConfigurationException;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureModelManager;
import org.maltparser.core.io.dataformat.DataFormatInstance;
import org.maltparser.core.lw.graph.LWDependencyGraph;
import org.maltparser.core.lw.graph.LWDeprojectivizer;
import org.maltparser.core.options.OptionManager;
import org.maltparser.core.propagation.PropagationManager;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.symbol.parse.ParseSymbolTableHandler;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.parser.AbstractParserFactory;
import org.maltparser.parser.DependencyParserConfig;

/**
 *  A lightweight version of org.maltparser.parser.SingleMalt. This class can only perform parsing and is used by 
 *  the concurrent MaltParser model. 
 * 
 * @author Johan Hall
 *
 */
public final class LWSingleMalt implements DependencyParserConfig {
	public final static Class<?>[] paramTypes = { org.maltparser.parser.DependencyParserConfig.class };
	private final McoModel mcoModel;
	private final int optionContainerIndex;
	private final DataFormatInstance dataFormatInstance;
	private final PropagationManager propagationManager;
	private final FeatureModelManager featureModelManager;
	private final AbstractParserFactory parserFactory;
	private final String decisionSettings;
	private final int kBestSize;
	private final String classitem_separator;
	private final URL featureModelURL;
	private final String dataSplitColumn;
	private final String dataSplitStructure;
	private final boolean excludeNullValues; 
	private final LWDecisionModel decisionModel;
	
	public LWSingleMalt(int containerIndex, DataFormatInstance dataFormatInstance, McoModel _mcoModel, PropagationManager _propagationManager, FeatureModelManager _featureModelManager) throws MaltChainedException {
		this.optionContainerIndex = containerIndex;
		this.mcoModel = _mcoModel;
		this.dataFormatInstance = dataFormatInstance;
		this.propagationManager = _propagationManager;
		this.featureModelManager = _featureModelManager;
		this.parserFactory = makeParserFactory();
		this.decisionSettings = getOptionValue("guide", "decision_settings").toString().trim();
		this.kBestSize = ((Integer)getOptionValue("guide", "kbest")).intValue();
		this.classitem_separator = getOptionValue("guide", "classitem_separator").toString().trim();
		this.featureModelURL = getConfigFileEntryURL(getOptionValue("guide", "features").toString().trim());
		this.dataSplitColumn = getOptionValue("guide", "data_split_column").toString().trim();
		this.dataSplitStructure = getOptionValue("guide", "data_split_structure").toString().trim();
		this.excludeNullValues = getOptionValue("singlemalt", "null_value").toString().equalsIgnoreCase("none");
		this.decisionModel = new LWDecisionModel(mcoModel, excludeNullValues, getOptionValueString("guide","learner"));
	}
	
	private AbstractParserFactory makeParserFactory() throws MaltChainedException {
		Class<?> clazz = (Class<?>)getOptionValue("singlemalt", "parsing_algorithm");
		try {	
			Object[] arguments = { this };
			return (AbstractParserFactory)clazz.getConstructor(paramTypes).newInstance(arguments);
		} catch (NoSuchMethodException e) {
			throw new ConfigurationException("The parser factory '"+clazz.getName()+"' cannot be initialized. ", e);
		} catch (InstantiationException e) {
			throw new ConfigurationException("The parser factory '"+clazz.getName()+"' cannot be initialized. ", e);
		} catch (IllegalAccessException e) {
			throw new ConfigurationException("The parser factory '"+clazz.getName()+"' cannot be initialized. ", e);
		} catch (InvocationTargetException e) {
			throw new ConfigurationException("The parser factory '"+clazz.getName()+"' cannot be initialized. ", e);			
		}
	}
	public FeatureModelManager getFeatureModelManager() {
		return featureModelManager;
	}
	
	public AbstractParserFactory getParserFactory() {
		return parserFactory;
	}

	public void parse(DependencyStructure graph) throws MaltChainedException {
		if (graph.hasTokens()) {
			LWDeterministicParser parser = new LWDeterministicParser(this, graph.getSymbolTables());
			parser.parse(graph);
		}
	}
	
    public List<String[]> parseSentences(List<String[]> inputSentences, String defaultRootLabel, int markingStrategy, boolean coveredRoot, SymbolTableHandler parentSymbolTableHandler, DataFormat concurrentDataFormat) throws MaltChainedException {
    	List<String[]> outputSentences = Collections.synchronizedList(new ArrayList<String[]>());
    	SymbolTableHandler parseSymbolTableHandler = new ParseSymbolTableHandler(parentSymbolTableHandler);
    	LWDependencyGraph parseGraph = new LWDependencyGraph(concurrentDataFormat, parseSymbolTableHandler);
    	LWDeterministicParser parser = new LWDeterministicParser(this, parseSymbolTableHandler);
    	
		for (int i = 0; i < inputSentences.size(); i++) {
			String[] tokens = inputSentences.get(i);
			// TODO nothing to parse
			parseGraph.resetTokens(tokens, defaultRootLabel, false);
			parser.parse(parseGraph);
			if (markingStrategy != 0 || coveredRoot) { 
				new LWDeprojectivizer().deprojectivize(parseGraph, markingStrategy);
			}
			String[] outputTokens = new String[tokens.length];
			for (int j = 0; j < outputTokens.length; j++) {
				outputTokens[j] = parseGraph.getDependencyNode(j+1).toString();
			}
			outputSentences.add(outputTokens);
		}
		return outputSentences;
    }
	
	public void oracleParse(DependencyStructure goldGraph, DependencyStructure oracleGraph) throws MaltChainedException {}
	
	public void terminate(Object[] arguments) throws MaltChainedException {}
	
	public boolean isLoggerInfoEnabled() {
		return false;
	}
	public boolean isLoggerDebugEnabled() {
		return false;
	}
	public void logErrorMessage(String message) {}
	public void logInfoMessage(String message) {}
	public void logInfoMessage(char character) {}
	public void logDebugMessage(String message) {}
	public void writeInfoToConfigFile(String message) throws MaltChainedException {}
	
	public OutputStreamWriter getOutputStreamWriter(String fileName) throws MaltChainedException {
		return null;
	}
	
	public OutputStreamWriter getAppendOutputStreamWriter(String fileName) throws MaltChainedException {
		return null;
	}
	
	public InputStreamReader getInputStreamReader(String fileName) throws MaltChainedException {
		try {
			return mcoModel.getInputStreamReader(fileName, "UTF-8");
		} catch (IOException e) {
			throw new ConfigurationException("Couldn't read file "+fileName+" from mco-file ", e);
		}
	}
	
	public InputStream getInputStreamFromConfigFileEntry(String fileName) throws MaltChainedException {
		try {
			return mcoModel.getInputStream(fileName);
		} catch (IOException e) {
			throw new ConfigurationException("Couldn't read file "+fileName+" from mco-file ", e);
		}
	}
	
	public URL getConfigFileEntryURL(String fileName) throws MaltChainedException {
		try {
			return mcoModel.getMcoEntryURL(fileName);
		} catch (IOException e) {
			throw new ConfigurationException("Couldn't read file "+fileName+" from mco-file ", e);
		}
	}
	
	public Object getConfigFileEntryObject(String fileName) throws MaltChainedException {
		return mcoModel.getMcoEntryObject(fileName);
	}
	
	public String getConfigFileEntryString(String fileName) throws MaltChainedException {
		return mcoModel.getMcoEntryString(fileName);
	}
	
	public File getFile(String fileName) throws MaltChainedException {
		return new File(System.getProperty("user.dir")+File.separator+fileName);
	}

	public Object getOptionValue(String optiongroup, String optionname) throws MaltChainedException {
		return OptionManager.instance().getOptionValue(optionContainerIndex, optiongroup, optionname);
	}
	
	public String getOptionValueString(String optiongroup, String optionname) throws MaltChainedException {
		return OptionManager.instance().getOptionValueString(optionContainerIndex, optiongroup, optionname);
	}

	public SymbolTableHandler getSymbolTables() {
		return null;
	}
	
	public DataFormatInstance getDataFormatInstance() {
		return dataFormatInstance;
	}
	
	public PropagationManager getPropagationManager() {
		return propagationManager;
	}

	public String getDecisionSettings() {
		return decisionSettings;
	}

	public int getkBestSize() {
		return kBestSize;
	}

	public String getClassitem_separator() {
		return classitem_separator;
	}

	public URL getFeatureModelURL() {
		return featureModelURL;
	}

	public String getDataSplitColumn() {
		return dataSplitColumn;
	}

	public String getDataSplitStructure() {
		return dataSplitStructure;
	}

	public boolean isExcludeNullValues() {
		return excludeNullValues;
	}

	public LWDecisionModel getDecisionModel() {
		return decisionModel;
	}
}
