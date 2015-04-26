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
	id 				INTEGER 		NOT NULL, -- the feature id (corresponds to the line number in the dataset)
	feature 		VARCHAR(1024) 	NOT NULL, -- the serialized feature
	cluster			INTEGER 		NOT NULL, -- the id of the cluster the feature was initially placed into
	
	CONSTRAINT feature_pk PRIMARY KEY (id)
	
);

-- Table that stores the status of the clusters at intervals of the clustering
CREATE TABLE Snapshot
(
	timestamp 		BIGINT 			NOT NULL, -- timestamp at which the status is being recorded
	featureCount	INTEGER			NOT NULL, -- number of features received so far
	lastFeature		INTEGER			NOT NULL, -- id of the last feature that was clustered
	clusters		CLOB			NOT NULL,  -- serialized representation of cluster ids and id-lists
	
	CONSTRAINT snapshot_pk PRIMARY KEY (timestamp)
);