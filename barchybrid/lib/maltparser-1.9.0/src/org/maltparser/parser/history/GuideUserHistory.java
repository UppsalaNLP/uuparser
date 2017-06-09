package org.maltparser.parser.history;

import java.util.ArrayList;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.parser.history.action.GuideUserAction;
import org.maltparser.parser.history.container.ActionContainer;
import org.maltparser.parser.history.container.TableContainer;
/**
*
* @author Johan Hall
* @since 1.1
**/
public interface GuideUserHistory {
	public GuideUserAction getEmptyGuideUserAction() throws MaltChainedException; 
	public ArrayList<ActionContainer> getActionContainers();
	public ActionContainer[] getActionContainerArray();
	public int getNumberOfDecisions();
	public void clear() throws MaltChainedException; 
//	public void setKBestListClass(Class<?> kBestListClass) throws MaltChainedException;
//	public Class<?> getKBestListClass();
	public int getKBestSize();
//	public void setKBestSize(int kBestSize);
//	public void setSeparator(String separator) throws MaltChainedException;
	public ArrayList<TableContainer> getDecisionTables();
	public ArrayList<TableContainer> getActionTables();
}
