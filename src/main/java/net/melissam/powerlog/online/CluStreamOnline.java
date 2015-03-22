package net.melissam.powerlog.online;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;

import net.melissam.powerlog.common.FeatureVector;
import net.melissam.powerlog.common.StreamListener;

/**
 * Online phase of the CluStream algorithm.
 * 
 * Uses kMeansPlusPlus for initialisation. We may want to make this configurable to see the difference.
 * 
 * @author melissam
 *
 */
public class CluStreamOnline implements StreamListener{

	/** Maximum number of clusters to exist at any given time. */
	private int maxClusters;
	
	/** Factor to use for calculating the maximum boundary of a micro-cluster. */
	private double t;
	
	/** Number of points to calculate the recency timestamp of this cluster on. */
	private double m;
	
	/** Create clusters. */
	private List<MicroCluster> clusters;
	
	/** The points to initialise clusters on. */
	private List<FeatureVector> initialisationPoints;
	
	/** The number of points to use for initialisation. */
	private int initNumber;
	
	/** Threshold to use for chosing which micro-clusters can be eliminated. */
	private double relevanceThreshold;
	
	/** Whether the clusters have been initialised or not. */
	private boolean initialised;
		
	public CluStreamOnline(int maxClusters, double t, double relevanceThreshold, int initNumber){
		
		this.maxClusters = maxClusters;
		this.t = t;
		
		this.clusters = new ArrayList<MicroCluster>(maxClusters);
		
		this.relevanceThreshold = relevanceThreshold;
		
		// initialisation properties
		this.initNumber = initNumber;
		this.initialisationPoints = new ArrayList<FeatureVector>(initNumber); 

	}
	
	
	public void onItemReceived(FeatureVector featureVector) {
		
		if (!initialised){
			
			// first check if all initial clusters have been created
			if (this.initialisationPoints.size() < initNumber){
				initialisationPoints.add(featureVector);	
				return;
			}
			
			// otherwise let's use a kmeans algorithm on the initial clusters (k-nearest-neighbour)
			List<CentroidCluster<FeatureVector>> clusters = kmeans(initialisationPoints, maxClusters);
			for (CentroidCluster<FeatureVector> cluster : clusters){
				this.clusters.add(new MicroCluster(cluster.getCenter().getPoint(), 0L, t, m));
			}
			
		}		
		
		// otherwise, let's choose a cluster to add this feature vector to
				
		// keep track of the distance of the feature vector to each cluster
		TreeMap<Double, MicroCluster> distances = new TreeMap<Double, MicroCluster>();
		// compute the distances
		for(MicroCluster cluster : clusters){			
			distances.put(cluster.getDistance(featureVector.getPoint()), cluster);
		}
		
		// take the closest micro-cluster
		Entry<Double, MicroCluster> closest = distances.firstEntry();
		
		// if the point's distrance is within the maximum boundary of the cluster, then we can add the point
		if (closest.getKey() < closest.getValue().getMaximumBoundary()){
			closest.getValue().addFeatureVector(featureVector.getPoint(), featureVector.getTimestamp());
		}else{
			
			// decide to delete old cluster or merge 2 clusters
			
			// get the relevance timestamp of each micro-cluster
			// the micro-cluster with the eldest relevance timestamp below the threshold
	
			// if all the clusters are within the threshold, then merge the 2 closest clusters	
			
		}
		
	}
	
	
	/**
	 * Use KMeansPlusPlus clustering on the initialisation points to create maxClusters clusters.
	 * @param points			The points to cluster.
	 * @param maxClusters		The desired number of clusters.
	 * @return
	 */
	private List<CentroidCluster<FeatureVector>> kmeans(List<FeatureVector> points, int maxClusters){
		
		KMeansPlusPlusClusterer<FeatureVector> clusterer = new KMeansPlusPlusClusterer<FeatureVector>(maxClusters);
		return clusterer.cluster(points);

	}
	

}
