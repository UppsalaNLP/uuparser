package org.maltparser.parser;


public interface AlgoritmInterface {
	public ParserRegistry getParserRegistry();
	public ParserConfiguration getCurrentParserConfiguration();
	public DependencyParserConfig getManager();
}
