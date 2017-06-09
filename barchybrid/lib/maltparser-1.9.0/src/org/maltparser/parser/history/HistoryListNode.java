package org.maltparser.parser.history;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.parser.history.action.GuideUserAction;
/**
 * 
 * @author Johan Hall
*/
public class HistoryListNode implements HistoryNode {
	private HistoryNode previousNode;
	private GuideUserAction action;
	private int position;
	
	public HistoryListNode(HistoryNode _previousNode, GuideUserAction _action) {
		this.previousNode = _previousNode;
		this.action = _action;
		if (previousNode != null) {
			this.position = previousNode.getPosition()+1;
		} else {
			this.position = 1;
		}
	}
	
	public HistoryNode getPreviousNode() {
		return previousNode;
	}

	public GuideUserAction getAction() {
		return action;
	}

	public void setPreviousNode(HistoryNode node) {
		this.previousNode = node;
	}

	public void setAction(GuideUserAction action) {
		this.action = action;
	}
	
	public int getPosition() {
		return position;
	}
	
	public void clear() throws MaltChainedException {
		this.previousNode = null;
		this.action = null;
	}
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(action);
		return sb.toString();
	} 
	
}
