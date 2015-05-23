package net.melissam.powerlog.local;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jms.JMSException;

import net.melissam.powerlog.normalisation.DataNormaliser;
import net.melissam.powerlog.normalisation.MeanRangeDataNormalizer;
import net.melissam.powerlog.normalisation.StatisticalDataNormaliser;
import net.melissam.powerlog.clustering.CluStream;
import net.melissam.powerlog.clustering.FeatureVector;
import net.melissam.powerlog.clustering.MicroCluster;
import net.melissam.powerlog.datasource.FeatureSelector;
import net.melissam.powerlog.datasource.KDD99FeatureSelector;

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
	private String dataset;
	
	// factor of feature number to use for training from the dataset
	private int featureSelectionFactor;
	
	// number of features that have been read
	private int features;
	
	// the clustering learner
	private CluStream learner;
	
	// object that sends microclusters to Global clustering
	private MicroClusterMessageSender sender;
	
	// object responsible for normalising features before they are used for training
	private DataNormaliser dataNormaliser;
	
	// feature reader for clustering
	private FeatureSelector featureSelector;
	
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
		
		this.dataset = config.getString("dataset");		
		this.featureSelectionFactor = config.getInteger("datasetFeatureSelection", 1);
		
		this.learner = new CluStream(config.getInteger("maxClusters", 100), config.getInteger("maximumBoundaryFactor", 2), config.getInteger("relevanceThreshold", 1000), config.getInteger("initNumber", 1000));
		this.features = 0;
		
		this.sender = new MicroClusterMessageSender(this.instanceId, config.getString("mq.broker.host"), config.getInt("mq.broker.port"), config.getString("mq.queue"));
		
		this.jsonWriter = new Gson();
		
		LOG.info("{localClusterer={}, streamSpeed={}, dataset={}, featureSelectionFactor={}, initNumber={}}", this.instanceId, this.streamSpeed, config.getString("dataset"), featureSelectionFactor, config.getInteger("initNumber", 1000));
	}
	
	
	public void initialise() throws IOException{
		
		this.featureSelector = new KDD99FeatureSelector(this.dataset, 2, 0);		
		this.dataNormaliser = new MeanRangeDataNormalizer(featureSelector);
		this.dataNormaliser.setup();		
		
	}
	
	public void train() throws Exception{

		// keep track of the number of features that we have used
		int featuresUsed = 0;

		// restart the feature selector
		featureSelector.restart();

		FeatureVector fv = null;
		Map<FeatureVector, Integer> placement = null;

		// initialise the time
		timestamp = 1;

		// go through all available features
		while((fv = featureSelector.getNext()) != null){

			dataNormaliser.normalise(fv);			
			fv.setTimestamp(timestamp);

			// give it to the learner
			placement = learner.cluster(fv);

			// record the cluster id the feature was added to
			if (placement != null && !placement.isEmpty()){

				for (Entry<FeatureVector, Integer> entry : placement.entrySet()){
					fv = entry.getKey();
					fv.setInitialCluster(entry.getValue());
					LOG.info("fv=" + jsonWriter.toJson(fv));
				}

				// decide whether it is time to take a snapshot of the clusters
				if (featuresUsed >= 2000 && featuresUsed % 50 == 0){

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
			if (++featuresUsed % streamSpeed == 0){
				++timestamp;	
			}

		}

		if (learner.getClusters() != null){
			LOG.info("clusters=" + jsonWriter.toJson(learner.getClusters()));
		}

		LOG.info("totalFeatures={}", featuresUsed);
	}
	
	
	public static void main(String... args) throws Exception{
		
		long start = System.currentTimeMillis();
		
		// set up db
		// LOG.info("Setting up the database.");
		// DbUtils dbConfigurer = new DbUtils("/localdb.sql");
		// dbConfigurer.build();
		// LOG.info("Database ready.");
		
		// create the clusterer
		LocalClusterer clusterer = new LocalClusterer();
		clusterer.initialise();
		clusterer.train();
		
		LOG.info("executionTime={}ms", System.currentTimeMillis() - start);
		
		System.exit(0);
				
		
	}
	
}
