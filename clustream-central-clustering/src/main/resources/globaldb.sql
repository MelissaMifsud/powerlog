/**
 *  
 *  Creation script for global clustream instances.
 *  Author: Melissa
 *  Date:   13th March 2015   
 *  
 */

-- Table that keeps record of all the features received from local instances
-- Each feature is the center of a cluster at a given timestamp from a local instance
CREATE TABLE Feature
(
	id					INTEGER			NOT NULL, 	-- id given to the feature
	gtime	 			INTEGER 		NOT NULL, 	-- time at which the input was received
	instance			INTEGER			NOT NULL, 	-- the id of the local instance the input came from
	icluster			INTEGER			NOT NULL, 	-- the id of the cluster the feature represents on the remote instance
	idList				VARCHAR(1024), 			  	-- the id list of the cluster that was sent from a remote instance
	sumOfValues			VARCHAR(1024)	NOT NULL,	-- serialised array of sum of attributes
	sumSquareOfValues	VARCHAR(1024)	NOT NULL,	-- serialised array of sum of squares of values
	size				INTEGER			NOT NULL,	-- number of features in the cluster
	
	CONSTRAINT feature_pk PRIMARY KEY (id)
	
);

CREATE TABLE Placement
(
	featureId		INTEGER			NOT NULL,	-- id of feature
	clusterId		INTEGER			NOT NULL,	-- id of cluster the feature was placed into
	
	CONSTRAINT placement_pk PRIMARY KEY (featureId, clusterId)
);

-- Table that stores the status of the clusters at intervals of the clustering
CREATE TABLE Snapshot
(
	time 				INTEGER 		NOT NULL, 	-- time at which the status is being recorded
	clusterId			INTEGER			NOT NULL, 	-- id of cluster
	idList				VARCHAR(1024),				-- list of clusters that were merged with this cluster	
	sumOfValues			VARCHAR(1024)	NOT NULL,	-- serialised array of sum of attributes
	sumSquareOfValues	VARCHAR(1024)	NOT NULL,	-- serialised array of sum of squares of values
	size				INTEGER			NOT NULL,	-- number of features in the cluster
	
	CONSTRAINT snapshot_pk PRIMARY KEY (time, clusterId)
);