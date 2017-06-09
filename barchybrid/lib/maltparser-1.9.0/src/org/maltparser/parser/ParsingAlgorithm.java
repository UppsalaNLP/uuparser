package org.maltparser.parser;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.core.syntaxgraph.node.DependencyNode;
import org.maltparser.parser.guide.ClassifierGuide;
/**
 * @author Johan Hall
 *
 */
public abstract class ParsingAlgorithm implements AlgoritmInterface {
	protected final DependencyParserConfig manager;
	protected final ParserRegistry registry;
	protected ClassifierGuide classifierGuide;
	protected final ParserState parserState;
	protected ParserConfiguration currentParserConfiguration;

	/**
	 * Creates a parsing algorithm
	 * 
	 * @param _manager a reference to the single malt configuration
	 * @param symbolTableHandler a reference to the symbol table handler
	 * @throws MaltChainedException
	 */
	public ParsingAlgorithm(DependencyParserConfig _manager, SymbolTableHandler symbolTableHandler) throws MaltChainedException {
		this.manager = _manager;
		this.registry = new ParserRegistry();
		registry.setSymbolTableHandler(symbolTableHandler);
		registry.setDataFormatInstance(manager.getDataFormatInstance());
		registry.setAbstractParserFeatureFactory(manager.getParserFactory());
		parserState = new ParserState(manager, symbolTableHandler, manager.getParserFactory());	
	}
	
	public abstract void terminate() throws MaltChainedException;

	public ParserRegistry getParserRegistry() {
		return registry;
	}
	
	/**
	 * Returns the classifier guide.
	 * 
	 * @return the classifier guide
	 */
	public ClassifierGuide getGuide() {
		return classifierGuide;
	}
	
	/**
	 * Sets the classifier guide
	 * 
	 * @param guide a classifier guide
	 */
	public void setGuide(ClassifierGuide guide) {
		this.classifierGuide = guide;
	}

	/**
	 * Returns the current active parser configuration
	 * 
	 * @return the current active parser configuration
	 */
	public ParserConfiguration getCurrentParserConfiguration() {
		return currentParserConfiguration;
	}
	
	/**
	 * Sets the current parser configuration
	 * 
	 * @param currentParserConfiguration a parser configuration
	 */
	protected void setCurrentParserConfiguration(ParserConfiguration currentParserConfiguration) {
		this.currentParserConfiguration = currentParserConfiguration;
	}
	
	/**
	 * Returns the parser state
	 * 
	 * @return the parser state
	 */
	public ParserState getParserState() {
		return parserState;
	}
	
	
	/**
	 * Returns the single malt configuration
	 * 
	 * @return the single malt configuration
	 */
	public DependencyParserConfig getManager() {
		return manager;
	}

	
	/**
	 * Copies the edges of the source dependency structure to the target dependency structure
	 * 
	 * @param source a source dependency structure
	 * @param target a target dependency structure
	 * @throws MaltChainedException
	 */
	protected void copyEdges(DependencyStructure source, DependencyStructure target) throws MaltChainedException {
		for (int index : source.getTokenIndices()) {
			DependencyNode snode = source.getDependencyNode(index);
			
			if (snode.hasHead()) {
				Edge s = snode.getHeadEdge();
				Edge t = target.addDependencyEdge(s.getSource().getIndex(), s.getTarget().getIndex());
				
				for (SymbolTable table : s.getLabelTypes()) {
					t.addLabel(table, s.getLabelSymbol(table));
				}
			}
		}
	}
	
	protected void copyDynamicInput(DependencyStructure source, DependencyStructure target) throws MaltChainedException {
		for (int index : source.getTokenIndices()) {
			DependencyNode snode = source.getDependencyNode(index);
			DependencyNode tnode = target.getDependencyNode(index);
			for (SymbolTable table : snode.getLabelTypes()) {
				if (!tnode.hasLabel(table)) {
					tnode.addLabel(table,snode.getLabelSymbol(table));
				}
			}
		}
	}
}
