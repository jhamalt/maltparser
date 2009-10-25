package org.maltparser.core.feature;

import java.net.URL;

import org.maltparser.core.config.ConfigurationDir;
import org.maltparser.core.config.ConfigurationRegistry;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.spec.SpecificationModel;
import org.maltparser.core.feature.spec.SpecificationModels;
import org.maltparser.core.feature.system.FeatureEngine;

/**
*
*
* @author Johan Hall
*/
public class FeatureModelManager {
	protected SpecificationModels specModels;
	protected FeatureEngine featureEngine;
	
	public FeatureModelManager(FeatureEngine engine, ConfigurationDir configDirectory) throws MaltChainedException {
		specModels = new SpecificationModels(configDirectory);
		setFeatureEngine(engine);
	}
	
	public void loadSpecification(String specModelFileName) throws MaltChainedException {
		specModels.load(specModelFileName);
	}
	
	public void loadSpecification(URL specModelURL) throws MaltChainedException {
		specModels.load(specModelURL);
	}
	
	public void loadParSpecification(String specModelFileName, boolean malt04, String markingStrategy, String coveredRoot) throws MaltChainedException {
		specModels.loadParReader(specModelFileName, malt04, markingStrategy, coveredRoot);
	}
	
	public void loadParSpecification(URL specModelURL, boolean malt04, String markingStrategy, String coveredRoot) throws MaltChainedException {
		specModels.loadParReader(specModelURL, malt04, markingStrategy, coveredRoot);
	}
	
	public FeatureModel getFeatureModel(String specModelURL, int specModelUrlIndex, ConfigurationRegistry registry) throws MaltChainedException {
		return new FeatureModel(specModels.getSpecificationModel(specModelURL, specModelUrlIndex), registry, featureEngine);
	}
	
	public FeatureModel getFeatureModel(String specModelURL, ConfigurationRegistry registry) throws MaltChainedException {
		return new FeatureModel(specModels.getSpecificationModel(specModelURL, 0), registry, featureEngine);
	}
	
	public FeatureModel getFeatureModel(SpecificationModel specModel, ConfigurationRegistry registry) throws MaltChainedException {
		return new FeatureModel(specModel, registry, featureEngine);
	}
	
	public SpecificationModels getSpecModels() {
		return specModels;
	}

	protected void setSpecModels(SpecificationModels specModel) {
		this.specModels = specModel;
	}
	
	public FeatureEngine getFeatureEngine() {
		return featureEngine;
	}

	public void setFeatureEngine(FeatureEngine featureEngine) {
		this.featureEngine = featureEngine;
	}

	public String toString() {
		return specModels.toString();
	}
}
