/**
 *  
 *  Creation script for global clustream instances.
 *  Author: Melissa
 *  Date:   13th March 2015   
 *  
 */

-- Table that keeps record of all the inputs from local instances and the cluster they are put into
-- Each feature is the center of a cluster at a given timestamp from a local instance
CREATE TABLE Input
(
	timestamp 		INTEGER 		NOT NULL, -- timestamp at which the input was received
	instance		INTEGER			NOT NULL, -- the id of the local instance the input came from
	itimestamp		INTEGER			NOT NULL, -- timestamp of the feature when it was sent from the instance
	icluster		INTEGER			NOT NULL, -- the id of the cluster the feature represents
	feature 		VARCHAR(1024) 	NOT NULL, -- the serialized feature (= center of iCluster)
	cluster			INTEGER			NOT NULL, -- the id of the global cluster the input was put into
	
	CONSTRAINT input_pk PRIMARY KEY (timestamp)
	
);

-- Table that stores the status of the clusters at intervals of the clustering
CREATE TABLE Snapshot
(
	timestamp 		INTEGER 		NOT NULL, -- timestamp at which the status is being recorded
	featureCount	INTEGER			NOT NULL, -- number of features received so far
	clusters		VARCHAR(1024)	NOT NULL, -- serialized representation of cluster ids and id-lists
	
	CONSTRAINT snapshot_pk PRIMARY KEY (timestamp)
);