package net.melissam.powerlog.common;

import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.math3.ml.clustering.Clusterable;

/**
 * Represents a feature vector of arbitrary type.
 * 
 * @author melissam
 */
public class FeatureVector extends ArrayList<Double> implements Clusterable{
	
	private long timestamp;
	
	public FeatureVector(){
		super();
		this.timestamp = -1;
	}
	
	public FeatureVector(long timestamp){
		super();
		this.timestamp = timestamp;
	}
	
	public FeatureVector(long timestamp, int size){
		super(size);
		this.timestamp = timestamp;
	}
	
	public long getTimestamp(){
		return this.timestamp;
	}
	
	public void setTimestamp(long timestamp){
		this.timestamp = timestamp;
	}
	
	public double[] getPoint(){
		
		double[] values = new double[this.size()];
		int i = 0;
		for (Double d : this){
			values[i++] = d.doubleValue();
		}
		
		return values;
		
	}
}
