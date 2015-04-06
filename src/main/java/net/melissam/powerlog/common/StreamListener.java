package net.melissam.powerlog.common;

import net.melissam.powerlog.clustering.FeatureVector;

public interface StreamListener {

	void onItemReceived(FeatureVector featureVector);
	
}
