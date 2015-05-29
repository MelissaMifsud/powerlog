/**
 *  
 *  Creation script for local clustream instances.
 *  Author: Melissa
 *  Date:   13th March 2015   
 *  
 */

-- Table that keeps record of all the features and the cluster they are initially added to
CREATE TABLE Feature
(
	id 					INTEGER 		NOT NULL, -- the feature id (corresponds to the line number in the dataset)
	time				INTEGER			NOT NULL, -- time at which the feature was received
	feature 			VARCHAR(1024) 	NOT NULL, -- the serialized feature
	label				VARCHAR(20),			  -- the ground truth classification of the feature, if available		
	clusterId			INTEGER 		NOT NULL, -- the id of the cluster the feature was initially placed into
	
	CONSTRAINT feature_pk PRIMARY KEY (id)
	
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