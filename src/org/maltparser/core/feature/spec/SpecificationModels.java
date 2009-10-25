package org.maltparser.core.feature.spec;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.maltparser.core.config.ConfigurationDir;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.FeatureException;
import org.maltparser.core.feature.spec.reader.FeatureSpecReader;
import org.maltparser.core.feature.spec.reader.ParReader;
import org.maltparser.core.helper.Util;

/**
*
*
* @author Johan Hall
*/
public class SpecificationModels {
	private HashMap<URL, FeatureSpecReader> specReaderMap;
	private HashMap<String, SpecificationModel> specModelMap;
	private HashMap<Integer, SpecificationModel> specModelIntMap;
	private LinkedHashMap<URL, ArrayList<SpecificationModel>> specModelKeyMap;
	private ArrayList<SpecificationModel> currentSpecModelURL;
	private int counter = 0;
	private ConfigurationDir configDirectory;
//	private Configuration configuration;
	
	public SpecificationModels(ConfigurationDir configDirectory) throws MaltChainedException {
//	public SpecificationModels(Configuration configuration) throws MaltChainedException {
		setConfigDirectory(configDirectory);
		specReaderMap = new HashMap<URL, FeatureSpecReader>();
		specModelMap = new HashMap<String, SpecificationModel>();
		specModelIntMap = new HashMap<Integer, SpecificationModel>();
		specModelKeyMap = new LinkedHashMap<URL, ArrayList<SpecificationModel>>();
//		setConfiguration(configuration);
	}
	
	public void add(int index, String featureSpec) throws MaltChainedException {
		this.add(Integer.toString(index), "MAIN", featureSpec);
	}
	
	public void add(String specModelName, String featureSpec) throws MaltChainedException {
		this.add(specModelName, "MAIN", featureSpec);
	}
	
	public void add(int index, String subModelName, String featureSpec) throws MaltChainedException {
		this.add(Integer.toString(index), subModelName, featureSpec);
	}
	
	public void add(String specModelName, String subModelName, String featureSpec) throws MaltChainedException {
		if (featureSpec == null) { throw new FeatureException("Feature specification is missing."); }
		if (specModelName == null) {throw new FeatureException("Unknown feature model name."); }
		if (subModelName == null) {throw new FeatureException("Unknown subfeature model name."); }
		
		if (!specModelMap.containsKey(specModelName.toUpperCase())) {
			SpecificationModel specModel = new SpecificationModel(specModelName.toUpperCase());
			specModelMap.put(specModelName.toUpperCase(), specModel);
			currentSpecModelURL.add(specModel);
			specModelIntMap.put(counter++, specModel);
		}
		specModelMap.get(specModelName.toUpperCase()).add(subModelName, featureSpec);
	}
	
	public int getNextIndex() {
		return counter;
	}
	
	public void loadParReader(String specModelFileName, boolean malt04, String markingStrategy, String coveredRoot) throws MaltChainedException {
		URL url = Util.findURL(specModelFileName);
		if (url == null) {
			int indexSlash = specModelFileName.lastIndexOf('/');
			if (indexSlash == -1) {
				indexSlash = specModelFileName.lastIndexOf('\\');
			}
			if (indexSlash == -1) {
				url = Util.findURL(configDirectory.getFile(specModelFileName).getAbsolutePath());
			} else {
				url = Util.findURL(configDirectory.getFile(specModelFileName.substring(indexSlash+1)).getAbsolutePath());
			}
		}
		loadParReader(url, malt04, markingStrategy, coveredRoot);
	}
	
	public void loadParReader(URL specModelURL, boolean malt04, String markingStrategy, String coveredRoot) throws MaltChainedException {
		if (specModelURL == null) {
			throw new FeatureException("The URL to the feature specification model is missing or not well-formed. ");
		}
		FeatureSpecReader specReader = null;
		String urlSuffix = specModelURL.toString().substring(specModelURL.toString().length()-3);
		urlSuffix = Character.toUpperCase(urlSuffix.charAt(0)) + urlSuffix.substring(1);
		try {
			Class<?> clazz = Class.forName("org.maltparser.core.feature.spec.reader."+urlSuffix+"Reader");
			specReader = (FeatureSpecReader)clazz.newInstance();
		} catch (InstantiationException e) {
			throw new FeatureException("Could not initialize the feature specification reader to read the specification file: "+specModelURL.toString(), e);
		} catch (IllegalAccessException e) {
			throw new FeatureException("Could not initialize the feature specification reader to read the specification file: "+specModelURL.toString(), e);
		} catch (ClassNotFoundException e) {
			throw new FeatureException("Could not find the feature specification reader to read the specification file: "+specModelURL.toString(), e);
		}
		specReaderMap.put(specModelURL, specReader);
		
		if (specReader instanceof ParReader) {
			if (malt04) {
				((ParReader)specReader).setMalt04Emulation(true);
			}
			
			if (markingStrategy.equalsIgnoreCase("head") || markingStrategy.equalsIgnoreCase("path") || markingStrategy.equalsIgnoreCase("head+path")) {
				((ParReader)specReader).setPplifted(true);
			}
			if (markingStrategy.equalsIgnoreCase("path") || markingStrategy.equalsIgnoreCase("head+path")) {
				((ParReader)specReader).setPppath(true);
			}
			if (!coveredRoot.equalsIgnoreCase("none")) {
				((ParReader)specReader).setPpcoveredRoot(true);
			}
		}
		currentSpecModelURL = new ArrayList<SpecificationModel>();
		specModelKeyMap.put(specModelURL, currentSpecModelURL);
		specReader.load(specModelURL, this);
	}
	
	public void load(String specModelFileName) throws MaltChainedException {
		URL url = Util.findURL(specModelFileName);
		if (url == null) {
			int indexSlash = specModelFileName.lastIndexOf('/');
			if (indexSlash == -1) {
				indexSlash = specModelFileName.lastIndexOf('\\');
			}
			if (indexSlash == -1) {
				url = Util.findURL(configDirectory.getFile(specModelFileName).getAbsolutePath());
			} else {
				url = Util.findURL(configDirectory.getFile(specModelFileName.substring(indexSlash+1)).getAbsolutePath());
			}
		}
		load(url);
	}
	
	
	
	public void load(URL specModelURL) throws MaltChainedException {
		if (specModelURL == null) {
			throw new FeatureException("The URL to the feature specification model is missing or not well-formed. ");
		}
		FeatureSpecReader specReader = null;
		String urlSuffix = specModelURL.toString().substring(specModelURL.toString().length()-3);
		urlSuffix = Character.toUpperCase(urlSuffix.charAt(0)) + urlSuffix.substring(1);
		try {
			Class<?> clazz = Class.forName("org.maltparser.core.feature.spec.reader."+urlSuffix+"Reader");
			specReader = (FeatureSpecReader)clazz.newInstance();
		} catch (InstantiationException e) {
			throw new FeatureException("Could not initialize the feature specification reader to read the specification file: "+specModelURL.toString(), e);
		} catch (IllegalAccessException e) {
			throw new FeatureException("Could not initialize the feature specification reader to read the specification file: "+specModelURL.toString(), e);
		} catch (ClassNotFoundException e) {
			throw new FeatureException("Could not find the feature specification reader to read the specification file: "+specModelURL.toString(), e);
		}
		specReaderMap.put(specModelURL, specReader);
		
//		if (specReader instanceof ParReader) {
//			if ((Boolean)configuration.getOptionValue("malt0.4", "behavior").equals(true)) {
//				((ParReader)specReader).setMalt04Emulation(true);
//			}
//			if (configuration.getOptionValueString("singlemalt", "parsing_algorithm").equals("covnonproj") ||
//					configuration.getOptionValueString("singlemalt", "parsing_algorithm").equals("covproj")) {
//				((ParReader)specReader).setCovington(true);
//			}
//			String markingStrategy = configuration.getOptionValue("pproj", "marking_strategy").toString().trim();
//			String coveredRoot = configuration.getOptionValue("pproj", "covered_root").toString().trim();
//
//			if (markingStrategy.equalsIgnoreCase("head") || markingStrategy.equalsIgnoreCase("path") || markingStrategy.equalsIgnoreCase("head+path")) {
//				((ParReader)specReader).setPplifted(true);
//			}
//			if (markingStrategy.equalsIgnoreCase("path") || markingStrategy.equalsIgnoreCase("head+path")) {
//				((ParReader)specReader).setPppath(true);
//			}
//			if (!coveredRoot.equalsIgnoreCase("none")) {
//				((ParReader)specReader).setPpcoveredRoot(true);
//			}
//		}
		currentSpecModelURL = new ArrayList<SpecificationModel>();
		specModelKeyMap.put(specModelURL, currentSpecModelURL);
		specReader.load(specModelURL, this);
	}
	
	public SpecificationModel getSpecificationModel(String specModelURL, int specModelUrlIndex) throws MaltChainedException {
		URL url = Util.findURL(specModelURL);
		if (url == null) {
			int indexSlash = specModelURL.lastIndexOf('/');
			if (indexSlash == -1) {
				indexSlash = specModelURL.lastIndexOf('\\');
			}
			if (indexSlash == -1) {
				url = Util.findURL(configDirectory.getFile(specModelURL).getAbsolutePath());
			} else {
				url = Util.findURL(configDirectory.getFile(specModelURL.substring(indexSlash+1)).getAbsolutePath());
			}
		}
		return specModelKeyMap.get(url).get(specModelUrlIndex);
	}
	
	
//	public Configuration getConfiguration() {
//		return configuration;
//	}
//
//	public void setConfiguration(Configuration configuration) {
//		this.configuration = configuration;
//	}

	public ConfigurationDir getConfigDirectory() {
		return configDirectory;
	}

	public void setConfigDirectory(ConfigurationDir configDirectory) {
		this.configDirectory = configDirectory;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();

		for (URL url : specModelKeyMap.keySet()) {
			for (int i = 0; i < specModelKeyMap.get(url).size(); i++) {
				sb.append(url.toString());
				sb.append(':');
				sb.append(i);
				sb.append('\n');
				sb.append(specModelKeyMap.get(url).get(i).toString());
			}
		}
		return sb.toString();
	}
}
