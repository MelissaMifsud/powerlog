package net.melissam.powerlog.common;

import java.util.ArrayList;
import java.util.Date;

/**
 * Represents a feature vector of arbitrary type.
 * 
 * @author melissam
 */
public class FeatureVector extends ArrayList<Integer>{
	
	private Date timestamp;
	
	public FeatureVector(Date timestamp){
		super();
		this.timestamp = timestamp;
	}
	
	public FeatureVector(Date timestamp, int size){
		super(size);
		this.timestamp = timestamp;
	}
	
	public Date getTimestamp(){
		return this.timestamp;
	}
	
	public void setTimestamp(Date timestamp){
		this.timestamp = timestamp;
	}
	
}
