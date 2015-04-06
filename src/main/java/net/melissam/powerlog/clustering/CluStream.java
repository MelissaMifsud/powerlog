package net.melissam.powerlog.clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.melissam.powerlog.common.MathUtils;
import net.melissam.powerlog.common.StreamListener;

/**
 * Online phase of the CluStream algorithm.
 * 
 * Uses kMeansPlusPlus for initialisation. We may want to make this configurable to see the difference.
 * 
 * @author melissam
 *
 */
public class CluStream{

	/** Maximum number of clusters to exist at any given time. */
	private int maxClusters;
	
	/** Factor to use for calculating the maximum boundary of a micro-cluster. */
	private double t;
	
	/** Number of points to calculate the recency timestamp of this cluster on. */
	private double m;
	
	/** Create clusters. */
	private List<MicroCluster> clusters;
	
	/** Threshold to use for chosing which micro-clusters can be eliminated. */
	private int relevanceThreshold;
	
	/** Timestamp to keep track of after initialisation. */
	private long timestamp;
	
	/** The points to initialise clusters on. */
	private List<FeatureVector> initialisationPoints;
	
	/** The number of points to use for initialisation. */
	private int initNumber;
	
	/** Whether the clusters have been initialised or not. */
	private boolean initialised;
	
	/** Number of features received. */
	private int numFeatures;
	
	/** Cluster sequence number, serves as id. */
	private int clusterSequence;
	
	/** Class logger. */
	private static final Logger LOG = LogManager.getLogger(CluStream.class);
		
	public CluStream(int maxClusters, double t, int relevanceThreshold, int initNumber){
		
		this.maxClusters = maxClusters;
		this.t = t;
		
		this.clusters = new ArrayList<MicroCluster>(maxClusters);
		
		this.timestamp = -1;
		this.relevanceThreshold = relevanceThreshold;
		
		// initialisation properties
		this.initNumber = initNumber;
		this.initialisationPoints = new ArrayList<FeatureVector>(initNumber); 
		
		this.numFeatures = 0;
		this.clusterSequence = 0;

	}
	
	
	public Map<FeatureVector, Integer> cluster(FeatureVector featureVector) {
		
		// return a mapping of where the feature vector was placed
		// we need to return a map so we can return the initial placements after kMeans clustering
		Map<FeatureVector, Integer> placement = new HashMap<FeatureVector, Integer>();
		
		// let's choose a cluster to add this feature vector to
		featureVector.setTimestamp(++timestamp);	
		
		if (!initialised){
			
			// first check if all initial clusters have been created
			if (this.initialisationPoints.size() < initNumber){
				initialisationPoints.add(featureVector);
				LOG.debug("featurevector={} will be used for initialisation.", featureVector.getId());
				return null;
			}
		
			// otherwise let's use a kmeans algorithm on the initial clusters (k-nearest-neighbour)
			List<CentroidCluster<FeatureVector>> clusters = kmeans(initialisationPoints, maxClusters);
		
			// create the clusters, keeping track of which feature vector went into which cluster
			for (CentroidCluster<FeatureVector> cluster : clusters){
				
				// this is only ok because we know that each cluster contains one point which is the center
				// unfortunately Apache Math kMeans does not maintain a reference to the clusterable object that is the center
				this.clusters.add(new MicroCluster(++clusterSequence, cluster.getCenter().getPoint(), ((FeatureVector)cluster.getPoints().get(0)).getTimestamp(), t, m));
				
				// add all the elements in the cluster
				if (cluster.getPoints() != null){
					for (FeatureVector fv : cluster.getPoints()){
						placement.put(fv, clusterSequence);
					}
				}
			}
			
			initialised = true;
			
			LOG.info("{} clusters initialised.", this.clusters.size());
		
		}				
		
		// keep track of the distance of the feature vector to each cluster
		TreeMap<Double, MicroCluster> distances = new TreeMap<Double, MicroCluster>();
		// compute the distances
		for(MicroCluster cluster : clusters){			
			distances.put(cluster.getDistance(featureVector.getPoint()), cluster);
		}
		
		// take the closest micro-cluster
		Entry<Double, MicroCluster> closest = distances.firstEntry();
		
		// if the point's distance is within the maximum boundary of the cluster, then we can add the point
		if (closest.getKey() < closest.getValue().getMaximumBoundary()){
			closest.getValue().addFeatureVector(featureVector.getPoint(), featureVector.getTimestamp());
			placement.put(featureVector, closest.getValue().getIdList().get(0));
		}else{
			
			// decide to delete old cluster or merge 2 clusters
			
			// old clusters are those before the relevanceThreshold
			long threshold = timestamp - relevanceThreshold;
			
			// get the timestamp of each micro-cluster
			double eldestTimestamp = clusters.get(0).getAverageTimestamp();
			int eldestCluster = 0;
			for (int i=1; i<clusters.size(); i++){
				double timestamp = clusters.get(i).getAverageTimestamp();
				if (timestamp < eldestTimestamp){
					eldestTimestamp = timestamp;
					eldestCluster = i;
				}
			}
			
			// the micro-cluster with the eldest relevance timestamp below the threshold
			if (eldestTimestamp < threshold){
				
				// we can delete the eldest cluster and create a new one
				clusters.remove(eldestCluster);
				clusters.add(new MicroCluster(++clusterSequence, featureVector.getPoint(), featureVector.getTimestamp(), t, m));
				
				placement.put(featureVector, clusterSequence);
				
			}else{
				
				// all the clusters are within the threshold, merge the 2 closest clusters
				int closestCluster1 = 0;
				int closestCluster2 = 0;
				double minDistance = Double.MAX_VALUE;
				for ( int i = 0; i < clusters.size(); i++ ) {
					double[] center1 = clusters.get(i).getCenter();
					for ( int j = i + 1; j < clusters.size(); j++ ) {
						double distance = MathUtils.calculateDistance(center1, clusters.get(j).getCenter());
						if (distance < minDistance) {
							minDistance = distance;
							closestCluster1 = i;
							closestCluster2 = j;
						}
					}
				}
				
				// TODO: check that the closest cluster are not the same?
				MicroCluster merged = clusters.get(closestCluster1);
				merged.merge(clusters.get(closestCluster2));
				clusters.remove(closestCluster2);
				merged.addFeatureVector(featureVector.getPoint(), featureVector.getTimestamp());
				
				placement.put(featureVector, merged.getIdList().get(0));
			}
			
		}
		
		return placement;
	}
	
	public List<MicroCluster> getClusters(){
		return this.clusters;
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
