package org.maltparser.parser.algorithm.nivre;

import java.util.Stack;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.propagation.PropagationManager;
import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.core.syntaxgraph.node.DependencyNode;
import org.maltparser.parser.ParserConfiguration;
import org.maltparser.parser.TransitionSystem;
import org.maltparser.parser.history.GuideUserHistory;
import org.maltparser.parser.history.action.ComplexDecisionAction;
import org.maltparser.parser.history.action.GuideUserAction;
import org.maltparser.parser.transition.TransitionTable;
/**
 * @author Johan Hall
 *
 */
public class ArcEager extends TransitionSystem {
	protected static final int SHIFT = 1;
	protected static final int REDUCE = 2;
	protected static final int RIGHTARC = 3;
	protected static final int LEFTARC = 4;
	protected static final int UNSHIFT = 5;
	
	public ArcEager(PropagationManager propagationManager) throws MaltChainedException {
		super(propagationManager);
	}
	
	public void apply(GuideUserAction currentAction, ParserConfiguration config) throws MaltChainedException {
		final NivreConfig nivreConfig = (NivreConfig)config;
		final Stack<DependencyNode> stack = nivreConfig.getStack();
		final Stack<DependencyNode> input = nivreConfig.getInput();
		currentAction.getAction(actionContainers);
		Edge e = null;
		if (!nivreConfig.isEnforceTree()) {
			switch (transActionContainer.getActionCode()) {
			case LEFTARC:
				e = nivreConfig.getDependencyStructure().addDependencyEdge(input.peek().getIndex(), stack.peek().getIndex());
				addEdgeLabels(e);
				stack.pop();
				break;
			case RIGHTARC:
				e = nivreConfig.getDependencyStructure().addDependencyEdge(stack.peek().getIndex(), input.peek().getIndex());
				addEdgeLabels(e);
				stack.push(input.pop());
				break;
			case REDUCE:
				stack.pop();
				break;
			default:
				stack.push(input.pop());
				break;
			}
		} else {
			switch (transActionContainer.getActionCode()) {
			case LEFTARC:
				e = nivreConfig.getDependencyStructure().addDependencyEdge(input.peek().getIndex(), stack.peek().getIndex());
				addEdgeLabels(e);
				stack.pop();
				break;
			case RIGHTARC:
				e = nivreConfig.getDependencyStructure().addDependencyEdge(stack.peek().getIndex(), input.peek().getIndex());
				addEdgeLabels(e);
				stack.push(input.pop());
                if(input.isEmpty() && !nivreConfig.isEnd()) {
                	nivreConfig.setEnd(true);
                }
				break;
			case REDUCE:
				stack.pop();
				break;
	        case UNSHIFT:                                                                                                                               
	            input.push(stack.pop());
	            break;
			default:
				stack.push(input.pop());
	                                                                                                                                                 
	            if (input.isEmpty() && !nivreConfig.isEnd()) {
	            	nivreConfig.setEnd(true);
	            }
	
				break;
			}
			
		}
	}
	
    public GuideUserAction getDeterministicAction(GuideUserHistory history, ParserConfiguration config) throws MaltChainedException {
    	final NivreConfig nivreConfig = (NivreConfig)config;
    	if (!nivreConfig.isEnforceTree()) {
          if (!nivreConfig.isAllowRoot() && nivreConfig.getStack().peek().isRoot()) {
          	return updateActionContainers(history, ArcEager.SHIFT, null);
          }
    	} else {
    		//Added
	    	if (!nivreConfig.isAllowRoot() && nivreConfig.getStack().peek().isRoot() && !nivreConfig.isEnd()) {
	    		return updateActionContainers(history, ArcEager.SHIFT, null);
	    	}
	                                                                                                                                                        
	    	if (nivreConfig.getInput().isEmpty() && nivreConfig.getStack().peek().hasHead()) {
	            return updateActionContainers(history, ArcEager.REDUCE, null);
	    	}
	    	
	    	if (nivreConfig.getInput().isEmpty() && !nivreConfig.getStack().peek().hasHead()) {
	            return updateActionContainers(history, ArcEager.UNSHIFT, null);
	    	}
    	}
    	return null;
    }

	
	protected void addAvailableTransitionToTable(TransitionTable ttable) throws MaltChainedException {
		ttable.addTransition(SHIFT, "SH", false, null);
		ttable.addTransition(REDUCE, "RE", false, null);
		ttable.addTransition(RIGHTARC, "RA", true, null);
		ttable.addTransition(LEFTARC, "LA", true, null);
		ttable.addTransition(UNSHIFT, "USH", false, null);//Added
	}
	
	protected void initWithDefaultTransitions(GuideUserHistory history) throws MaltChainedException {
		GuideUserAction currentAction = new ComplexDecisionAction(history);
		
		transActionContainer.setAction(SHIFT);
		transActionContainer.setAction(REDUCE);
		for (int i = 0; i < arcLabelActionContainers.length; i++) {
			arcLabelActionContainers[i].setAction(-1);
		}
		currentAction.addAction(actionContainers);
	}
	
	public String getName() {
		return "nivreeager";
	}

	public boolean permissible(GuideUserAction currentAction, ParserConfiguration config) throws MaltChainedException {
		currentAction.getAction(actionContainers);
		final int trans = transActionContainer.getActionCode();
		final NivreConfig nivreConfig = (NivreConfig)config;
		final DependencyNode stackPeek = nivreConfig.getStack().peek();
		if ((trans == LEFTARC || trans == RIGHTARC) && !isActionContainersLabeled()) {
			return false;
		}
		if ((trans == LEFTARC || trans == REDUCE) && stackPeek.isRoot()) { 
			return false;
		}
		if (trans == LEFTARC && stackPeek.hasHead()) { 
			return false;
		}
		if (trans == REDUCE && !stackPeek.hasHead() && !nivreConfig.isAllowReduce()) {
			return false;
		}
        //Added                                                                                                                                                   
        if (trans == SHIFT && nivreConfig.isEnforceTree() && nivreConfig.isEnd()){
            return false;
        }

		return true;
	}
	
	public GuideUserAction defaultAction(GuideUserHistory history, ParserConfiguration configuration) throws MaltChainedException {
		return updateActionContainers(history, ArcEager.SHIFT, null);
	}
}