package net.melissam.powerlog.clustering;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.math3.analysis.function.Inverse;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.special.Erf;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

/**
 * Representation of a micro-cluster maintained in the online phase.
 * 
 * @author melissam
 *
 */
public class MicroCluster implements Serializable {

	/** Serial UUID */
	private static final long serialVersionUID = 107446923322303897L;

	/** Number of elements in the cluster. */
	private double size;
	
	/** Sum of squares of the values of each feature vector added to the micro-cluster. */
	//vector(CF2x)
	private double[] sumOfSquaresOfValues;
	
	/** Linear sum of values of each feature vector added to the micro-cluster. */
	// vector(CF1x)
	private double[] sumOfValues;
	
	/** Sum of the squares of the timestamps at which each feature vector arrives. */
	// CF2t
	private double sumOfSquaresOfTimestamps;
	
	
	/** Sum of timestamps at which each feature vector arrives. */
	// CF1t
	private double sumOfTimestamps;
	
	/** 
	 * Factor used for predicting whether a new feature vector belongs to the cluster.
	 * Used for calculating the maximum boundary of the micro-cluster -> a factor of t of the the root-square-means deviation of 
	 * all the data points in the cluster.
	 */
	private double t;
	
	/** Number of points to calculate the recency timestamp of this cluster on. */ 
	private double m;
	
	/** List of ids of the cluster. */
	private List<Integer> idList;
	
	
	/**
	 * Construct a Microcluster from a single feature vector. This initial addition is also the center of the cluster.
	 * @param center
	 */
	public MicroCluster(int id, double[] center, long timestamp, double t, double m){
		
		// set id
		this.idList = new ArrayList<Integer>();
		this.idList.add(id);
		
		// nothing to add with
		sumOfValues	= center;
		
		// sum the values of center and populate sumOfSquares
		sumOfSquaresOfValues = new double[center.length];
		for (int i = 0; i<center.length; i++){
			sumOfSquaresOfValues[i] = Math.pow(center[i], 2);
		}
		
		// first timestamp
		this.sumOfTimestamps = timestamp;
		
		// sum of square of timestamps
		this.sumOfSquaresOfTimestamps = Math.pow(timestamp, 2);
		
		this.t = t;
		this.m = m;
		
		this.size++;
	}
	
	public List<Integer> getIdList(){
		return this.idList;
	}
	
	public double getSize() {
		return size;
	}

	public double[] getSumOfSquaresOfValues() {
		return sumOfSquaresOfValues;
	}

	public double[] getSumOfValues() {
		return sumOfValues;
	}

	public double getSumOfSquaresOfTimestamps() {
		return sumOfSquaresOfTimestamps;
	}

	public double getSumOfTimestamps() {
		return sumOfTimestamps;
	}

	/**
	 * Add a feature vector to this micro-cluster.
	 * 
	 * @param featureVector 
	 * @param timestamp
	 */
	public void addFeatureVector(double[] featureVector, long timestamp){
		
		assert(featureVector.length == sumOfValues.length);
		
		// adjust sumOfValues
		for (int i = 0; i < sumOfValues.length; i++){
			sumOfValues[i] += featureVector[i];
		}
		
		// adjust sum of squares
		for (int i = 0; i < sumOfSquaresOfValues.length; i++){
			sumOfSquaresOfValues[i] += Math.pow(featureVector[i], 2);
		}
		
		// adjust timestamp values
		sumOfTimestamps += timestamp;
		sumOfSquaresOfTimestamps += Math.pow(timestamp, 2);
		
		// increment size
		size++;
	}
	
	
	/**
	 * Get the center of the micro-cluster from the sum of all feature vectors the cluster contains.
	 * 
	 * @return A vector of center values.
	 */
	public double[] getCenter(){
		
		assert (size > 0);
		double center[] = new double[sumOfValues.length];
		for (int i = 0; i < sumOfValues.length; i++) {
			center[i] = sumOfValues[i] / size;
		}
		return center;
		
	}
	
	
	
	/**
	 * Calculates the (Euclidean) distance of a point from the centroid of this micro-cluster.	 * 
	 * This is required to decide which cluster a point belongs to.
	 * 
	 * @param point The point to find the distance of.
	 * @return	The distance of the point from the center of this micro-cluster
	 */
	public double getDistance(double[] point){
	
		double[] center = getCenter();
		double distance = 0.0;
		
		for (int i = 0; i < center.length; i++){
			
			//square o
			distance += Math.pow(center[i] - point[i], 2);
			
		}
		
		return Math.sqrt(distance);
		
	}
	
	
	
	/**
	 * Returns the maximum boundary of the cluster. This is defined as a factor of t of the root-means-square deviation of the data points from the center.
	 * 
	 * For clusters with 1 point, then the maximum boundary is the distance to the closest cluster, which needs to be calculated externally to the cluster.
	 * In the case of a 1 point cluster, this method will return 0 so that the online algorithm knows it needs to find the closest cluster instead.
	 * 
	 * @return The maximum boundary of the cluster. This is also referred to as the radius.
	 */
	public double getMaximumBoundary(){
	
		if(size == 1) return 0; 			// paper says "If the cluster only has one point the maximum boundary is the distance to the closest cluster
											// this needs to be calculated external to the micro-cluster, so return 0 will be indicative of this
		else return getDeviation() * t;
		
	}
	
	
	/**
	 * Merge a cluster into this cluster.
	 * 
	 * @param other the cluster to merge.
	 */
	public void merge(MicroCluster other){
	
		// use CF additivity property
		this.size += other.getSize();
		this.sumOfTimestamps += other.sumOfTimestamps;
		this.sumOfSquaresOfTimestamps += other.sumOfSquaresOfTimestamps;
	
		for ( int i = 0; i < this.sumOfValues.length; i++ ) {
		    this.sumOfValues[i] += other.sumOfValues[i];
		    this.sumOfSquaresOfValues[i] += other.sumOfSquaresOfValues[i];
		}
		
		this.idList.addAll(other.getIdList());
	}
	
	
	/**
	 * Referred to as relevance stamp in paper.
	 * 
	 * Approximates the recency of the data points in this cluster.
	 * If the size of the cluster is less than 2*m, then we just use the mean timestamp.
	 * Otherwise returns the m/(2*size)-th percentile timestamp.
	 * 
	 * @return An approximation of the average timestamp.
	 */
	public double getAverageTimestamp(){
	
		if (size < m*2){					
			return this.getTimestampMean();
		}else{
			
			// calculate percentile
			return getTimestampMean() + getTimestampStandardDeviation() * getQuantile(m/(2 * size));
			
		}
	}
	
	
	// ------------------ Private methods. ---------------------- /
	
	// Calculation of root-square-means deviation
	
	// Calculate the variation of the points from the center
	private double[] getVariance(){
		
		double[] variance = new double[this.sumOfValues.length];
	
		for (int i = 0; i < this.sumOfValues.length; i++) {
				 
			// this is the value of the center CF for point i
			double avgSum = this.sumOfValues[i] / this.size;
		  	
			// this is the sum of squares center CF for point i
		    double avgSumOfSquares = this.sumOfSquaresOfValues[i] / size;
            
		    // calculate the variance 
		    // TODO: confirm that taking the absolute value is the correct way to deal with negatives here?
		    // shall we round to 0 instead?
		    variance[i] = Math.abs(avgSumOfSquares - Math.pow(avgSum, 2));
		    
		}
		 
		return variance;
		
	}
	
	
	// The root-means-square (RMS) deviation
	private double getDeviation(){
		
		// get the variance of all points from the centroid
		double[] variance = getVariance();
		double sumOfDeviation = 0.0;
		
		// calculate the sum of the square roots of the variance of each data point
		for (int i = 0; i < variance.length; i++) {
		    sumOfDeviation += Math.sqrt(variance[i]);
		}
		
		// take the mean value
		return sumOfDeviation / variance.length;
	}
	

	// Timestamp operations for calculating recency
	
	/**
	 * Returns the mean of all the timestamps of values in the cluster.
	 * @return The mean of all timestamps.
	 */
	private double getTimestampMean(){
		return sumOfTimestamps / size;
	}
	
	
	/**
	 * Returns the standard deviation of the timestamps of values in the cluster.
	 * @return
	 */
	private double getTimestampStandardDeviation(){
		return Math.sqrt(sumOfSquaresOfTimestamps / size - Math.pow(sumOfTimestamps, 2)/size);
	}
	
	
	private double getQuantile(double q){	
		return Math.sqrt(2) * Erf.erfInv(q);		
	}
	
}
