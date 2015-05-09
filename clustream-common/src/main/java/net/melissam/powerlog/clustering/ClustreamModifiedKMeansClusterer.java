package net.melissam.powerlog.clustering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Implements modified KMeans clustering for creating macro-clusters as specified in Aggarwal section 4.
 * 
 * @author mmifsud
 *
 */
public class ClustreamModifiedKMeansClusterer {
	
	private static final Logger LOG = LogManager.getFormatterLogger(ClustreamModifiedKMeansClusterer.class);

	public MacroCluster[] makeMacroClusters(List<MicroCluster> microClusters, int k){
		
		// initialise the seeds
		MacroCluster[] macroClusters = new MacroCluster[k];
		
		// choose the seeds as the centers of the micro clusters with the most points
		TreeMap<Integer, List<MicroCluster>> microClusterSizes = new TreeMap<Integer, List<MicroCluster>>();
		
		for(MicroCluster microCluster : microClusters){
			
			List<MicroCluster> sameSizeList = microClusterSizes.get(microCluster.getSize());
			if (sameSizeList == null) sameSizeList = new ArrayList<MicroCluster>();
			sameSizeList.add(microCluster);
			microClusterSizes.put((int)microCluster.getSize(), sameSizeList);
			
		}
		
		// get the top 5 as the seeds
		int macroClustersAdded = 0;
		for (Entry<Integer, List<MicroCluster>> entry : microClusterSizes.entrySet()){
			
			for (MicroCluster cluster : entry.getValue()){
				macroClusters[macroClustersAdded] = new MacroCluster(cluster.getCenter());
				++macroClustersAdded;
				
				if (!(macroClustersAdded < macroClusters.length)){
					break; // found what we needed
				}
			}
			
			if (!(macroClustersAdded < macroClusters.length)){
				break; // found what we needed
			}
		}
		
		
		return doKmeans(macroClusters, microClusters);
	}
	
	private MacroCluster[] doKmeans(MacroCluster[] seeds, List<MicroCluster> points){
		
		int iterations = 0;
		
		int dimension = seeds[0].getCenter().length;

		// assign points to partitions until all partitions have converged
		while (true) {
			
			// Assign points to clusters by finding the closest seed for each point
			for (MicroCluster point : points) {
				
				// initialise the minimum distance as the distance to the first seed
				double minDistance = Cluster.getDistance(point.getCenter(), seeds[0].getCenter());				
				int closestCluster = 0;
				
				// check all the other seeds
				for (int i = 1; i < seeds.length; i++) {
					
					double distance = Cluster.getDistance(point.getCenter(), seeds[i].getCenter());
					
					// if the point is closer to this seed, change the closestCluster
					if (distance < minDistance) {
						closestCluster = i;
						minDistance = distance;
					}
				}

				// add point to the closest partition list
				seeds[closestCluster].addMicroCluster(point);
				
			}

			
			// Adjust seeds
			// From paper: New seed for a partition is the weighted centroid of the micro-clusters in the partition
			MacroCluster[] newSeeds = new MacroCluster[seeds.length];
			for (int i = 0; i < seeds.length; i++) {
				// assign a new seed
				newSeeds[i] = getWeightedCentroid(seeds[i], dimension);
			}
			
			
			// Let's check if the clusters have converged
			// Clusters have converged if the new seeds are equal to the old seeds i.e. seeds have not changed
			boolean converged = true;
			for (int i = 0; i < seeds.length; i++) {
				if (!Arrays.equals(seeds[i].getCenter(), newSeeds[i].getCenter())) {
					converged = false;
					break;
				}
			}
			
			// if partitions have converged, we can stop clustering
			if (converged)	break;
			// otherwise re-assign seeds to the new ones
			else {
				seeds = newSeeds;
				iterations++;
			}
			
		}
		
		LOG.info("MacroCluster k-means converged after {} iterations.", iterations);

		return seeds;
		
	}
	
	
	/**
	 * Calculates the weighted center of a k-means partition.
	 * 
	 * @param partition	
	 * @param dimension
	 * @return
	 */
	private MacroCluster getWeightedCentroid(MacroCluster partition, int dimension) {
		
		MacroCluster seed = null;
		
		double[] center = new double[dimension];
		for (int i = 0; i < center.length; i++) {
			center[i] = 0.0;
		}

		// if the partition is empty, then the seed is a 0 value array with radius 0.
		if (partition.getWeight() == 0) {
			
			seed = new MacroCluster(center);
			
		}else{

			// if partition has items, calculate a vector as the sum of the points
			for (MicroCluster point : partition.getMicroClusters()) {
				
				for (int i = 0; i < center.length; i++) {
					center[i] += point.getCenter()[i];
				}
				
			}

			// normalize the resulting vector by the number of points in the partition
			for (int i = 0; i < center.length; i++) {
				center[i] /= partition.getWeight();
			}
			
			seed = new MacroCluster(center);

		}
		
		return seed;
	}
	
}
