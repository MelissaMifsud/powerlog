package net.melissam.powerlog.local;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jms.JMSException;

import net.melissam.powerlog.clustering.CluStream;
import net.melissam.powerlog.clustering.FeatureVector;
import net.melissam.powerlog.clustering.MicroCluster;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

/**
 * Main class for running Local clustering.
 * 
 * @author melissam
 *
 */
public class LocalClusterer {
	
	// id of local clusterer
	private int instanceId;	
	
	// number of stream items per time unit
	private int streamSpeed;
	private int timestamp;
	
	// snapshot frequencies
	private int snapshotAlpha;
	private int snapshotL;
	
	// where to read training set from
	private FileInputStream dataset;
	
	// factor of feature number to use for training from the dataset
	private int featureSelectionFactor;
	
	// number of features that have been read
	private int features;
	
	// the clustering learner
	private CluStream learner;
	
	// object that sends microclusters to Global clustering
	private MicroClusterMessageSender sender;
	
	// Json writer
	private Gson jsonWriter;
	
	// Class logger
	private static final Logger LOG = LogManager.getLogger(LocalClusterer.class);
	
	// db creation script
	// private static final String DB_CREATE_SCRIPT = "localdb.sql";
	
	public LocalClusterer() throws ConfigurationException, FileNotFoundException, JMSException{
		
		
		// read properties
		PropertiesConfiguration config = new PropertiesConfiguration("local.properties");
		
		this.instanceId = config.getInteger("instanceid", 1);
		
		this.streamSpeed = config.getInteger("streamSpeed", 2000);
		this.timestamp = 0;
		
		this.snapshotAlpha = config.getInteger("snapshot.a", 2);
		this.snapshotL = config.getInteger("snapshot.l", 10);
		
		this.dataset = new FileInputStream(config.getString("dataset"));		
		this.featureSelectionFactor = config.getInteger("datasetFeatureSelection", 1);
		
		this.learner = new CluStream(config.getInteger("maxClusters", 100), config.getInteger("maximumBoundaryFactor", 2), config.getInteger("relevanceThreshold", 1000), config.getInteger("initNumber", 1000));
		this.features = 0;
		
		this.sender = new MicroClusterMessageSender(this.instanceId, config.getString("mq.broker.host"), config.getInt("mq.broker.port"), config.getString("mq.queue"));
		
		this.jsonWriter = new Gson();
		
		LOG.info("{localClusterer={}, streamSpeed={}, dataset={}, featureSelectionFactor={}, initNumber={}}", this.instanceId, this.streamSpeed, config.getString("dataset"), featureSelectionFactor, config.getInteger("initNumber", 1000));
	}
	
	public void train(){
		
		int linesRead = 0;
		int featuresUsed = 0;
		
		// read feature from dataset file
		BufferedReader reader = new BufferedReader(new InputStreamReader(dataset));
		String line = null;
		
		try{
			
			FeatureVector fv = null;
			Map<FeatureVector, Integer> placement = null;
			
			// initialise the time
			timestamp = 1;
			
			// go through all available features
			while((line = reader.readLine()) != null){

				// should this cluster read this line?
				if (++linesRead % featureSelectionFactor == 0){

					// select the features we are going to use from a single item
					fv = getFeatureVector(line);

					if (fv != null){
						
						fv.setId(linesRead);
						fv.setTimestamp(timestamp);
						
						// give it to the learner
						placement = learner.cluster(fv);
						++featuresUsed;

						// record the cluster id the feature was added to
						if (placement != null && !placement.isEmpty()){
							
							for (Entry<FeatureVector, Integer> entry : placement.entrySet()){
								fv = entry.getKey();
								fv.setInitialCluster(entry.getValue());
								LOG.info("fv=" + jsonWriter.toJson(fv));
							}

							// decide whether it is time to take a snapshot of the clusters
							if (featuresUsed % 2000 == 0){
							
								List<MicroCluster> clusters = learner.getClusters();
								LOG.info("clusters=" + jsonWriter.toJson(clusters));
								
								// send to Global
								try{
									sender.send(clusters, timestamp);
								}catch(JMSException ex){
									LOG.error("Error sending clusters to Global.", ex);
								}
							}
							
						}else{
							LOG.info("featureVector={} used for initialisation.", fv.getId());
						}
						
						// adjust timestamp according to stream speed, if needed
						if (featuresUsed % streamSpeed == 0){
							++timestamp;	
						}

					}else{						
						LOG.info("Skipping line={} because no feature vector could be created from the attributes.", linesRead);
					}


				}else{
					LOG.info("Skipping line={} because of featureSelectionFactor={}", linesRead, featureSelectionFactor);
				}


			}
			
			if (learner.getClusters() != null){
				LOG.info("clusters=" + jsonWriter.toJson(learner.getClusters()));
			}
			
		}catch(IOException ex){
			
			LOG.error("Exception during training at line={} with featureSelectionFactor={}. Training will be aborted.", linesRead, featureSelectionFactor, ex);
			
		}
		
		LOG.info("totalFeaturesRead={}, totalFeaturesUsed={}", linesRead, featuresUsed);
	}
	
	
	public static void main(String... args) throws Exception{
		
		// set up databaset
		// LOG.info("Setting up the database.");
		// DbUtils dbConfigurer = new DbUtils("/localdb.sql");
		// dbConfigurer.build();
		// LOG.info("Database ready.");
		
		// create the clusterer
		LocalClusterer clusterer = new LocalClusterer();
		clusterer.train();
				
		
	}
	
	//--------------------------------- Private Methods. --------------------------------------------//
	
	/**
	 * Reads a single data line of attributes and builds a feature vector. Similarly to the CluStream experiments, we take the 34 continuous attributes out of 
	 * a total of 42 attributes.
	 * 
	 * @param attributes The attributes as read as a single line from the dataset file.
	 * @return	A FeatureVector with the relevant attributes or null if the line is incorrect.
	 */
	private FeatureVector getFeatureVector(String attributes){
		
		FeatureVector fv = null;
		
		String[] _attributes = attributes.split(",");
		if (_attributes.length == 42){
			
			// choose the items we need
			fv = new FeatureVector();
			fv.add(Double.parseDouble(_attributes[0]));
			fv.add(Double.parseDouble(_attributes[4]));
			fv.add(Double.parseDouble(_attributes[5]));
			fv.add(Double.parseDouble(_attributes[7]));
			fv.add(Double.parseDouble(_attributes[8]));
			fv.add(Double.parseDouble(_attributes[9]));
			fv.add(Double.parseDouble(_attributes[10]));
			fv.add(Double.parseDouble(_attributes[12]));
			fv.add(Double.parseDouble(_attributes[13]));
			fv.add(Double.parseDouble(_attributes[14]));
			fv.add(Double.parseDouble(_attributes[15]));
			fv.add(Double.parseDouble(_attributes[16]));
			fv.add(Double.parseDouble(_attributes[17]));
			fv.add(Double.parseDouble(_attributes[18]));
			fv.add(Double.parseDouble(_attributes[19]));
			
			for (int i = 22; i < 41; i++){
				fv.add(Double.parseDouble(_attributes[i]));
			}
			
			// LOG.info("Created feature vector with {} attributes.", fv.size());
			
		}else{
			LOG.warn("Incorrect number of attributes. expected=42, found={}", _attributes.length);
		}
		
		return fv;
		
	}
	
}
