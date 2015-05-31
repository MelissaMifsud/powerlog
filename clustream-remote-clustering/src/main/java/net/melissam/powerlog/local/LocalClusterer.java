package net.melissam.powerlog.local;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jms.JMSException;

import net.melissam.powerlog.clustering.CluStream;
import net.melissam.powerlog.clustering.Cluster;
import net.melissam.powerlog.clustering.ClustreamModifiedKMeansClusterer;
import net.melissam.powerlog.clustering.FeatureVector;
import net.melissam.powerlog.clustering.MicroCluster;
import net.melissam.powerlog.datasource.FeatureSelector;
import net.melissam.powerlog.datasource.KDD99FeatureSelector;
import net.melissam.powerlog.evaluation.SSQEvaluation;
import net.melissam.powerlog.local.db.LocalDbUtils;
import net.melissam.powerlog.normalisation.DataNormaliser;
import net.melissam.powerlog.normalisation.NormalisationProcess;
import net.melissam.powerlog.normalisation.SlidingWindowStatisticalDataNormaliser;
import net.melissam.powerlog.normalisation.StatisticalDataNormaliser;

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
	
	// the number of features to use for initialisation of clusters
	// this will also be used as the number of features to use for initialising sliding window normalisation if it is selected
	private int initNumber;
	
	// the clustering learner
	private CluStream learner;
	
	// object that sends microclusters to Global clustering
	private MicroClusterMessageSender sender;
	
	// object responsible for normalising features before they are used for training
	private DataNormaliser dataNormaliser;
	private NormalisationProcess normalisationProcess;
	
	// feature reader for clustering
	private FeatureSelector featureSelector;
	
	// Json writer
	private Gson jsonWriter;
	
	// Db utils if snapshots are going to be persisted
	private LocalDbUtils dbConfigurer;
	
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
		
		this.initNumber = config.getInteger("initNumber", 2000);
				
		this.learner = new CluStream(config.getInteger("maxClusters", 100), config.getInteger("maximalBoundaryFactor", 2), config.getInteger("relevanceThreshold", 1000), initNumber);
		this.features = 0;
		
		this.sender = new MicroClusterMessageSender(this.instanceId, config.getString("mq.broker.host"), config.getInt("mq.broker.port"), config.getString("mq.queue"));
		
		this.jsonWriter = new Gson();
		
		LOG.info("{localClusterer={}, streamSpeed={}, dataset={}, featureSelectionFactor={}, initNumber={}}", this.instanceId, this.streamSpeed, config.getString("dataset"), featureSelectionFactor, config.getInteger("initNumber", 1000));
	}
	
	public void train(boolean evaluate) throws Exception{
		
		// if we are going to evaluate the clustering then we need to keep a list of all the features that were clustered
		// if normalising the data points, keep the normalised points
		List<FeatureVector> points = null;
		if (evaluate){
			points = new ArrayList<FeatureVector>();
		}

		// keep track of the number of features that we have used
		int featuresUsed = 0;

		// restart the feature selector
		this.featureSelector = new KDD99FeatureSelector(this.dataset, 2, instanceId % 2);		
		
		FeatureVector fv = null;
		Map<FeatureVector, Integer> placement = null;
		
		// if the normalisation type is instream, then we need to buffer the first initNumber features
		if (normalisationProcess == NormalisationProcess.INSTREAM){
		
			SlidingWindowStatisticalDataNormaliser swsdn = (SlidingWindowStatisticalDataNormaliser)this.dataNormaliser;
			List<FeatureVector> buffer = new ArrayList<FeatureVector>(initNumber);
			while(buffer.size() < initNumber && (fv = featureSelector.getNext()) != null){
				
				// update the timestamp
				adjustTimestamp(++featuresUsed);				
				fv.setTimestamp(timestamp);
				
				// add to buffer
				buffer.add(fv);
				
				// add to normaliser
				swsdn.add(fv);
			}
			
			// give the features to the learner now that we can normalise them
			for (FeatureVector buffered : buffer){
				// normalise the point
				swsdn.normalise(buffered);
				// add it to a list for evaluation later on
				if (evaluate) points.add(buffered);
				// give it to the learner
				placement = learner.cluster(buffered);
				handleFeaturePlacement(placement, buffered);
			}
			
			handleSnapshot(featuresUsed);
		}		

		// go through all available features
		while((fv = featureSelector.getNext()) != null){
			
			// update the timestamp
			adjustTimestamp(++featuresUsed);
			fv.setTimestamp(timestamp);
			
			// normalise if we are normalising
			if (dataNormaliser != null) {	
				
				// if using in-stream normalisation, give the feature to the normaliser for recalculation
				if (normalisationProcess == NormalisationProcess.INSTREAM){
					((SlidingWindowStatisticalDataNormaliser)dataNormaliser).add(fv);
				}
				
				// normalise the feature
				dataNormaliser.normalise(fv);
			}
			
			// save point for later if we are evaluating
			if (evaluate) points.add(fv);

			// give it to the learner
			placement = learner.cluster(fv);
			
			// handle the placement information (log / persist )
			handleFeaturePlacement(placement, fv);
			
			// check if it's time to take a snapshot
			handleSnapshot(featuresUsed);

		}

		if (learner.getClusters() != null){
			LOG.info("clusters=" + jsonWriter.toJson(learner.getClusters()));			
			LOG.info("Starting macro-clustering phase using {} micro-clusters.", learner.getClusters().size());
			
			long start = System.currentTimeMillis();
			
			ClustreamModifiedKMeansClusterer clusterer = new ClustreamModifiedKMeansClusterer();
			Map<Cluster, List<MicroCluster>> macroClusters = clusterer.doMacroClusterCreation(learner.getClusters(), 5);			
			LOG.info("{} macro clusters in {}ms", macroClusters.size(), System.currentTimeMillis() - start);
									
			// let's output the results to a results file that is ready for importing into Matlab for visualisation
			saveResultsToFile(macroClusters);
			
			if (evaluate){				
				// run evaluations
				LOG.info("SSQ = {} for {} features.", new SSQEvaluation().evaluate(macroClusters.keySet(), points), points.size());				
			}
		
		}

		LOG.info("totalFeatures={}", featuresUsed);
	}
	
	
	public static void main(String... args) throws Exception{
		
		long start = System.currentTimeMillis();
		
		// create the clusterer
		LocalClusterer clusterer = new LocalClusterer();
		clusterer.initialise(args);
		clusterer.train(true);
		
		LOG.info("executionTime={}ms", System.currentTimeMillis() - start);
		
		System.exit(0);
				
		
	}
	
	//---------------------------------- Initialisation methods. ---------------------------------
	
	/**
	 * Initialise using process arguments.
	 * 
	 * @param args			Any arguments the user may have added when starting the process.
	 * @throws Exception	Thrown if anything goes wrong, aborting the whole process
	 */
	public void initialise(String[] args) throws Exception{		
		
		// initialiseDatabase();		
		initialiseNormalisation(args);	
		
	}
	
	
	private void initialiseDatabase() throws Exception{

		// set up db
		LOG.info("Setting up the database.");
		dbConfigurer = new LocalDbUtils("localhost", 8089, "powerlog", "admin", "admin");
		dbConfigurer.build(" /localdb.sql");
		LOG.info("Database ready.");

	}
	
	private void initialiseNormalisation(String[] args) throws IOException{
		
		String normalisationType = "none"; // set choice to default for now
		
		// find out what the user chose
		for (String arg : args){
			if (arg.startsWith("-n=")){				
				normalisationType = arg.substring(arg.lastIndexOf("=") + 1);				
			}
		}
		
		this.normalisationProcess = NormalisationProcess.fromName(normalisationType);
		
		switch(normalisationProcess){
		
		case BEFORE		: 	this.dataNormaliser = new StatisticalDataNormaliser(new KDD99FeatureSelector(this.dataset, 1, 0));		
							this.dataNormaliser.setup();		
							break;
		
		case INSTREAM 	: 	this.dataNormaliser = new SlidingWindowStatisticalDataNormaliser(this.initNumber, 100);
							break;
		
		default 		: // no normalisation
		
		
		}
		
		
	}
	
	private void adjustTimestamp(int featuresUsed){
		
		// adjust timestamp according to stream speed, if needed
		if (featuresUsed % streamSpeed == 0){
			++timestamp;	
		}
		
	}
	
	private void handleFeaturePlacement(Map<FeatureVector, Integer> placement, FeatureVector placed){
		
		// record the cluster id the feature was added to
		if (placement != null && !placement.isEmpty()){

			FeatureVector fv = null;
			for (Entry<FeatureVector, Integer> entry : placement.entrySet()){
				fv = entry.getKey();
				fv.setInitialCluster(entry.getValue());
				LOG.info("fv=" + jsonWriter.toJson(fv));
			}

		}else{
			LOG.info("featureVector={} used for initialisation.", placed.getId());
		}

	}
	
	private void handleSnapshot(int featuresUsed){		

		// decide whether it is time to take a snapshot of the clusters
		if (featuresUsed >= initNumber && featuresUsed % streamSpeed == 0){

			List<MicroCluster> clusters = learner.getClusters();
			LOG.info("snapshot-time={}, clusters={}", timestamp, jsonWriter.toJson(clusters));

			// send to Global
			try{
				sender.send(clusters, timestamp);
			}catch(JMSException ex){
				LOG.error("Error sending clusters to Global.", ex);
			}
		}

	}
	
	/**
	 * Saves results to a separate file, whilst also logging them.
	 * 
	 * @param macroClusters	The final macro clusters.
	 */
	private void saveResultsToFile(Map<Cluster, List<MicroCluster>> macroClusters){
		
		File resultFile = null;
		FileOutputStream fos = null;
		BufferedWriter writer = null;
		
		try{
			
			resultFile = new File("results/results-" + new SimpleDateFormat("yyyyMMddhhmm").format(System.currentTimeMillis()) + ".txt");
			fos = new FileOutputStream(resultFile);
			
			writer = new BufferedWriter(new OutputStreamWriter(fos));
			int mc = 0;
			String line = null;
			for (Entry<Cluster, List<MicroCluster>> entry : macroClusters.entrySet()){
				
				Cluster macroCluster = entry.getKey();						
				line = String.format("MacroCluster=%s, center=%s, radius=%s, microClusters=%s",++mc, jsonWriter.toJson(macroCluster.getCenter()), macroCluster.getRadius(), entry.getValue().size());
				LOG.info(line);
				writer.write(line); writer.newLine();
				
				int _mc = 0;
				for (MicroCluster microCluster : entry.getValue()){
					line = String.format("MicroCluster=%s, center=%s, radius=%s, size=%s", ++_mc, jsonWriter.toJson(microCluster.getCenter()), microCluster.getRadius(), microCluster.getSize());
					LOG.info(line);
					writer.write(line); writer.newLine();
				}
				
			}
			
			
		}catch(IOException ex){
			
			LOG.error("Could not write results to file.", ex);
			
		}finally{
			
			if (writer != null) try{ writer.close(); } catch(IOException ex) { /* close quietly */ }
			if (fos != null) try{ fos.close(); } catch(IOException ex) { /* close quietly */ }
			
		}
		
	}
	
}
