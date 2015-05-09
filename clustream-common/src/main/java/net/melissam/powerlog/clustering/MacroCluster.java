package net.melissam.powerlog.clustering;

import java.util.ArrayList;
import java.util.List;

public class MacroCluster extends ClusterFeatureVector {

	/** Serial UUID. */
	private static final long serialVersionUID = -1529714241903686162L;

	/** List of micro clusters in this cluster. */
	private List<MicroCluster> microClusters;
		
	public MacroCluster(double[] center){
		super(center);
		this.microClusters = new ArrayList<MicroCluster>();
	}
	
	
	public void addMicroCluster(MicroCluster microCluster){
		
		// use CF additivity feature
		super.add(microCluster);
		
		// keep track of the micro clusters in this macro cluster
		this.microClusters.add(microCluster);
	}
	
	
	public List<MicroCluster> getMicroClusters(){
		return this.microClusters;
	}
	
	
	@Override
	public double getRadius(){

		// calculate the radius as the largest distance of a point to the center
		double radius = 0.0;
		for (MicroCluster point : microClusters) {
			double distance = getDistanceFromCenter(point.getCenter());
			if (distance > radius) {
				radius = distance;
			}
		}
		
		return radius;

	}

}
