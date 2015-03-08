package net.melissam.powerlog.online;

import java.util.List;

import net.melissam.powerlog.common.FeatureVector;

/**
 * Represents a micro-cluster of feature vectors of dimension d. 
 * 
 * TODO: Should this be a stream listener?
 * 
 * @author melissam
 *
 */
public class Microcluster{

	/** Sum of squares of the data in the feature vectors in the microcluster. */
	private List<Integer> dataSumOfSquares;
	
	/** The sum of the data in the feature vectors in the microcluster. */
	private List<Integer> dataSumOfValues;
	
	/** The sum of squares of the timestamp of each feature vector in the microcluster. */
	private int timestampSumOfSquares;
	
	/** The sum of the timestamp of each feacture vector in the microcluster. */
	private int timestampSumOfValues;
	
	/** Number of feature vectors in the micro-cluster. */
	private int size;
	
	
	/**
	 * Adds a feature vector to the micro-cluster.
	 * @param featureVector
	 */
	public void addFeatureVector(FeatureVector<E> featureVector){
		
		// update  micro-cluster statistics
		
	}
	
	
	/** 
	 * Save the microcluster - referred to as a snapshot.
	 */
	public void save(){
		
		// TODO where are we saving this?
		
	}
	
	
	
}
