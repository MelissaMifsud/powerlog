package net.melissam.powerlog.clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KMeansClusterer {

	private Map<FeatureVector, Integer> placements;
	
	// MicroCluster settings
	private double t;
	private double m;
	
	public KMeansClusterer(double t, double m){
		this.t = t;
		this.m = m;
	}
	
	/**
	 * Use KMeansPlusPlus clustering on the initialisation points to create maxClusters clusters.
	 * @param points			The points to cluster.
	 * @param maxClusters		The desired number of clusters.
	 * @return
	 */
	public List<MicroCluster> cluster(List<FeatureVector> points, int maxClusters){
		
		// choose random centroids
		double[][] centroids = new double[maxClusters][points.get(0).getDimension()];
		Set<Integer> chosen = new HashSet<Integer>();
		for (int i = 0; i < maxClusters; i++){
			int r = -1;
			
			do{
				r = (int)(Math.random() * points.size());				
			}while(chosen.contains(r));
			
			chosen.add(r);
			
			FeatureVector fv = points.get(r);			
			for (int j=0; j < fv.getDimension(); j++){
				centroids[i][j] = fv.get(j); 
			}
		}
		
		// convert points to 2-d array
		double[][] _points = new double[points.size()][points.get(0).getDimension()];
		for (int i=0; i<points.size(); i++){
			for (int j=0; j < points.get(0).getDimension(); j++){
				_points[i][j] = points.get(i).get(j);
			}
		}
	
		ModifiedEKmeans kmeansClusterer = new ModifiedEKmeans(centroids, _points);
		kmeansClusterer.setIteration(100);
		kmeansClusterer.run();		
		
		List<MicroCluster> microClusters = new ArrayList<MicroCluster>();
		for (int i = 0; i < kmeansClusterer.getCentroids().length; i++){			
			microClusters.add(new MicroCluster(i+1, centroids[i], 1, t, m)); // start ids from 1
		}

		// add points to clusters
		placements = new HashMap<FeatureVector, Integer>(points.size());
		int[] assignments = kmeansClusterer.getAssignments();
		for (int i = 0; i < assignments.length; i++){			
			microClusters.get(assignments[i]).addFeatureVector(points.get(i));		
			placements.put(points.get(i), microClusters.get(assignments[i]).getIdList().get(0));
		}
		
		return microClusters;
	}
	
	public Map<FeatureVector, Integer> getPlacements(){
		return this.placements;
	}
	
}
