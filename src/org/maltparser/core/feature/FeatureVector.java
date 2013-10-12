package org.maltparser.core.feature;

import java.io.Serializable;
import java.util.ArrayList;

import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.feature.function.FeatureFunction;
import org.maltparser.core.feature.spec.SpecificationSubModel;
import org.maltparser.core.feature.value.FeatureValue;

/**
*
*
* @author Johan Hall
*/
public class FeatureVector extends ArrayList<FeatureFunction> implements Serializable {
	public final static long serialVersionUID = 3256444702936019250L;
	private final SpecificationSubModel specSubModel;
	private final FeatureModel featureModel;

	
	/**
	 * Constructs a feature vector
	 * 
	 * @param _featureModel	the parent feature model
	 * @param _specSubModel	the subspecifiction-model
	 * @throws MaltChainedException
	 */
	public FeatureVector(FeatureModel _featureModel, SpecificationSubModel _specSubModel) throws MaltChainedException {
		this.specSubModel = _specSubModel;
		this.featureModel = _featureModel;
		for (String spec : specSubModel) {
			add(featureModel.identifyFeature(spec));	
		}
	}
	
	/**
	 * Returns the subspecifiction-model.
	 * 
	 * @return the subspecifiction-model
	 */
	public SpecificationSubModel getSpecSubModel() {
		return specSubModel;
	}
	
	/**
	 * Returns the feature model that the feature vector belongs to.
	 * 
	 * @return the feature model that the feature vector belongs to
	 */
	public FeatureModel getFeatureModel() {
		return featureModel;
	}
	
	/**
	 * Updates all feature value in the feature vector according to the current state.
	 * 
	 * @throws MaltChainedException
	 */
	public void update() throws MaltChainedException {
		final int size =  size();
		for (int i = 0; i < size; i++) {
			get(i).update();
		}
	}
	
	
	public FeatureValue getFeatureValue(int index) {
		if (index < 0 || index >= size()) {
			return null;
		}
		return get(index).getFeatureValue();
	}
	
	public FeatureValue[] getFeatureValues() {
		final int size = size();
		FeatureValue[] featureValues = new FeatureValue[size];
		for (int i = 0; i < size; i++) {
			featureValues[i] = get(i).getFeatureValue();
		}
		return featureValues;
	}
	
	/* (non-Javadoc)
	 * @see java.util.AbstractCollection#toString()
	 */
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (FeatureFunction function : this) {
			if (function != null) {
				sb.append(function.getFeatureValue().toString());
				sb.append('\n');
			}
		}
		return sb.toString();
	}
}
