package net.melissam.powerlog.messaging;

import java.io.Serializable;
import java.util.List;

import net.melissam.powerlog.clustering.*;

public class MicroClusterMessage implements Serializable {
		
	/** Serial UUID. */
	private static final long serialVersionUID = 4504913003816308807L;

	// Id of sender
	private int instanceId;
	
	// The timestamp at the source when the message was sent
	private int timestamp;
	
	// the list of MicroClusters
	private List<MicroCluster> microClusters;
	
	
	public MicroClusterMessage(int instanceId, int timestamp){
		this.instanceId = instanceId;
		this.timestamp = timestamp;
	}


	public int getInstanceId() {
		return instanceId;
	}


	public int getTimestamp() {
		return timestamp;
	}


	public List<MicroCluster> getMicroClusters() {
		return microClusters;
	}


	public void setMicroClusters(List<MicroCluster> microClusters) {
		this.microClusters = microClusters;
	}
	
	
}
