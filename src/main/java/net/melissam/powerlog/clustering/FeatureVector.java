package net.melissam.powerlog.clustering;

import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.math3.ml.clustering.Clusterable;

/**
 * Represents a feature vector of arbitrary type.
 * 
 * @author melissam
 */
public class FeatureVector extends ArrayList<Double> implements Clusterable{
	
	private int  id;			// line number from dataset
	private long timestamp;
	private int  initialCluster;
	
	public FeatureVector(){
		super();
		this.id = -1;
		this.timestamp = -1;
		this.initialCluster = -1;
	}
	
	public FeatureVector(int id){
		super();
		this.id = id;
		this.timestamp = -1;
		this.initialCluster = -1;
	}
	
	public FeatureVector(int id, long timestamp){
		super();
		this.id = id;
		this.timestamp = timestamp;
		this.initialCluster = -1;
	}
	
	public int getId(){
		return this.id;
	}
	
	public void setId(int id){
		this.id = id;
	}
	
	public long getTimestamp(){
		return this.timestamp;
	}
	
	public void setTimestamp(long timestamp){
		this.timestamp = timestamp;
	}
	
	public int getInitialCluster(){
		return initialCluster;
	}
	
	public void setInitialCluster(int cluster){
		this.initialCluster = cluster;
	}
	
	public double[] getPoint(){
		
		double[] values = new double[this.size()];
		int i = 0;
		for (Double d : this){
			values[i++] = d.doubleValue();
		}
		
		return values;
		
	}
	
	public boolean equals(Object o){
		
		boolean equals = false;
		
		if (o != null && o instanceof FeatureVector){
			
			FeatureVector other = (FeatureVector)o;
			equals = this.id == other.getId();
			
		}
		
		return equals;
	}
	
	public int hashCode(){
		
		return new Integer(id).hashCode();
		
	}
}
