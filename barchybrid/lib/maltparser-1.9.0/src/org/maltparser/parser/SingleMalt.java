package org.maltparser.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Formatter;
import java.util.regex.Pattern;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.maltparser.core.config.ConfigurationDir;
import org.maltparser.core.config.ConfigurationException;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureModelManager;
import org.maltparser.core.feature.system.FeatureEngine;
import org.maltparser.core.helper.SystemLogger;
import org.maltparser.core.helper.URLFinder;
import org.maltparser.core.io.dataformat.DataFormatInstance;
import org.maltparser.core.options.OptionManager;
import org.maltparser.core.plugin.PluginLoader;
import org.maltparser.core.propagation.PropagationException;
import org.maltparser.core.propagation.PropagationManager;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.parser.guide.ClassifierGuide;

/**
 * @author Johan Hall
 *
 */
public class SingleMalt implements DependencyParserConfig {
	public final static Class<?>[] paramTypes = { org.maltparser.parser.DependencyParserConfig.class };
	public static final int LEARN = 0;
	public static final int PARSE = 1;
	protected ConfigurationDir configDir;
	protected Logger configLogger;
	protected int optionContainerIndex;
	protected ParsingAlgorithm parsingAlgorithm = null;
	protected int mode;
	protected SymbolTableHandler symbolTableHandler;
	protected DataFormatInstance dataFormatInstance;
	protected FeatureModelManager featureModelManager;
	protected long startTime;
	protected long endTime;
	protected int nIterations = 0;
	protected PropagationManager propagationManager;
	private Parser parser;
	private Trainer trainer;
	private AbstractParserFactory parserFactory;
	
	
	public void initialize(int containerIndex, DataFormatInstance dataFormatInstance, SymbolTableHandler symbolTableHandler, ConfigurationDir configDir, int mode) throws MaltChainedException {
		this.optionContainerIndex = containerIndex;
		this.mode = mode;
		setConfigurationDir(configDir);
		startTime = System.currentTimeMillis();
		configLogger = initConfigLogger(getOptionValue("config", "logfile").toString(), getOptionValue("config", "logging").toString());
		this.dataFormatInstance = dataFormatInstance;
		this.symbolTableHandler = symbolTableHandler;
		this.parserFactory = makeParserFactory();
		if (mode == SingleMalt.LEARN) {
			checkOptionDependency();
		}
		initPropagation();
		initFeatureSystem();
		initParsingAlgorithm(); 
		
		if (configLogger.isInfoEnabled()) {
			URL inputFormatURL = configDir.getInputFormatURL(); 
			URL outputFormatURL = configDir.getOutputFormatURL();
			if (inputFormatURL != null) {
				if (outputFormatURL == null || outputFormatURL.toString().equals(inputFormatURL.toString())) {
					int index = inputFormatURL.toString().indexOf('!');
					if (index == -1) {
						configLogger.info("  Data Format          : "+inputFormatURL.toString()+"\n");
					} else {
						configLogger.info("  Data Format          : "+inputFormatURL.toString().substring(index+1)+"\n");
					}
				} else {
					int indexIn = inputFormatURL.toString().indexOf('!');
					int indexOut = outputFormatURL.toString().indexOf('!');
					if (indexIn == -1) {
						configLogger.info("  Input Data Format    : "+inputFormatURL.toString()+"\n");
					} else {
						configLogger.info("  Input Data Format    : "+inputFormatURL.toString().substring(indexIn+1)+"\n");
					}
					if (indexOut == -1) {
						configLogger.info("  Output Data Format   : "+outputFormatURL.toString()+"\n");
					} else {
						configLogger.info("  Output Data Format   : "+outputFormatURL.toString().substring(indexOut+1)+"\n");
					}
				}
			}
		}
	}
	
	private void initPropagation()  throws MaltChainedException {
		String propagationSpecFileName = getOptionValue("singlemalt", "propagation").toString();
		if (propagationSpecFileName == null || propagationSpecFileName.length() == 0) {
			return;
		}
		propagationManager = new PropagationManager();
		if (mode == SingleMalt.LEARN) {
			propagationSpecFileName = configDir.copyToConfig(propagationSpecFileName);
			OptionManager.instance().overloadOptionValue(optionContainerIndex, "singlemalt", "propagation", propagationSpecFileName);
		}
		if (isLoggerInfoEnabled()) {
			logInfoMessage("  Propagation          : " + propagationSpecFileName+"\n");
		}
		propagationManager.loadSpecification(findURL(propagationSpecFileName));
		propagationManager.createPropagations(dataFormatInstance, symbolTableHandler);
	}
	
	/**
	 * Initialize the parsing algorithm
	 * 
	 * @throws MaltChainedException
	 */
	protected void initParsingAlgorithm() throws MaltChainedException {
		boolean diagnostics = (Boolean)getOptionValue("singlemalt", "diagnostics");
		if (mode == LEARN) {
			if (!diagnostics) {
				parsingAlgorithm = trainer = new BatchTrainer(this, symbolTableHandler);
			} else {
				parsingAlgorithm = trainer = new BatchTrainerWithDiagnostics(this, symbolTableHandler);
			}
		} else if (mode == PARSE) {
			if (!diagnostics) {
				parsingAlgorithm = parser = new DeterministicParser(this, symbolTableHandler);
			} else {
				parsingAlgorithm = parser = new DeterministicParserWithDiagnostics(this, symbolTableHandler);
			}
		}
	}
	
	protected void initFeatureSystem() throws MaltChainedException {
		final FeatureEngine system = new FeatureEngine();
		system.load("/appdata/features/ParserFeatureSystem.xml");
		system.load(PluginLoader.instance());
		featureModelManager = new FeatureModelManager(system);
		String featureModelFileName = getOptionValue("guide", "features").toString().trim();
		if (featureModelFileName.endsWith(".par")) {
			String markingStrategy = getOptionValue("pproj", "marking_strategy").toString().trim();
			String coveredRoot = getOptionValue("pproj", "covered_root").toString().trim();
			featureModelManager.loadParSpecification(findURL(featureModelFileName), markingStrategy, coveredRoot);
		} else {
			featureModelManager.loadSpecification(findURL(featureModelFileName));
		}
	}
	
	/**
	 * Creates a parser factory specified by the --singlemalt-parsing_algorithm option
	 * 
	 * @return a parser factory
	 * @throws MaltChainedException
	 */
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
	
	public AbstractParserFactory getParserFactory() {
		return parserFactory;
	}
	
	public FeatureModelManager getFeatureModelManager() {
		return featureModelManager;
	}
	
	public void process(Object[] arguments) throws MaltChainedException {
		if (mode == LEARN) {
			if (arguments.length < 2 || !(arguments[0] instanceof DependencyStructure) || !(arguments[1] instanceof DependencyStructure)) {
				throw new MaltChainedException("The single malt learn task must be supplied with at least two dependency structures. ");
			}
			DependencyStructure systemGraph = (DependencyStructure)arguments[0];
			DependencyStructure goldGraph = (DependencyStructure)arguments[1];
			if (systemGraph.hasTokens() && getGuide() != null) {
				getGuide().finalizeSentence(((Trainer)getAlgorithm()).parse(goldGraph, systemGraph));
			}
		} else if (mode == PARSE) {
			if (arguments.length < 1 || !(arguments[0] instanceof DependencyStructure)) {
				throw new MaltChainedException("The single malt parse task must be supplied with at least one input terminal structure and one output dependency structure. ");
			}
			DependencyStructure processGraph = (DependencyStructure)arguments[0];
			if (processGraph.hasTokens()) {
				parser.parse(processGraph);
//				((Parser)getAlgorithm()).parse(processGraph);
			}
		}
	}
	
	public void parse(DependencyStructure graph) throws MaltChainedException {
		if (graph.hasTokens()) {
//			((Parser)getAlgorithm()).parse(graph);
			parser.parse(graph);
		}
	}
	
	public void oracleParse(DependencyStructure goldGraph, DependencyStructure oracleGraph) throws MaltChainedException {
		if (oracleGraph.hasTokens()) {
			if (getGuide() != null) {
				getGuide().finalizeSentence(trainer.parse(goldGraph, oracleGraph));
			} else {
				trainer.parse(goldGraph, oracleGraph);
			}
		}
	}
	
	public void train() throws MaltChainedException {
		if (getGuide() == null) {
			((Trainer)getAlgorithm()).train();
		}
	}
	
	public void terminate(Object[] arguments) throws MaltChainedException {
//		if (getAlgorithm() instanceof Trainer) {
//			((Trainer)getAlgorithm()).terminate();
//		}
		getAlgorithm().terminate();
		if (getGuide() != null) {
			getGuide().terminate();
		}
		if (mode == LEARN) {
			endTime = System.currentTimeMillis();
			long elapsed = endTime - startTime;
			if (configLogger.isInfoEnabled()) {
				configLogger.info("Learning time: " +new Formatter().format("%02d:%02d:%02d", elapsed/3600000, elapsed%3600000/60000, elapsed%60000/1000)+" ("+elapsed+" ms)\n");
			}
		} else if (mode == PARSE) {
			endTime = System.currentTimeMillis();
			long elapsed = endTime - startTime;
			if (configLogger.isInfoEnabled()) {
				configLogger.info("Parsing time: " +new Formatter().format("%02d:%02d:%02d", elapsed/3600000, elapsed%3600000/60000, elapsed%60000/1000)+" ("+elapsed+" ms)\n");
			}
		}
		if (SystemLogger.logger() != configLogger && configLogger != null) {
			configLogger.removeAllAppenders();
		}
	}
	
	/**
	 * Initialize the configuration logger
	 * 
	 * @return the configuration logger
	 * @throws MaltChainedException
	 */
	public Logger initConfigLogger(String logfile, String level) throws MaltChainedException {
		if (logfile != null && logfile.length() > 0 && !logfile.equalsIgnoreCase("stdout") && configDir != null) {
			configLogger = Logger.getLogger(logfile);
			FileAppender fileAppender = null;
			try {
				fileAppender = new FileAppender(new PatternLayout("%m"),configDir.getWorkingDirectory().getPath()+File.separator+logfile, true);
			} catch(IOException e) {
				throw new ConfigurationException("It is not possible to create a configuration log file. ", e);
			}
			fileAppender.setThreshold(Level.toLevel(level, Level.INFO));
			configLogger.addAppender(fileAppender);
			configLogger.setLevel(Level.toLevel(level, Level.INFO));	
		} else {
			configLogger = SystemLogger.logger();
		}

		return configLogger;
	}
	
	public boolean isLoggerInfoEnabled() {
		return configLogger != null && configLogger.isInfoEnabled();
	}
	public boolean isLoggerDebugEnabled() {
		return configLogger != null && configLogger.isDebugEnabled();
	}
	public void logErrorMessage(String message) {
		configLogger.error(message);
	}
	public void logInfoMessage(String message) {
		configLogger.info(message);
	}
	public void logInfoMessage(char character) {
		configLogger.info(character);
	}
	public void logDebugMessage(String message) {
		configLogger.debug(message);
	}
	
	public void writeInfoToConfigFile(String message) throws MaltChainedException {
		try {
			configDir.getInfoFileWriter().write(message);
			configDir.getInfoFileWriter().flush();
		} catch (IOException e) {
			throw new ConfigurationException("Could not write to the configuration information file. ", e);
	
		}
	}
	
	public Logger getConfigLogger() {
		return configLogger;
	}

	public void setConfigLogger(Logger logger) {
		configLogger = logger;
	}
	
	public ConfigurationDir getConfigurationDir() {
		return configDir;
	}
	
	public void setConfigurationDir(ConfigurationDir configDir) {
		this.configDir = configDir;
	}
	
	public OutputStreamWriter getOutputStreamWriter(String fileName) throws MaltChainedException {
		return configDir.getOutputStreamWriter(fileName);
	}
	
	public OutputStreamWriter getAppendOutputStreamWriter(String fileName) throws MaltChainedException {
		return configDir.getAppendOutputStreamWriter(fileName);
	}
	
	public InputStreamReader getInputStreamReader(String fileName) throws MaltChainedException {
		return configDir.getInputStreamReader(fileName);
	}
	
	public InputStream getInputStreamFromConfigFileEntry(String fileName) throws MaltChainedException {
		return configDir.getInputStreamFromConfigFileEntry(fileName);
	}
	
	public URL getConfigFileEntryURL(String fileName) throws MaltChainedException {
		return configDir.getConfigFileEntryURL(fileName);
	}
	
	public File getFile(String fileName) throws MaltChainedException {
		return configDir.getFile(fileName);
	}
	
	public Object getConfigFileEntryObject(String fileName) throws MaltChainedException {
		Object object = null;
		try {
		    ObjectInputStream input = new ObjectInputStream(getInputStreamFromConfigFileEntry(fileName));
		    try {
		    	object = input.readObject();
			} catch (ClassNotFoundException e) {
				throw new ConfigurationException("Could not load object '"+fileName+"' from mco-file", e);
			} catch (Exception e) {
				throw new ConfigurationException("Could not load object '"+fileName+"' from mco-file", e);
		    } finally {
		    	input.close();
		    }
		} catch (IOException e) {
			throw new ConfigurationException("Could not load object from '"+fileName+"' in mco-file", e);
		}
	    return object;
	}
	
	public String getConfigFileEntryString(String fileName) throws MaltChainedException {
		StringBuilder sb = new StringBuilder();
		try {
			final BufferedReader in = new BufferedReader(new InputStreamReader(getInputStreamFromConfigFileEntry(fileName), "UTF-8"));
			String line;
			
			while((line = in.readLine()) != null) {
				 sb.append(line);
				 sb.append('\n');
			}
		} catch (IOException e) {
			throw new ConfigurationException("Could not load string from '"+fileName+"' in mco-file", e);
		}
	    return sb.toString();
	}
	
	public int getMode() {
		return mode;
	}

	public Object getOptionValue(String optiongroup, String optionname) throws MaltChainedException {
		return OptionManager.instance().getOptionValue(optionContainerIndex, optiongroup, optionname);
	}
	
	public String getOptionValueString(String optiongroup, String optionname) throws MaltChainedException {
		return OptionManager.instance().getOptionValueString(optionContainerIndex, optiongroup, optionname);
	}
	
	public OptionManager getOptionManager() throws MaltChainedException {
		return OptionManager.instance();
	}
	/******************************** MaltParserConfiguration specific  ********************************/
	
	/**
	 * Returns the list of symbol tables
	 * 
	 * @return the list of symbol tables
	 */
	public SymbolTableHandler getSymbolTables() {
		return symbolTableHandler;
	}
	
	public DataFormatInstance getDataFormatInstance() {
		return dataFormatInstance;
	}
	
	public PropagationManager getPropagationManager() {
		return propagationManager;
	}
	
	public ParsingAlgorithm getAlgorithm() {
		return parsingAlgorithm;
	}
	/**
	 * Returns the guide
	 * 
	 * @return the guide
	 */
	public ClassifierGuide getGuide() {
		return parsingAlgorithm.getGuide();
	}
	
	public void checkOptionDependency() throws MaltChainedException {
		try {
			if (configDir.getInfoFileWriter() != null) {
				configDir.getInfoFileWriter().write("\nDEPENDENCIES\n");
			}
			
			// Copy the feature model file into the configuration directory
			String featureModelFileName = getOptionValue("guide", "features").toString().trim();
			if (featureModelFileName.equals("")) {
				
				// use default feature model depending on the selected parser algorithm
				OptionManager.instance().overloadOptionValue(optionContainerIndex, "guide", "features", getOptionValueString("singlemalt", "parsing_algorithm"));
				featureModelFileName = getOptionValue("guide", "features").toString().trim();
				
				/* START: Temp fix during development of new liblinear and libsvm interface */
				String learner = getOptionValueString("guide", "learner");
				if (!learner.startsWith("lib")) {
					learner = "lib"+learner;
				}
				/* END: Temp fix during development of new liblinear and libsvm interface */
				featureModelFileName = featureModelFileName.replace("{learner}", learner);
				featureModelFileName = featureModelFileName.replace("{dataformat}", getOptionValue("input", "format").toString().trim().replace(".xml", ""));
				
				final URLFinder f = new URLFinder();
				featureModelFileName = configDir.copyToConfig(f.findURLinJars(featureModelFileName));
			} else {
				featureModelFileName = configDir.copyToConfig(featureModelFileName);
			}
			OptionManager.instance().overloadOptionValue(optionContainerIndex, "guide", "features", featureModelFileName);
			if (configDir.getInfoFileWriter() != null) {
				configDir.getInfoFileWriter().write("--guide-features (  -F)                 "+getOptionValue("guide", "features").toString()+"\n");
			}

			if (getOptionValue("guide", "data_split_column").toString().equals("") && !getOptionValue("guide", "data_split_structure").toString().equals("")) {
				configLogger.warn("Option --guide-data_split_column = '' and --guide-data_split_structure != ''. Option --guide-data_split_structure is overloaded with '', this will cause the parser to induce a single model.\n ");
				OptionManager.instance().overloadOptionValue(optionContainerIndex, "guide", "data_split_structure", "");
				if (configDir.getInfoFileWriter() != null) {
					configDir.getInfoFileWriter().write("--guide-data_split_structure (  -s)\n");
				}
			}
			if (!getOptionValue("guide", "data_split_column").toString().equals("") && getOptionValue("guide", "data_split_structure").toString().equals("")) {
				configLogger.warn("Option --guide-data_split_column != '' and --guide-data_split_structure = ''. Option --guide-data_split_column is overloaded with '', this will cause the parser to induce a single model.\n");
				OptionManager.instance().overloadOptionValue(optionContainerIndex, "guide", "data_split_column", "");
				if (configDir.getInfoFileWriter() != null) {
					configDir.getInfoFileWriter().write("--guide-data_split_column (  -d)\n");
				}
			}
			
			String decisionSettings = getOptionValue("guide", "decision_settings").toString().trim();
			String markingStrategy = getOptionValue("pproj", "marking_strategy").toString().trim();
			String coveredRoot = getOptionValue("pproj", "covered_root").toString().trim();
			StringBuilder newDecisionSettings = new StringBuilder();

			if (decisionSettings == null || decisionSettings.length() < 1 || decisionSettings.equals("default")) {
				decisionSettings = "T.TRANS+A.DEPREL";
			} else {
				decisionSettings = decisionSettings.toUpperCase();
			}
			
			if (markingStrategy.equalsIgnoreCase("head") || markingStrategy.equalsIgnoreCase("path") || markingStrategy.equalsIgnoreCase("head+path")) {
				if (!Pattern.matches(".*A\\.PPLIFTED.*", decisionSettings)) {
					newDecisionSettings.append("+A.PPLIFTED");
				}
			}
			if (markingStrategy.equalsIgnoreCase("path") || markingStrategy.equalsIgnoreCase("head+path")) {
				if (!Pattern.matches(".*A\\.PPPATH.*", decisionSettings)) {
					newDecisionSettings.append("+A.PPPATH");
				}
			}
			if (!coveredRoot.equalsIgnoreCase("none") && !Pattern.matches(".*A\\.PPCOVERED.*", decisionSettings)) {
				newDecisionSettings.append("+A.PPCOVERED");
			}
			if (!getOptionValue("guide", "decision_settings").toString().equals(decisionSettings) || newDecisionSettings.length() > 0) {
				OptionManager.instance().overloadOptionValue(optionContainerIndex, "guide", "decision_settings", decisionSettings+newDecisionSettings.toString());
				if (configDir.getInfoFileWriter() != null) {
					configDir.getInfoFileWriter().write("--guide-decision_settings (  -gds)                 "+getOptionValue("guide", "decision_settings").toString()+"\n");
				}
			}
			if (configDir.getInfoFileWriter() != null) {
				configDir.getInfoFileWriter().flush();
			}
		} catch (IOException e) {
			throw new ConfigurationException("Could not write to the configuration information file. ", e);
		}
	}
	
	private URL findURL(String propagationSpecFileName) throws MaltChainedException {
		URL url = null;
		File specFile = configDir.getFile(propagationSpecFileName);
		if (specFile.exists()) {
			try {
				url = new URL("file:///"+specFile.getAbsolutePath());
			} catch (MalformedURLException e) {
				throw new PropagationException("Malformed URL: "+specFile, e);
			}
		} else {
			url = configDir.getConfigFileEntryURL(propagationSpecFileName);
		}
		return url;
	}
}
