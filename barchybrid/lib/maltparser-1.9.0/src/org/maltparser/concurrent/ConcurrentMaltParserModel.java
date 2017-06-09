package org.maltparser.concurrent;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.maltparser.concurrent.graph.ConcurrentDependencyGraph;
import org.maltparser.concurrent.graph.dataformat.DataFormat;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureModelManager;
import org.maltparser.core.feature.system.FeatureEngine;
import org.maltparser.core.io.dataformat.DataFormatInstance;
import org.maltparser.core.io.dataformat.DataFormatManager;
import org.maltparser.core.lw.graph.LWDeprojectivizer;
import org.maltparser.core.lw.graph.LWDependencyGraph;
import org.maltparser.core.lw.parser.LWSingleMalt;
import org.maltparser.core.lw.parser.McoModel;
import org.maltparser.core.options.OptionManager;
import org.maltparser.core.plugin.PluginLoader;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.symbol.hash.HashSymbolTableHandler;
import org.maltparser.core.symbol.parse.ParseSymbolTableHandler;

/**
 * A concurrent MaltParser model that can be used to parse sentences in both a single threaded or multi threaded 
 * environment. To create an object of ConcurrentMaltParserModel use the static methods in ConcurrentMaltParserService.
 * 
 * @author Johan Hall
 *
 */
public final class ConcurrentMaltParserModel {
    private final DataFormatInstance dataFormatInstance;
    private final DataFormat concurrentDataFormat;
    private final SymbolTableHandler parentSymbolTableHandler;
    private final LWSingleMalt singleMalt;
    private final int optionContainer;
    private final McoModel mcoModel;    
    private final int markingStrategy; 
    private final boolean coveredRoot;
    private final String defaultRootLabel; 
    
    /**
     * This constructor can only be used by ConcurrentMaltParserService
     * 
     * @param _optionContainer a option container index
     * @param _mcoURL a URL to a valid MaltParser model file. 
     * @throws MaltChainedException
     */
    protected ConcurrentMaltParserModel(int _optionContainer, URL _mcoURL) throws MaltChainedException {
		this.optionContainer = _optionContainer;
		this.mcoModel = new McoModel(_mcoURL);
		String inputFormatName = OptionManager.instance().getOptionValue(optionContainer, "input", "format").toString().trim();	
		URL inputFormatURL = null;
		try {
			inputFormatURL = mcoModel.getMcoEntryURL(inputFormatName);
		} catch(IOException e) {
			throw new MaltChainedException("Couldn't read file "+inputFormatName+" from mco-file ", e);
		}
		DataFormatManager dataFormatManager = new DataFormatManager(inputFormatURL, inputFormatURL);
		this.parentSymbolTableHandler = new HashSymbolTableHandler();
		this.dataFormatInstance = dataFormatManager.getInputDataFormatSpec().createDataFormatInstance(this.parentSymbolTableHandler, OptionManager.instance().getOptionValueString(optionContainer, "singlemalt", "null_value"));
		try {
			this.parentSymbolTableHandler.load(mcoModel.getInputStreamReader("symboltables.sym", "UTF-8"));
		} catch(IOException e) {
			throw new MaltChainedException("Couldn't read file symboltables.sym from mco-file ", e);
		}
		this.defaultRootLabel = OptionManager.instance().getOptionValue(optionContainer, "graph", "root_label").toString().trim();	
		this.markingStrategy = LWDeprojectivizer.getMarkingStrategyInt(OptionManager.instance().getOptionValue(optionContainer, "pproj", "marking_strategy").toString().trim());
		this.coveredRoot = !OptionManager.instance().getOptionValue(optionContainer, "pproj", "covered_root").toString().trim().equalsIgnoreCase("none");
//		final PropagationManager propagationManager = loadPropagationManager(this.optionContainer, mcoModel);
		final FeatureModelManager featureModelManager = loadFeatureModelManager(this.optionContainer, mcoModel);
		this.singleMalt = new LWSingleMalt(this.optionContainer, this.dataFormatInstance, mcoModel, null, featureModelManager);
		this.concurrentDataFormat = DataFormat.parseDataFormatXMLfile(inputFormatURL);
    }
    
    /**
     * Parses an array of tokens and returns a dependency graph.
     * 
     * @param tokens an array of tokens
     * @return a dependency graph 
     * @throws MaltChainedException
     */
    public ConcurrentDependencyGraph parse(String[] tokens) throws MaltChainedException {
    	return new ConcurrentDependencyGraph(concurrentDataFormat, internalParse(tokens), defaultRootLabel);
    }
 
	/**
	 * Same as parse(String[] tokens), but instead it returns an array of tokens with a head index and a dependency type at the end of string
	 * 
	 * @param tokens an array of tokens to parse
	 * @return an array of tokens with a head index and a dependency type at the end of string 
	 * @throws MaltChainedException
	 */
	public String[] parseTokens(String[] tokens) throws MaltChainedException {
		LWDependencyGraph outputGraph = internalParse(tokens);
		String[] outputTokens = new String[tokens.length];
		for (int i = 0; i < outputTokens.length; i++) {
			outputTokens[i] = outputGraph.getDependencyNode(i+1).toString();
		}
		return outputTokens;
	}

    private LWDependencyGraph internalParse(String[] tokens) throws MaltChainedException {
		if (tokens == null || tokens.length == 0) {
		    throw new MaltChainedException("Nothing to parse. ");
		}

		LWDependencyGraph parseGraph = new LWDependencyGraph(concurrentDataFormat, new ParseSymbolTableHandler(parentSymbolTableHandler), tokens, defaultRootLabel, false);
		
		singleMalt.parse(parseGraph);
		if (markingStrategy != 0 || coveredRoot) { 
			new LWDeprojectivizer().deprojectivize(parseGraph, markingStrategy);
		}
		
		return parseGraph;
    }
	
    public List<String[]> parseSentences(List<String[]> inputSentences) throws MaltChainedException {
    	return singleMalt.parseSentences(inputSentences, defaultRootLabel, markingStrategy, coveredRoot, parentSymbolTableHandler, concurrentDataFormat);
//    	List<String[]> outputSentences = Collections.synchronizedList(new ArrayList<String[]>());;
//		for (int i = 0; i < inputSentences.size(); i++) {
//			String[] tokens = inputSentences.get(i);
//			// TODO nothing to parse
//			LWDependencyGraph parseGraph = new LWDependencyGraph(concurrentDataFormat, new ParseSymbolTableHandler(parentSymbolTableHandler), tokens, defaultRootLabel, false);
//			singleMalt.parse(parseGraph);
//			if (markingStrategy != 0 || coveredRoot) { 
//				new LWDeprojectivizer().deprojectivize(parseGraph, markingStrategy);
//			}
//			String[] outputTokens = new String[tokens.length];
//			for (int j = 0; j < outputTokens.length; j++) {
//				outputTokens[i] = parseGraph.getDependencyNode(j+1).toString();
//			}
//			outputSentences.add(outputTokens);
//		}
//		return outputSentences;
    }
    
    
//	private PropagationManager loadPropagationManager(int optionContainer, McoModel mcoModel) throws MaltChainedException {
//		String propagationSpecFileName = OptionManager.instance().getOptionValue(optionContainer, "singlemalt", "propagation").toString();
//		PropagationManager propagationManager = null;
//		if (propagationSpecFileName != null && propagationSpecFileName.length() > 0) {
//			propagationManager = new PropagationManager();
//			try {
//				propagationManager.loadSpecification(mcoModel.getMcoEntryURL(propagationSpecFileName));
//			} catch(IOException e) {
//				throw new MaltChainedException("Couldn't read file "+propagationSpecFileName+" from mco-file ", e);
//			}
//		}
//		return propagationManager;
//	}
	
	private FeatureModelManager loadFeatureModelManager(int optionContainer, McoModel mcoModel) throws MaltChainedException {
		final FeatureEngine system = new FeatureEngine();
		system.load("/appdata/features/ParserFeatureSystem.xml");
		system.load(PluginLoader.instance());
		FeatureModelManager featureModelManager = new FeatureModelManager(system);
		String featureModelFileName = OptionManager.instance().getOptionValue(optionContainer, "guide", "features").toString().trim();
		try {
			if (featureModelFileName.endsWith(".par")) {
				String markingStrategy = OptionManager.instance().getOptionValue(optionContainer, "pproj", "marking_strategy").toString().trim();
				String coveredRoot = OptionManager.instance().getOptionValue(optionContainer, "pproj", "covered_root").toString().trim();
				featureModelManager.loadParSpecification(mcoModel.getMcoEntryURL(featureModelFileName), markingStrategy, coveredRoot);
			} else {
				featureModelManager.loadSpecification(mcoModel.getMcoEntryURL(featureModelFileName));
			}
		} catch(IOException e) {
			throw new MaltChainedException("Couldn't read file "+featureModelFileName+" from mco-file ", e);
		}
		return featureModelManager;
	}
}
