package org.maltparser.core.lw.parser;
import java.util.ArrayList;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureModel;
import org.maltparser.core.feature.FeatureModelManager;
import org.maltparser.core.helper.HashMap;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.symbol.TableHandler;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.parser.AlgoritmInterface;
import org.maltparser.parser.DependencyParserConfig;
import org.maltparser.parser.ParserConfiguration;
import org.maltparser.parser.ParserRegistry;
import org.maltparser.parser.TransitionSystem;
import org.maltparser.parser.history.GuideUserHistory;
import org.maltparser.parser.history.action.ComplexDecisionAction;
import org.maltparser.parser.history.action.GuideUserAction;
import org.maltparser.parser.history.container.ActionContainer;
import org.maltparser.parser.history.container.CombinedTableContainer;
import org.maltparser.parser.history.container.TableContainer;

/**
* A lightweight version of org.maltparser.parser.DeterministicParser. This class also implements a lightweight version of 
* org.maltparser.parser.history.History and reduces the need of org.maltparser.parser.ParserState. 
* 
* The class must be used in the same thread.
* 
* @author Johan Hall
*/
public final class LWDeterministicParser implements AlgoritmInterface,  GuideUserHistory {
	private final LWSingleMalt manager;
	private final ParserRegistry registry;

	private final TransitionSystem transitionSystem;
	private final ParserConfiguration config;
	private final FeatureModel featureModel;
	private final ComplexDecisionAction currentAction;
	
	private final int kBestSize;
	private final ArrayList<TableContainer> decisionTables;
	private final ArrayList<TableContainer> actionTables; 
	private final HashMap<String, TableHandler> tableHandlers;
	
	public LWDeterministicParser(LWSingleMalt lwSingleMalt, SymbolTableHandler symbolTableHandler, FeatureModel _featureModel) throws MaltChainedException {
		this.manager = lwSingleMalt;
		this.registry = new ParserRegistry();
		this.registry.setSymbolTableHandler(symbolTableHandler);
		this.registry.setDataFormatInstance(manager.getDataFormatInstance());
		this.registry.setAbstractParserFeatureFactory(manager.getParserFactory());
		this.registry.setAlgorithm(this);
		this.transitionSystem = manager.getParserFactory().makeTransitionSystem();
		this.transitionSystem.initTableHandlers(lwSingleMalt.getDecisionSettings(), symbolTableHandler);
		
		this.tableHandlers = transitionSystem.getTableHandlers();
		this.kBestSize = lwSingleMalt.getkBestSize();
		this.decisionTables = new ArrayList<TableContainer>();
		this.actionTables = new ArrayList<TableContainer>();
		initDecisionSettings(lwSingleMalt.getDecisionSettings(), lwSingleMalt.getClassitem_separator());
		this.transitionSystem.initTransitionSystem(this);
		this.config = manager.getParserFactory().makeParserConfiguration();
		this.featureModel = _featureModel;
		this.currentAction = new ComplexDecisionAction(this);
	}
	
	public LWDeterministicParser(LWSingleMalt lwSingleMalt, SymbolTableHandler symbolTableHandler) throws MaltChainedException {
		this.manager = lwSingleMalt;
		this.registry = new ParserRegistry();
		this.registry.setSymbolTableHandler(symbolTableHandler);
		this.registry.setDataFormatInstance(manager.getDataFormatInstance());
		this.registry.setAbstractParserFeatureFactory(manager.getParserFactory());
		this.registry.setAlgorithm(this);
		this.transitionSystem = manager.getParserFactory().makeTransitionSystem();
		this.transitionSystem.initTableHandlers(lwSingleMalt.getDecisionSettings(), symbolTableHandler);
		
		this.tableHandlers = transitionSystem.getTableHandlers();
		this.kBestSize = lwSingleMalt.getkBestSize();
		this.decisionTables = new ArrayList<TableContainer>();
		this.actionTables = new ArrayList<TableContainer>();
		initDecisionSettings(lwSingleMalt.getDecisionSettings(), lwSingleMalt.getClassitem_separator());
		this.transitionSystem.initTransitionSystem(this);
		this.config = manager.getParserFactory().makeParserConfiguration();
		this.featureModel = manager.getFeatureModelManager().getFeatureModel(lwSingleMalt.getFeatureModelURL(), 0, registry, manager.getDataSplitColumn(), manager.getDataSplitStructure());
		this.currentAction = new ComplexDecisionAction(this);
	}
	
	public DependencyStructure parse(DependencyStructure parseDependencyGraph) throws MaltChainedException {
		config.clear();
		config.setDependencyGraph(parseDependencyGraph);
		config.initialize();

		while (!config.isTerminalState()) {
			GuideUserAction action = transitionSystem.getDeterministicAction(this, config);
			if (action == null) {
				action = predict();
			}
			transitionSystem.apply(action, config);
		} 
		parseDependencyGraph.linkAllTreesToRoot();
		return parseDependencyGraph;
	}
	
	private GuideUserAction predict() throws MaltChainedException {
		currentAction.clear();
		try {
			manager.getDecisionModel().predict(featureModel, currentAction, true);
			
			while (!transitionSystem.permissible(currentAction, config)) {
				if (manager.getDecisionModel().predictFromKBestList(featureModel, currentAction) == false) {
					GuideUserAction defaultAction = transitionSystem.defaultAction(this, config);
					ActionContainer[] actionContainers = this.getActionContainerArray();
					defaultAction.getAction(actionContainers);
					currentAction.addAction(actionContainers);
					break;
				}
			}
		} catch (NullPointerException e) {
			throw new MaltChainedException("The guide cannot be found. ", e);
		}
		return currentAction;
	}
	
	public ParserRegistry getParserRegistry() {
		return registry;
	}
	
	public ParserConfiguration getCurrentParserConfiguration() {
		return config;
	}
	
	public DependencyParserConfig getManager() {
		return manager;
	}
	
	public String getGuideName() {
		return null;
	}

	public void setGuideName(String guideName) { }
	
	// GuideUserHistory interface
	public GuideUserAction getEmptyGuideUserAction() throws MaltChainedException {
		return new ComplexDecisionAction(this);
	}
	
	public ArrayList<ActionContainer> getActionContainers() {
		ArrayList<ActionContainer> actionContainers = new ArrayList<ActionContainer>();
		for (int i=0; i<actionTables.size(); i++) {
			actionContainers.add(new ActionContainer(actionTables.get(i)));
		}
		return actionContainers;
	}
	
	public ActionContainer[] getActionContainerArray() {
		ActionContainer[] actionContainers = new ActionContainer[actionTables.size()];
		for (int i=0; i<actionTables.size(); i++) {
			actionContainers[i] = new ActionContainer(actionTables.get(i));
		}
		return actionContainers;
	}
	
	public void clear() throws MaltChainedException { }
	
	public int getNumberOfDecisions() {
		return decisionTables.size();
	}
	
	public int getKBestSize() {
		return kBestSize;
	}

	public int getNumberOfActions() {
		return actionTables.size();
	}
	
	public ArrayList<TableContainer> getDecisionTables() {
		return decisionTables;
	}

	public ArrayList<TableContainer> getActionTables() {
		return actionTables;
	}

	private void initDecisionSettings(String decisionSettings, String separator) throws MaltChainedException {
		if (decisionSettings.equals("T.TRANS+A.DEPREL")) {
			actionTables.add(new TableContainer(tableHandlers.get("T").getSymbolTable("TRANS"), "T.TRANS", '+'));
			actionTables.add(new TableContainer(tableHandlers.get("A").getSymbolTable("DEPREL"), "A.DEPREL", ' '));
			decisionTables.add(new CombinedTableContainer(tableHandlers.get("A"), separator, actionTables, ' '));
		} else if (decisionSettings.equals("T.TRANS,A.DEPREL")) {
			TableContainer transTableContainer = new TableContainer(tableHandlers.get("T").getSymbolTable("TRANS"), "T.TRANS", ',');
			TableContainer deprelTableContainer = new TableContainer(tableHandlers.get("A").getSymbolTable("DEPREL"), "A.DEPREL", ',');
			actionTables.add(transTableContainer);
			actionTables.add(deprelTableContainer);
			decisionTables.add(transTableContainer);
			decisionTables.add(deprelTableContainer);
		} else if (decisionSettings.equals("T.TRANS#A.DEPREL")  || decisionSettings.equals("T.TRANS;A.DEPREL")) {
			TableContainer transTableContainer = new TableContainer(tableHandlers.get("T").getSymbolTable("TRANS"), "T.TRANS", '#');
			TableContainer deprelTableContainer = new TableContainer(tableHandlers.get("A").getSymbolTable("DEPREL"), "A.DEPREL", '#');
			actionTables.add(transTableContainer);
			actionTables.add(deprelTableContainer);
			decisionTables.add(transTableContainer);
			decisionTables.add(deprelTableContainer);
		} else {
			int start = 0;
			int k = 0;
			char prevDecisionSeparator = ' ';
			TableContainer tmp = null;
			final StringBuilder sbTableHandler = new StringBuilder();
			final StringBuilder sbTable = new StringBuilder();
			int state = 0;
			for (int i = 0; i < decisionSettings.length(); i++) {
				switch (decisionSettings.charAt(i)) {
				case '.':
					state = 1;
					break;
				case '+':
					tmp = new TableContainer(tableHandlers.get(sbTableHandler.toString()).getSymbolTable(sbTable.toString()), 
							sbTableHandler.toString()+"."+sbTable.toString(), '+');
					actionTables.add(tmp);
					k++;
					sbTableHandler.setLength(0);
					sbTable.setLength(0);
					state = 0;
					break;
				case '#':
					state = 2;
					break;
				case ';':
					state = 2;
					break;
				case ',':
					state = 2;
					break;
				default:
					if (state == 0) {
						sbTableHandler.append(decisionSettings.charAt(i));
					} else if (state == 1) {
						sbTable.append(decisionSettings.charAt(i));
					}
				}
				if (state == 2 || i == decisionSettings.length()-1) {
					char decisionSeparator = decisionSettings.charAt(i);
					if (i == decisionSettings.length()-1) {
						decisionSeparator = prevDecisionSeparator;
					}
					tmp = new TableContainer(tableHandlers.get(sbTableHandler.toString()).getSymbolTable(sbTable.toString()), 
							sbTableHandler.toString()+"."+sbTable.toString(), decisionSeparator);
					actionTables.add(tmp);
					k++;
					if (k-start > 1) {
						decisionTables.add(new CombinedTableContainer(tableHandlers.get("A"), separator, actionTables.subList(start, k), decisionSeparator));
					} else {
						decisionTables.add(tmp);
					}
					sbTableHandler.setLength(0);
					sbTable.setLength(0);
					state = 0;
					start = k;
					prevDecisionSeparator = decisionSeparator;
				}
			}
		}
	}
}
