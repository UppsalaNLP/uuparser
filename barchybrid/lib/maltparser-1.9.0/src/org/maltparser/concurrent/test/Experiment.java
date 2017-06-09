package org.maltparser.concurrent.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/**
* 
* @author Johan Hall
*/
public class Experiment {
	private final String modelName;
	private final URL modelURL;
	private final URL dataFormatURL;
	private final String charSet;
	private final List<URL> inURLs;
	private final List<File> outFiles;
	
	public Experiment(String _modelName, URL _modelURL, URL _dataFormatURL, String _charSet, List<URL> _inURLs, List<File> _outFiles) throws ExperimentException {
		this.modelName = _modelName;
		this.modelURL = _modelURL;
		this.dataFormatURL = _dataFormatURL;
		if (_charSet == null || _charSet.length() == 0) {
			this.charSet = "UTF-8";
		} else {
			this.charSet = _charSet;
		}
		if (_inURLs.size() != _outFiles.size()) {
			throw new ExperimentException("The lists of in-files and out-files must match in size.");
		}
		this.inURLs = Collections.synchronizedList(new ArrayList<URL>(_inURLs));
		this.outFiles = Collections.synchronizedList(new ArrayList<File>(_outFiles));
	}
	
	public Experiment(String _modelName, String _modelFileName, String _dataFormatFileName, String _charSet, List<String> _inFileNames, List<String> _outFileNames) throws ExperimentException {
		this.modelName = _modelName; 
		
		try {
			this.modelURL = new File(_modelFileName).toURI().toURL();
		} catch (MalformedURLException e) {
			throw new ExperimentException("The model file name is malformed", e);
		}
		
		try {
			this.dataFormatURL = new File(_dataFormatFileName).toURI().toURL();
		} catch (MalformedURLException e) {
			throw new ExperimentException("The data format file name is malformed", e);
		}
		
		if (_charSet == null || _charSet.length() == 0) {
			this.charSet = "UTF-8";
		} else {
			this.charSet = _charSet;
		}
		
		if (_inFileNames.size() != _outFileNames.size()) {
			throw new ExperimentException("The lists of in-files and out-files must match in size.");
		}
		
		this.inURLs = Collections.synchronizedList(new ArrayList<URL>());
		for (int i = 0; i < _inFileNames.size(); i++) {
			try {
				this.inURLs.add(new File(_inFileNames.get(i)).toURI().toURL());
			} catch (MalformedURLException e) {
				throw new ExperimentException("The in file name is malformed", e);
			}
		}
		
		this.outFiles = Collections.synchronizedList(new ArrayList<File>());
		for (int i = 0; i < _outFileNames.size(); i++) {
			this.outFiles.add(new File(_outFileNames.get(i)));
		}
	}
	
	public String getModelName() {
		return modelName;
	}

	public URL getModelURL() {
		return modelURL;
	}

	public URL getDataFormatURL() {
		return dataFormatURL;
	}

	public String getCharSet() {
		return charSet;
	}

	public List<URL> getInURLs() {
		return Collections.synchronizedList(new ArrayList<URL>(inURLs)); 
	}

	public List<File> getOutFiles() {
		return Collections.synchronizedList(new ArrayList<File>(outFiles));
	}
	
	public int nInURLs() {
		return inURLs.size();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((charSet == null) ? 0 : charSet.hashCode());
		result = prime * result + ((dataFormatURL == null) ? 0 : dataFormatURL.hashCode());
		result = prime * result + ((inURLs == null) ? 0 : inURLs.hashCode());
		result = prime * result + ((modelName == null) ? 0 : modelName.hashCode());
		result = prime * result + ((modelURL == null) ? 0 : modelURL.hashCode());
		result = prime * result + ((outFiles == null) ? 0 : outFiles.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Experiment other = (Experiment) obj;
		if (charSet == null) {
			if (other.charSet != null)
				return false;
		} else if (!charSet.equals(other.charSet))
			return false;
		if (dataFormatURL == null) {
			if (other.dataFormatURL != null)
				return false;
		} else if (!dataFormatURL.equals(other.dataFormatURL))
			return false;
		if (inURLs == null) {
			if (other.inURLs != null)
				return false;
		} else if (!inURLs.equals(other.inURLs))
			return false;
		if (modelName == null) {
			if (other.modelName != null)
				return false;
		} else if (!modelName.equals(other.modelName))
			return false;
		if (modelURL == null) {
			if (other.modelURL != null)
				return false;
		} else if (!modelURL.equals(other.modelURL))
			return false;
		if (outFiles == null) {
			if (other.outFiles != null)
				return false;
		} else if (!outFiles.equals(other.outFiles))
			return false;
		return true;
	}
	
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("#STARTEXP");sb.append('\n');
		sb.append("MODELNAME:");sb.append(modelName);sb.append('\n');
		sb.append("MODELURL:");sb.append(modelURL);sb.append('\n');
		sb.append("DATAFORMATURL:");sb.append(dataFormatURL);sb.append('\n');
		sb.append("CHARSET:");sb.append(charSet);sb.append('\n');
		sb.append("INURLS");sb.append('\n');
		for (int i = 0; i < inURLs.size(); i++) {
			sb.append(inURLs.get(i).toExternalForm());sb.append('\n');
		}
		sb.append("OUTFILES");sb.append('\n');
		for (int i = 0; i < outFiles.size(); i++) {
			sb.append(outFiles.get(i));sb.append('\n');
		}
		sb.append("#ENDEXP");sb.append('\n');
		return sb.toString();
	}
	
	public static List<Experiment> loadExperiments(String experimentsFileName) throws MalformedURLException, IOException, ExperimentException {
		return loadExperiments(new File(experimentsFileName).toURI().toURL());
	}
	
	public static List<Experiment> loadExperiments(URL experimentsURL) throws IOException, ExperimentException {
		List<Experiment> experiments = Collections.synchronizedList(new ArrayList<Experiment>());

		BufferedReader reader  = new BufferedReader(new InputStreamReader(experimentsURL.openStream(), "UTF-8"));
		String line;
		boolean read_expdesc = false;
		int read_inouturls = 0;
		String modelName = null;
		URL modelURL = null;
		URL dataFormatURL = null;
		String charSet = null;
		List<URL> inURLs = new ArrayList<URL>();
		List<File> outFiles = new ArrayList<File>();
		while ((line = reader.readLine()) != null) {
//			System.out.println(line);
			if (line.trim().equals("#STARTEXP")) {
				read_expdesc = true;
			} else if (line.trim().toUpperCase().startsWith("MODELNAME") && read_expdesc) {
				modelName = line.trim().substring(line.trim().indexOf(':') + 1);
			} else if (line.trim().toUpperCase().startsWith("MODELURL") && read_expdesc) {
				modelURL = new URL(line.trim().substring(line.trim().indexOf(':') + 1));
			} else if (line.trim().toUpperCase().startsWith("MODELFILE") && read_expdesc) {
				modelURL = new File(line.trim().substring(line.trim().indexOf(':') + 1)).toURI().toURL();
			} else if (line.trim().toUpperCase().startsWith("DATAFORMATURL") && read_expdesc) {
				dataFormatURL = new URL(line.trim().substring(line.trim().indexOf(':') + 1));
			} else if (line.trim().toUpperCase().startsWith("DATAFORMATFILE") && read_expdesc) {
				dataFormatURL = new File(line.trim().substring(line.trim().indexOf(':') + 1)).toURI().toURL();
			} else if (line.trim().toUpperCase().startsWith("CHARSET") && read_expdesc) {
				charSet = line.trim().substring(line.trim().indexOf(':') + 1);
			} else if (line.trim().toUpperCase().startsWith("INURLS") && read_expdesc) {
				read_inouturls = 1;
			} else if (line.trim().toUpperCase().startsWith("INFILES") && read_expdesc) {
				read_inouturls = 2;
			} else if (line.trim().toUpperCase().startsWith("OUTFILES") && read_expdesc) {
				read_inouturls = 3;
			} else if (read_expdesc && !line.trim().equals("#ENDEXP")) {
				if (read_inouturls == 1) {
					inURLs.add(new URL(line.trim()));
				} else if (read_inouturls == 2) {
					inURLs.add(new File(line.trim()).toURI().toURL());
				} else if (read_inouturls == 3) {
					outFiles.add(new File(line.trim()));
				}
			} else if (line.trim().equals("#ENDEXP") && read_expdesc) {
//				System.out.println(modelName);
//				System.out.println(modelURL);
//				System.out.println(dataFormatURL);
//				System.out.println(charSet);
//				System.out.println(inURLs);
//				System.out.println(outURLs);
				if (inURLs.size() > 0 && outFiles.size() > 0) {
					experiments.add(new Experiment(modelName, modelURL, dataFormatURL, charSet, inURLs, outFiles));
				}
				modelName = charSet = null;
				modelURL = dataFormatURL = null;
				inURLs.clear();
				outFiles.clear();
				read_expdesc = false;
				read_inouturls = 0;
			}
		}

		return experiments;
	}
}
