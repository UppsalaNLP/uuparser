package org.maltparser.concurrent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.maltparser.core.exception.MaltChainedException;

public class MaltParserRunnable implements Runnable {
	private final List<String[]> inputSentences;
	private List<String[]> outputSentences;
	private final ConcurrentMaltParserModel model;
	
	public MaltParserRunnable(List<String[]> sentences, ConcurrentMaltParserModel _model) {
		this.inputSentences = new ArrayList<String[]>(sentences);
		this.outputSentences = null;
		this.model = _model;
	}
	
	public void run() {
		try {
			outputSentences = model.parseSentences(inputSentences);
		} catch (MaltChainedException e) {
			e.printStackTrace();
		}
//		for (int i = 0; i < inputSentences.size(); i++) {
//			try {
//				outputSentences.add(model.parseTokens(inputSentences.get(i)));
//			} catch (MaltChainedException e) {
//				e.printStackTrace();
//			}
//		}
	}
	
	public List<String[]> getOutputSentences() {
		if (outputSentences == null) {
			return Collections.synchronizedList(new ArrayList<String[]>());
		}
		return Collections.synchronizedList(new ArrayList<String[]>(outputSentences));
	}
}
