package org.maltparser.parser.algorithm.nivre;

import java.util.Stack;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.node.DependencyNode;
import org.maltparser.parser.ParserConfiguration;
import org.maltparser.parser.ParsingException;
/**
 * @author Johan Hall
 *
 */
public class NivreConfig extends ParserConfiguration {
	private final Stack<DependencyNode> stack;
	private final Stack<DependencyNode> input;
	private DependencyStructure dependencyGraph;
	private final boolean allowRoot;
	private final boolean allowReduce;
	private final boolean enforceTree;
	
    private boolean end; //Added
	
	public NivreConfig(boolean allowRoot, boolean allowReduce, boolean enforceTree) throws MaltChainedException {
		super();
		this.stack = new Stack<DependencyNode>();
		this.input = new Stack<DependencyNode>();
		this.allowRoot = allowRoot;
		this.allowReduce = allowReduce;
		this.enforceTree = enforceTree;
		this.end = false; // Added 
	}
	
	public Stack<DependencyNode> getStack() {
		return stack;
	}
	
	public Stack<DependencyNode> getInput() {
		return input;
	}
	
	public DependencyStructure getDependencyStructure() {
		return dependencyGraph;
	}
	
	public boolean isTerminalState() {
		if (isEnforceTree()) {
			return input.isEmpty() && stack.size() == 1;
		}
		return input.isEmpty();
	}
	
	public DependencyNode getStackNode(int index) throws MaltChainedException {
		if (index < 0) {
			throw new ParsingException("Stack index must be non-negative in feature specification. ");
		}
		if (stack.size()-index > 0) {
			return stack.get(stack.size()-1-index);
		}
		return null;
	}
	
	public DependencyNode getInputNode(int index) throws MaltChainedException {
		if (index < 0) {
			throw new ParsingException("Input index must be non-negative in feature specification. ");
		}
		if (input.size()-index > 0) {
			return input.get(input.size()-1-index);
		}	
		return null;
	}
	
	public void setDependencyGraph(DependencyStructure source) throws MaltChainedException {
		this.dependencyGraph = source;
	}
	
	public DependencyStructure getDependencyGraph() {
		return dependencyGraph;
	}
	
	public void initialize(ParserConfiguration parserConfiguration) throws MaltChainedException {
		if (parserConfiguration != null) {
			final NivreConfig nivreConfig = (NivreConfig)parserConfiguration;
			final Stack<DependencyNode> sourceStack = nivreConfig.getStack();
			final Stack<DependencyNode> sourceInput = nivreConfig.getInput();
			setDependencyGraph(nivreConfig.getDependencyGraph());
			for (int i = 0, n = sourceStack.size(); i < n; i++) {
				stack.add(dependencyGraph.getDependencyNode(sourceStack.get(i).getIndex()));
			}
			for (int i = 0, n = sourceInput.size(); i < n; i++) {
				input.add(dependencyGraph.getDependencyNode(sourceInput.get(i).getIndex()));
			}
		} else {
			stack.push(dependencyGraph.getDependencyRoot());
			for (int i = dependencyGraph.getHighestTokenIndex(); i > 0; i--) {
				final DependencyNode node = dependencyGraph.getDependencyNode(i);
				if (node != null && !node.hasHead()) { // added !node.hasHead()
					input.push(node);
				}
			}
		}
	}
	
	public void initialize() throws MaltChainedException {
		stack.push(dependencyGraph.getDependencyRoot());
		for (int i = dependencyGraph.getHighestTokenIndex(); i > 0; i--) {
			final DependencyNode node = dependencyGraph.getDependencyNode(i);
			if (node != null && !node.hasHead()) { // added !node.hasHead()
				input.push(node);
			}
		}
	}
	
	public void setEnd(boolean end) {
		this.end = end;
	}
                                                                                                                                                             
    public boolean isEnd() {
        return end;
	}
    
    public boolean isAllowRoot() {
        return allowRoot;
	}
	
	public boolean isAllowReduce() {
	        return allowReduce;
	}
	
	public boolean isEnforceTree() {
		return enforceTree;
	}

	public void clear() throws MaltChainedException {
		stack.clear();
		input.clear();
		historyNode = null;
		end = false; // Added
	}
	
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NivreConfig that = (NivreConfig)obj;
		
		if (stack.size() != that.getStack().size()) 
			return false;
		if (input.size() != that.getInput().size())
			return false;
		if (dependencyGraph.nEdges() != that.getDependencyGraph().nEdges())
			return false;
		for (int i = 0; i < stack.size(); i++) {
			if (stack.get(i).getIndex() != that.getStack().get(i).getIndex()) {
				return false;
			}
		}
		for (int i = 0; i < input.size(); i++) {
			if (input.get(i).getIndex() != that.getInput().get(i).getIndex()) {
				return false;
			}
		}		
		return dependencyGraph.getEdges().equals(that.getDependencyGraph().getEdges());
	}
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(stack.size());
		sb.append(", ");
		sb.append(input.size());
		sb.append(", ");
		sb.append(dependencyGraph.nEdges());
		sb.append(", ");
		sb.append(allowRoot);		
		sb.append(", ");
		sb.append(allowReduce);		
		sb.append(", ");
		sb.append(enforceTree);
		return sb.toString();
	}
}
