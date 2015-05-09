package net.melissam.powerlog.clustering;

import java.io.Serializable;

/**
 * Representation of a cluster based on Euclidian distance.
 * 
 * @author melissam
 *
 */
public class Cluster implements Serializable{

	/** Serial UUID */
	private static final long serialVersionUID = 107446923322303897L;
	
	protected double[] center;
	protected double radius;
	protected double weight;
	
	public Cluster(double[] center, double radius, double weight){
		this.center = center;
		this.radius = radius;
		this.weight = weight;
	}
	
	public Cluster(double[] center, double radius){
		this(center, radius, 1.0);
	}
	
	public Cluster(){}
	
	public double[] getCenter() {
		return center;
	}
	
	
	public void setCenter(double[] center) {
		this.center = center;
	}
	
	
	public double getRadius() {
		return radius;
	}
	
	
	public void setRadius(double radius) {
		this.radius = radius;
	}
	
	
	public double getWeight() {
		return weight;
	}
	
	
	public void setWeight(double weight) {
		this.weight = weight;
	}
	
	
	public final static double getDistance(double[] point1, double [] point2) {
		
		double distance = 0.0;
		
		for (int i = 0; i < point1.length; i++) {
			double d = point1[i] - point2[i];
			distance += d * d;
		}
		
		return Math.sqrt(distance);
	}
	
	
	public final double getDistanceFromCenter(double[] point) {
		
		double distance = 0.0;
		
		for (int i = 0; i < point.length; i++) {
			double d = center[i] - point[i];
			distance += d * d;
		}
		
		return Math.sqrt(distance);
	}
	
}
