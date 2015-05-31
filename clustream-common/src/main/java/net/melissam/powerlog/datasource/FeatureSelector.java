package net.melissam.powerlog.datasource;

import net.melissam.powerlog.clustering.FeatureVector;

/**
 * 
 * Interface for feature selectors from any data source.
 * 
 * @author melissam
 *
 */
public interface FeatureSelector {

	FeatureVector getNext();
	
	int getFeatureCount();
	
	void restart() throws Exception;
	
}
