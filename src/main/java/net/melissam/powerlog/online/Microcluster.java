package net.melissam.powerlog.online;

import java.util.Arrays;
import java.util.Date;

/**
 * Representation of a micro-cluster maintained in the online phase.
 * 
 * @author melissam
 *
 */
public class MicroCluster {

	/** Number of elements in the cluster. */
	private int size;
	
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
	
	/** Cluster number. */
	// The algorithms defines 'm' as the maximum number of microclusters the algorithm should create
	// Here 'm' represents the sequence number of the particular micro-cluster amoung all micro-clusters.
	private int m;
	
	
	/**
	 * Construct a Microcluster from a single feature vector. This initial addition is also the center of the cluster.
	 * @param center
	 */
	public MicroCluster(double[] center, long timestamp, double t, int m){
		
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
	 * Calculates the maximum boundary factor which is used to decide whether a new feature vector belongs to this micro-cluster.
	 * The maximum boundary factor is calculated as the RSM of the all the data points in this cluster.
	 * 
	 * @return The maximum boundary factor of this cluster.
	 */
	public double getMaximumBoundaryFactor(){
		
		
		
	}
	
	
	// ------------------ Private methods. ---------------------- /
	
	// Calculation of root-square-means deviation
	
	private double[] getVariance(){
		
		double[] variance = new double[this.sumOfValues.length];
	
		for (int i = 0; i < this.sumOfValues.length; i++) {
			
			double sum = this.sumOfValues[i];
			double sumOfSquares = this.sumOfSquaresOfValues[i];
		 
			double avgSum = sum / this.size;
		    double avgSumSquared = avgSum * avgSum;
		    
		    double avgSumOfSquares = sumOfSquares / size;
            variance[i] = avgSumOfSquares - avgSumSquared;
		}
		 
		return variance;
		
	}
	
	
	private double getNormalizedDeviation(double[] featureVector){
		
		double[] variance = getVariance();
		double[] center = getCenter();
		
		double nd = 0.0;
		
		for (int i = 0; i < center.length; i++) {
			double diff = center[i] - featureVector[i];
			nd += (diff * diff);// variance[i];
		}
		
		return Math.sqrt(nd);
		
	}
	
}
