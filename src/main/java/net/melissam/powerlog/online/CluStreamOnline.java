package net.melissam.powerlog.online;

import java.util.Map.Entry;
import java.util.TreeMap;

import net.melissam.powerlog.common.FeatureVector;
import net.melissam.powerlog.common.StreamListener;

/**
 * Online phase of the CluStream algorithm.
 * 
 * @author melissam
 *
 */
public class CluStreamOnline implements StreamListener{

	/** Maximum number of clusters to exist at any given time. */
	private int maxClusters;
	
	/** Number of clusters created. */
	private int clusterCount;
	
	/** Factor to use for calculating the maximum boundary of a micro-cluster. */
	private double t;
	
	/** Create clusters. */
	private MicroCluster[] clusters;
		
	public CluStreamOnline(int maxClusters, double t){
		
		this.maxClusters = maxClusters;
		this.t = t;
		
		this.clusters = new MicroCluster[maxClusters];

	}
	
	public void initialiseClusters(){
		// initialisation of clusters 
		// use standard k-means clustering algorithm to create the first q micro-clusters
	}

	public void onItemReceived(FeatureVector featureVector) {
		
		TreeMap<Double, MicroCluster> distances = new TreeMap<Double, MicroCluster>();
		
		// keep track of the distance of the feature vector to each cluster
		for(MicroCluster cluster : clusters){
			
			distances.put(cluster.getDistance(featureVector.toDoubleArray()), cluster);
			
		}
		
		// take the closest micro-cluster
		Entry<Double, MicroCluster> closest = distances.firstEntry();
		
		// if the point's distrance is within the maximum boundary of the cluster, then we can add the point
		if (closest.getKey() < closest.getValue().getMaximumBoundary()){
			closest.getValue().addFeatureVector(featureVector.toDoubleArray(), featureVector.getTimestamp());
		}else{
			
			// decide to delete old cluster or merge 2 clusters
			
				
		}
		
	}

}
