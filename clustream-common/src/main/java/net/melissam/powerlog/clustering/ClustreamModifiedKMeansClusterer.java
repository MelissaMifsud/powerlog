package net.melissam.powerlog.clustering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;


/**
 * Implements modified KMeans clustering for creating macro-clusters as specified in Aggarwal section 4.
 * 
 * @author mmifsud
 *
 */
public class ClustreamModifiedKMeansClusterer {
	
	private static final Gson gson = new Gson();
	
	private static final Logger LOG = LogManager.getLogger(ClustreamModifiedKMeansClusterer.class);
		
	
	public Map<Cluster, List<MicroCluster>> doMacroClusterCreation(List<MicroCluster> _microClusters, int k){
		
		Cluster[] kmeansClusters = kMeans(_microClusters, k);
		
		// now cluster the micro clusters around the kmeans centers
		Cluster[] macroClusters = new MicroCluster[kmeansClusters.length];
		List<List<MicroCluster>> macroMicroClusters = new ArrayList<List<MicroCluster>>(macroClusters.length);
		for (int i = 0; i < macroClusters.length; i++){
			macroMicroClusters.add(new ArrayList<MicroCluster>());
		}
		
		// put micro clusters in the closest macro cluster
		for (MicroCluster point : _microClusters){
			
			// find closest center from kmeans
		    double minDistance = Double.MAX_VALUE;
		    int closestCluster = 0;
		    for (int i = 0; i < kmeansClusters.length; i++) {
		    	double distance = Cluster.getDistance(kmeansClusters[i].getCenter(), point.getCenter());
				if (distance < minDistance) {
				    closestCluster = i;
				    minDistance = distance;
				}
		    }

		    // Add to cluster
		    if ( macroClusters[closestCluster] == null ) {
		    	macroClusters[closestCluster] = point;
		    } 
		    // else{
		    //macroClusters[closestCluster].merge(point);
		    macroMicroClusters.get(closestCluster).add(point);
		    //}
		}
		
		Map<Cluster, List<MicroCluster>> result = new HashMap<Cluster, List<MicroCluster>>();
		for (int i = 0; i < macroClusters.length; i++){
			result.put(macroClusters[i], macroMicroClusters.get(i));
		}
		
		return result;
	}

	
	public Cluster[] kMeans(List<MicroCluster> microClusters, int k){
		
		// initialise the seeds
		Cluster[] macroClusters = new Cluster[k];
		
		// choose the seeds as the centers of the micro clusters with the most points
		TreeMap<Integer, List<MicroCluster>> microClusterSizes = new TreeMap<Integer, List<MicroCluster>>();
		
		for(MicroCluster microCluster : microClusters){
			
			List<MicroCluster> sameSizeList = microClusterSizes.get((int)microCluster.getSize());
			if (sameSizeList == null) sameSizeList = new ArrayList<MicroCluster>();
			sameSizeList.add(microCluster);
			microClusterSizes.put((int)microCluster.getSize(), sameSizeList);
			
		}
		
		// get the top 5 as the seeds
		int macroClustersAdded = 0;
		for (Integer size : microClusterSizes.descendingKeySet()){
			
			List<MicroCluster> clusters = microClusterSizes.get(size);
			
			for (MicroCluster cluster : clusters){
				
				LOG.info("Choosing micro cluster of size={} as initial seed.", cluster.getSize());
				
				macroClusters[macroClustersAdded] = new Cluster(cluster.getCenter(), 0);
				++macroClustersAdded;
				
				if (!(macroClustersAdded < macroClusters.length)){
					break; // found what we needed
				}
			}
			
			if (!(macroClustersAdded < macroClusters.length)){
				LOG.info("Initial seeds found.");
				break; // found what we needed
			}
		}
				
		return doKmeans(macroClusters, microClusters);
	}
	
	private Cluster[] doKmeans(Cluster[] seeds, List<MicroCluster> points){
		
		Cluster[] newSeeds = null;
		
		int iterations = 0;
		
		int dimension = seeds[0].getCenter().length;
		
		// keeps track of each point in each partition
		// this is needed to calculate the weighted centroid
		ArrayList<ArrayList<MicroCluster>> partitions = new ArrayList<ArrayList<MicroCluster>>();
		for (int i = 0; i < seeds.length; i++) {
			partitions.add(new ArrayList<MicroCluster>());
		}

		// assign points to partitions until all partitions have converged
		while (true) {
			
			// print info
			for (int s = 0; s < seeds.length; s++){
				LOG.info("seed[{}] = {}", s, gson.toJson(seeds[s].getCenter()));
			}
			
			
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
				partitions.get(closestCluster).add(point);
				
			}

			
			// Adjust seeds
			// From paper: New seed for a partition is the weighted centroid of the micro-clusters in the partition
			newSeeds = new Cluster[seeds.length];
			for (int i = 0; i < seeds.length; i++) {
				
				// assign a new seed with weighted centroid of the partition
				newSeeds[i] = getWeightedCentroid(partitions.get(i), dimension);
				LOG.info("newSeed[{}] = {}", i, gson.toJson(newSeeds[i].getCenter()));
				
				// clear list for next iteration
				partitions.get(i).clear();
			}
			
			
			// Let's check if the clusters have converged
			// Clusters have converged if the new seeds are equal to the old seeds i.e. seeds have not changed
			boolean converged = true;
			for (int i = 0; i < seeds.length; i++) {
				
				boolean foundMatch = false;
				
				for (int j = 0; j < newSeeds.length; j++){
					
					if (Arrays.equals(seeds[i].getCenter(), newSeeds[j].getCenter())) {
						LOG.info("seeds[{}] = newSeeds[{}]", i, j);
						foundMatch = true;
						break;
					}
					
				}
				
				if (!foundMatch){
					converged = false;
					break;
				}else{
					converged = true; // so far  we think clusters have converged
				}
				
			}
			
			// if partitions have converged, we can stop clustering
			if (converged) {				
				break;
			}
			// otherwise re-assign seeds to the new ones
			else {
				seeds = newSeeds;
				iterations++;
				LOG.info("iteration={}, clusters not converged yet.", iterations);
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
	private Cluster getWeightedCentroid(List<MicroCluster> partition, int dimension) {
		
		Cluster seed = null;
		
		double[] center = new double[dimension];
		for (int i = 0; i < center.length; i++) {
			center[i] = 0.0;
		}

		// if the partition is empty, then the seed is a 0 value array with radius 0.
		if (partition.size() == 0) {
			
			seed = new Cluster(center, 0);
			
		}else{

			// if partition has items, calculate a vector as the sum of the points
			for (MicroCluster point : partition) {
				
				for (int i = 0; i < center.length; i++) {
					center[i] += point.getCenter()[i];
				}
				
			}

			// normalize the resulting vector by the number of points in the partition
			for (int i = 0; i < center.length; i++) {
				center[i] /= partition.size();
			}
			
			seed = new Cluster(center, getRadius(partition, center));

		}
		
		return seed;
	}
	
	
	public double getRadius(List<MicroCluster> points, double[] center){

		// calculate the radius as the largest distance of a point to the center
		double radius = 0.0;
		for (MicroCluster point : points) {
			double distance = Cluster.getDistance(center, point.getCenter());
			if (distance > radius) {
				radius = distance;
			}
		}
		
		return radius;

	}
	
	
	
	
}
