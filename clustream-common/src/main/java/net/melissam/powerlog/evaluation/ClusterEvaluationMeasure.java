package net.melissam.powerlog.evaluation;

import java.util.Collection;
import java.util.List;

import net.melissam.powerlog.clustering.Cluster;
import net.melissam.powerlog.clustering.FeatureVector;

/**
 * Interface for all cluster evaluation measures.
 * 
 * @author melissam
 *
 */
public interface ClusterEvaluationMeasure {

	/**
	 * Give an evaluation of the clusters.
	 * 
	 * @param clusters	The clusters to evaluate.
	 * @return
	 */
	double evaluate(Collection<Cluster> clusters, List<FeatureVector> points);
	
}
