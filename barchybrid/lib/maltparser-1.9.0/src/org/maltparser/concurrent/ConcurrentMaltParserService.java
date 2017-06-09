package org.maltparser.concurrent;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.lw.helper.Utils;
import org.maltparser.core.options.OptionManager;

/**
* The purpose of ConcurrentMaltParserService is to provide an interface to MaltParser that makes it easier to parse sentences from 
* other programs. ConcurrentMaltParserService is hopefully thread-safe and can be used in a multi threaded environment. This class 
* replace the old interface org.maltparser.MaltParserService when you want to load a MaltParser model and then use it 
* to parse sentences in both a single threaded or multi threaded application. 
* 
* How to use ConcurrentMaltParserService, please see the examples provided in the directory 'examples/apiexamples/srcex'
* 
* @author Johan Hall
*/
public final class ConcurrentMaltParserService {
	private static int optionContainerCounter = 0;

    
	private static synchronized int getNextOptionContainerCounter() {
		return optionContainerCounter++;
	}
	
	private static synchronized void loadOptions() throws MaltChainedException {
    	if (!OptionManager.instance().hasOptions()) {
    		OptionManager.instance().loadOptionDescriptionFile();
    		OptionManager.instance().generateMaps();
    	}
	}
	
    /**
     * Initialize a MaltParser model from a MaltParser model file (.mco)
     * 
     * @param mcoFile File object containing the path to a valid MaltParser model file. Usually the file extension is ".mco" 
     * @return MaltParser model
     * @throws MaltChainedException
     * @throws MalformedURLException
     */
    public static ConcurrentMaltParserModel initializeParserModel(File mcoFile) throws MaltChainedException, MalformedURLException {
        return initializeParserModel(mcoFile.toURI().toURL());
    }
    
    /**
     * Initialize a MaltParser model from a MaltParser model file (.mco)
     * 
     * @param mcoURL URL to a valid MaltParser model file. Usually the file extension is ".mco"  
     * @return a concurrent MaltParser model
     * @throws MaltChainedException
     */
    public static ConcurrentMaltParserModel initializeParserModel(URL mcoURL) throws MaltChainedException {
    	loadOptions();
    	int optionContainer = getNextOptionContainerCounter();
    	String parserModelName = Utils.getInternalParserModelName(mcoURL);
		OptionManager.instance().parseCommandLine("-m parse", optionContainer);
        OptionManager.instance().loadOptions(optionContainer, Utils.getInputStreamReaderFromConfigFileEntry(mcoURL, parserModelName, "savedoptions.sop", "UTF-8"));
        return new ConcurrentMaltParserModel(optionContainer, mcoURL);
    }
}
