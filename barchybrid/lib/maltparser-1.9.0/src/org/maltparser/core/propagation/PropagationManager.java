package org.maltparser.core.propagation;

import java.net.URL;


import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.propagation.spec.PropagationSpecs;
import org.maltparser.core.propagation.spec.PropagationSpecsReader;
import org.maltparser.core.symbol.SymbolTableHandler;
import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.core.io.dataformat.DataFormatInstance;

public class PropagationManager {
	private final PropagationSpecs propagationSpecs;
	private Propagations propagations;
	
	public PropagationManager() {
		propagationSpecs = new PropagationSpecs();
	}
	
	public void loadSpecification(URL propagationSpecURL) throws MaltChainedException {
		PropagationSpecsReader reader = new PropagationSpecsReader();
		reader.load(propagationSpecURL, propagationSpecs);
	}
	
	public void createPropagations(DataFormatInstance dataFormatInstance, SymbolTableHandler tableHandler) throws MaltChainedException {
		propagations = new Propagations(propagationSpecs, dataFormatInstance, tableHandler);
	}
	
	public void propagate(Edge e) throws MaltChainedException {
		if (propagations != null && e != null) {
			propagations.propagate(e);
		}
	}
	
	public PropagationSpecs getPropagationSpecs() {
		return propagationSpecs;
	}

	public Propagations getPropagations() {
		return propagations;
	}
}
