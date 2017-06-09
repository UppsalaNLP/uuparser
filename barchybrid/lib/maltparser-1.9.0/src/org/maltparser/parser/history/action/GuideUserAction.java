package org.maltparser.parser.history.action;

import java.util.ArrayList;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.parser.history.container.ActionContainer;
/**
*
* @author Johan Hall
**/
public interface GuideUserAction {
	public void addAction(ArrayList<ActionContainer> actionContainers) throws MaltChainedException;
	public void addAction(ActionContainer[] actionContainers) throws MaltChainedException;
	public void getAction(ArrayList<ActionContainer> actionContainers) throws MaltChainedException;
	public void getAction(ActionContainer[] actionContainers) throws MaltChainedException;
	public int numberOfActions();
}
