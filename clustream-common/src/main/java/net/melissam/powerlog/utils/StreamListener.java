package net.melissam.powerlog.utils;

import net.melissam.powerlog.clustering.FeatureVector;

public interface StreamListener {

	void onItemReceived(FeatureVector featureVector);
	
}
