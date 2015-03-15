package net.melissam.powerlog.common;

import java.util.ArrayList;
import java.util.Date;

/**
 * Represents a feature vector of arbitrary type.
 * 
 * @author melissam
 */
public class FeatureVector extends ArrayList<Double>{
	
	private long timestamp;
	
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
	
	public double[] toDoubleArray(){
		
		double[] values = new double[this.size()];
		int i = 0;
		for (Double d : this){
			values[i++] = d.doubleValue();
		}
		
		return values;
		
	}
}
