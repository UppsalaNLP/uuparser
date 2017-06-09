package org.maltparser.core.syntaxgraph;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Observable;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.helper.HashMap;
import org.maltparser.core.pool.ObjectPoolList;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.syntaxgraph.node.Token;
import org.maltparser.core.syntaxgraph.node.TokenNode;
/**
*
*
* @author Johan Hall
*/
public class Sentence extends SyntaxGraph implements TokenStructure {
	protected final ObjectPoolList<Token> terminalPool;
	protected final SortedMap<Integer,Token> terminalNodes;
	protected final HashMap<Integer, ArrayList<String>> comments;
	protected int sentenceID;
	
	public Sentence(SymbolTableHandler symbolTables) throws MaltChainedException {
		super(symbolTables);
		terminalNodes = new TreeMap<Integer,Token>();
		terminalPool = new ObjectPoolList<Token>() {
			protected Token create() throws MaltChainedException { return new Token(); }
			public void resetObject(Token o) throws MaltChainedException { o.clear(); }
		};
		comments = new HashMap<Integer, ArrayList<String>>();
	}

	public TokenNode addTokenNode(int index) throws MaltChainedException {
		if (index > 0) {
			return getOrAddTerminalNode(index);
		}
		return null;
	}
	
	public TokenNode addTokenNode() throws MaltChainedException {
		int index = getHighestTokenIndex();
		if (index > 0) {
			return getOrAddTerminalNode(index+1);
		}
		return getOrAddTerminalNode(1);
	}
	
	public void addComment(String comment, int at_index) {
		ArrayList<String> commentList = comments.get(at_index);
		if (commentList == null) {
			commentList = new ArrayList<String>();
			comments.put(at_index, commentList);
		}
		commentList.add(comment);
	}
	
	public ArrayList<String> getComment(int at_index) {
		return comments.get(at_index);
	}
	
	public boolean hasComments() {
		return comments.size() > 0;
	}
	
	public int nTokenNode() {
		return terminalNodes.size();
	}
	
	public boolean hasTokens() {
		return !terminalNodes.isEmpty();
	}
	
	
	protected Token getOrAddTerminalNode(int index) throws MaltChainedException {
		Token node = terminalNodes.get(index);
		if (node == null) {
//		if (!terminalNodes.containsKey(index)) {
			if (index > 0){
				node = terminalPool.checkOut();
				node.setIndex(index);
				node.setBelongsToGraph(this); 
				
				if (index > 1) {
					Token prev = terminalNodes.get(index-1);
					if (prev == null) {
						try {
							prev = terminalNodes.get(terminalNodes.headMap(index).lastKey());
						} catch (NoSuchElementException e) {
							
						}
					}
					if (prev != null) {
						prev.setSuccessor(node);
						node.setPredecessor(prev);
					}
					
					if (terminalNodes.lastKey() > index) {
						Token succ = terminalNodes.get(index+1);
						if (succ == null) {
							try {
								succ = terminalNodes.get(terminalNodes.tailMap(index).firstKey());
							} catch (NoSuchElementException e) {
								
							}
						}
						if (succ != null) {
							succ.setPredecessor(node);
							node.setSuccessor(succ);
						}
					}
				}
			}
			terminalNodes.put(index,node);
			numberOfComponents++;
		} 
//		else {
//			node = terminalNodes.get(index);
//		}
		return node;
	}
	
	public SortedSet<Integer> getTokenIndices() {
		return new TreeSet<Integer>(terminalNodes.keySet());
	}
	
	public int getHighestTokenIndex() {
		try {
			return terminalNodes.lastKey();
		} catch (NoSuchElementException e) {
			return 0;
		}
	}
	
	public TokenNode getTokenNode(int index) {
		if (index > 0) {
			return terminalNodes.get(index);
		}
		return null;
	}
	
	
	public int getSentenceID() {
		return sentenceID;
	}

	public void setSentenceID(int sentenceID) {
		this.sentenceID = sentenceID;
	}

	public void clear() throws MaltChainedException {
		terminalPool.checkInAll();
		terminalNodes.clear();
		comments.clear();
		sentenceID = 0;
		super.clear();
	}
	
	public void update(Observable  o, Object str) { }
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (int index : terminalNodes.keySet()) {
			sb.append(terminalNodes.get(index).toString().trim());
			sb.append('\n');
		}
		sb.append("\n");
		return sb.toString();
	}
}
