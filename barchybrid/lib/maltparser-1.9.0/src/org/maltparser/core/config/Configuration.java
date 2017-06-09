package org.maltparser.core.config;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.SymbolTableHandler;

/**
*
*
* @author Johan Hall
*/
public interface Configuration {
//	public ConfigurationDir getConfigurationDir();
//	public void setConfigurationDir(ConfigurationDir configDir);
//	public Logger getConfigLogger(); 
//	public void setConfigLogger(Logger logger); 
	public boolean isLoggerInfoEnabled();
	public boolean isLoggerDebugEnabled();
	public void logErrorMessage(String message);
	public void logInfoMessage(String message);
	public void logInfoMessage(char character);
	public void logDebugMessage(String message);
	public void writeInfoToConfigFile(String message) throws MaltChainedException;
	
	public OutputStreamWriter getOutputStreamWriter(String fileName) throws MaltChainedException;
	public OutputStreamWriter getAppendOutputStreamWriter(String fileName) throws MaltChainedException;
	public InputStreamReader getInputStreamReader(String fileName) throws MaltChainedException;
	public InputStream getInputStreamFromConfigFileEntry(String fileName) throws MaltChainedException;
	public URL getConfigFileEntryURL(String fileName) throws MaltChainedException;
	public File getFile(String fileName) throws MaltChainedException;
	public Object getConfigFileEntryObject(String fileName) throws MaltChainedException;
	public String getConfigFileEntryString(String fileName) throws MaltChainedException;
	public SymbolTableHandler getSymbolTables();
//	public ConfigurationRegistry getRegistry();
//	public void addRegistry(Class<?> clazz, Object o);
	public Object getOptionValue(String optiongroup, String optionname) throws MaltChainedException;
	public String getOptionValueString(String optiongroup, String optionname) throws MaltChainedException;
}
