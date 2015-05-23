package net.melissam.powerlog.datasource;

import net.melissam.powerlog.clustering.FeatureVector;


public interface FeatureSelector {

	FeatureVector getNext();
	
	int getFeatureCount();
	
	void restart() throws Exception;
	
}
