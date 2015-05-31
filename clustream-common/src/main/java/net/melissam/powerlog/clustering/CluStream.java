package net.melissam.powerlog.clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import net.melissam.powerlog.utils.MathUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

/**
 * Online phase of the CluStream algorithm.
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
		
	
	/**
	 * Initialises the CluStream process.
	 * 
	 * @param maxClusters			Maximum number of clusters to have at any time.
	 * @param t						The factor to use to calculate the maximum radius boundary. 
	 * @param relevanceThreshold	Time units after which a cluster can be deleted if it has not changed.
	 * @param initNumber			The number of features to use to create the initial micro-clusters.
	 */
	public CluStream(int maxClusters, double t, int relevanceThreshold, int initNumber){
		
		this.maxClusters = maxClusters;
		this.t = t;
		this.m = maxClusters;
		
		this.clusters = new ArrayList<MicroCluster>();
		
		this.timestamp = 0;
		this.relevanceThreshold = relevanceThreshold;
		
		// initialisation properties
		this.initNumber = initNumber;
		this.initialisationPoints = new ArrayList<FeatureVector>(initNumber); 
		
		this.numFeatures = 0;
		this.clusterSequence = 0;

	}
	
	
	/**
	 * Cluster the given feature and return the cluster assignment.
	 * 
	 * @param featureVector The feature vector to cluster.
	 * @return	The cluster assignment for the feature.
	 */
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
		
			// otherwise let's use a kmeans algorithm on the initial clusters
			CluStreamKMeansClusterer kmeans = new CluStreamKMeansClusterer(t, m);
			clusters.addAll(kmeans.cluster(initialisationPoints, maxClusters));		
			clusterSequence = clusters.get(clusters.size()-1).getIdList().get(0);			
			placement.putAll(kmeans.getPlacements());
			
			initialised = true;
			
			LOG.info("{} clusters initialised.", this.clusters.size());
			LOG.info("initialClusters={}", new Gson().toJson(this.clusters));
		
		}				
		
		// keep track of the distance of the feature vector to each cluster
		TreeMap<Double, MicroCluster> distances = new TreeMap<Double, MicroCluster>();
		// compute the distances
		for(MicroCluster cluster : clusters){			
			distances.put(cluster.getDistance(featureVector.getPoint()), cluster);
		}
		
		// take the closest micro-cluster
		Entry<Double, MicroCluster> closest = distances.firstEntry();
		
		double radius = 0.0;
		if (closest.getValue().getSize() == 0){
			
			// take the radius to be the distance to the nearest neighbour
			
			radius = Double.MAX_VALUE;
			double[] center = closest.getValue().getCenter();
			for ( MicroCluster cluster : clusters ) {
				
				if ( cluster == closest ) {
					continue;
				}

				double distance = cluster.getDistance(center);
				radius = Math.min( distance, radius );
			}
			
		}else{
			radius = closest.getValue().getRadius();
		}
		
		// if the point's distance is within the maximum boundary of the cluster, then we can add the point
		if (closest.getKey() < radius){
			closest.getValue().addFeatureVector(featureVector);
			placement.put(featureVector, closest.getValue().getIdList().get(0));
		}else{
			
			// decide to delete old cluster or merge 2 clusters
			
			// old clusters are those before the relevanceThreshold
			long threshold = featureVector.getTimestamp() - relevanceThreshold;
			
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
				clusters.add(new MicroCluster(++clusterSequence, featureVector.getPoint(), featureVector.getTimestamp(), t, m));
				
				placement.put(featureVector, clusterSequence);
			}
			
		}
		
		return placement;
	}
	
	/**
	 * Return the current set of micro-clusters.
	 * @return The current set of micro-clusters.
	 */
	public List<MicroCluster> getClusters(){
		return this.clusters;
	}
		

}
