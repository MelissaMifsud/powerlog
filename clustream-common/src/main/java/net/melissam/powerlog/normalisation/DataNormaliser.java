package net.melissam.powerlog.normalisation;

import net.melissam.powerlog.clustering.FeatureVector;


/**
 * Interface for implementing different data normalisation techniques.
 * 
 * @author melissam
 *
 */
public interface DataNormaliser {
	
	/**
	 * Setup normalisation.
	 */
	void setup();
	
	
	/**
	 * Normalise a given feature vector.
	 * 
	 * @param featureVector The feature vector to normalise
	 * @return	The normalised feature vector.
	 */
	void normalise(FeatureVector featureVector);

}
