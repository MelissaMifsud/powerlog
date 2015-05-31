package net.melissam.powerlog.evaluation;

import java.util.Collection;
import java.util.List;

import net.melissam.powerlog.clustering.Cluster;
import net.melissam.powerlog.clustering.FeatureVector;

/**
 * Sum of square distance measure.
 * 
 * @author melissam
 *
 */
public class SSQEvaluation implements ClusterEvaluationMeasure{

	@Override
	public double evaluate(Collection<Cluster> clusters, List<FeatureVector> points) {
		
		double ssq = 0.0;
		
        for (FeatureVector fv : points) {

        	// find the closest cluster
            double minDistance = Double.MAX_VALUE;
            
            for (Cluster cluster : clusters) {
            	
                double distance = 0.0;
                double[] center = cluster.getCenter();
                
                for (int i = 0; i < center.length; i++) {
                    double d = fv.getPoint()[i] - center[i];
                    distance += d * d;
                }
                
                // keep the minimum distance so far
                minDistance = Math.min(distance, minDistance);
            }
            
            ssq += minDistance;
        }
				
		return ssq / points.size();
	}

}
