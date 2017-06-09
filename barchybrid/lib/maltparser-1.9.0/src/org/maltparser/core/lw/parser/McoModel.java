package org.maltparser.core.lw.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.maltparser.core.helper.HashMap;
import org.maltparser.core.helper.HashSet;

/**
 * @author Johan Hall
 *
 */
public final class McoModel {
	private final URL mcoUrl;
	private final Map<String, URL> nameUrlMap;
	private final Map<String, Object> preLoadedObjects;
	private final Map<String, String> preLoadedStrings;
	private final URL infoURL;
	private final String internalMcoName;

	
	public McoModel(URL _mcoUrl) { 
		this.mcoUrl = _mcoUrl;
		this.nameUrlMap = Collections.synchronizedMap(new HashMap<String, URL>());
		this.preLoadedObjects = Collections.synchronizedMap(new HashMap<String, Object>());
		this.preLoadedStrings = Collections.synchronizedMap(new HashMap<String, String>());
		URL tmpInfoURL = null;
		String tmpInternalMcoName = null;
		try {
			JarEntry je;
			JarInputStream jis = new JarInputStream(mcoUrl.openConnection().getInputStream());

			while ((je = jis.getNextJarEntry()) != null) {
				String fileName = je.getName();
				URL entryURL = new URL("jar:"+mcoUrl+"!/"+fileName + "\n");
				int index = fileName.indexOf('/');
				if (index == -1) {
					index = fileName.indexOf('\\');
				}				
				nameUrlMap.put(fileName.substring(index+1), entryURL);
				if (fileName.endsWith(".info") && tmpInfoURL == null) {
					tmpInfoURL = entryURL;
				} else if (fileName.endsWith(".moo") || fileName.endsWith(".map")) {
					preLoadedObjects.put(fileName.substring(index+1), preLoadObject(entryURL.openStream()));
				} else if (fileName.endsWith(".dsm")) {
					preLoadedStrings.put(fileName.substring(index+1), preLoadString(entryURL.openStream()));
				}
				if (tmpInternalMcoName == null) {
					tmpInternalMcoName = fileName.substring(0, index);
				}
				jis.closeEntry();
			}
			jis.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		this.internalMcoName = tmpInternalMcoName;
		this.infoURL = tmpInfoURL;
	}
	
	private Object preLoadObject(InputStream is) throws IOException, ClassNotFoundException {
		Object object = null;
		
	    ObjectInputStream input = new ObjectInputStream(is);
	    try {
	    	object = input.readObject();
	    } finally {
	    	input.close();
	    }
	    return object;
	}
	
	private String preLoadString(InputStream is) throws IOException, ClassNotFoundException {
		final BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		String line;
		StringBuilder sb = new StringBuilder();
		
		while((line = in.readLine()) != null) {
			 sb.append(line);
			 sb.append('\n');
		}
	    return sb.toString();
	}
	
	public InputStream getInputStream(String fileName) throws IOException {
		return nameUrlMap.get(fileName).openStream();
	}
	
	public InputStreamReader getInputStreamReader(String fileName, String charSet) throws IOException, UnsupportedEncodingException {
		return new InputStreamReader(getInputStream(fileName),  charSet);
	}
	
	public URL getMcoEntryURL(String fileName) throws MalformedURLException {
		return new URL(nameUrlMap.get(fileName).toString());
	}
	
	public URL getMcoURL() throws MalformedURLException {
		return new URL(mcoUrl.toString());
	}
	
	public Object getMcoEntryObject(String fileName) {
		return preLoadedObjects.get(fileName);
	}
	
	public Set<String> getMcoEntryObjectKeys() {
		return Collections.synchronizedSet(new HashSet<String>(preLoadedObjects.keySet()));
	}
	
	public String getMcoEntryString(String fileName) {
		return preLoadedStrings.get(fileName);
	}
	
	public String getInternalName() {
		return internalMcoName;
	}
	
	public String getMcoURLString() {
		return mcoUrl.toString();
	}
	
	public String getMcoInfo() throws IOException {
		StringBuilder sb = new StringBuilder();

		BufferedReader reader = new BufferedReader(new InputStreamReader(infoURL.openStream(), "UTF-8"));
		String line;
		while ((line = reader.readLine()) != null) {
			sb.append(line);
			sb.append('\n');
		}

		return sb.toString();
	}
}
