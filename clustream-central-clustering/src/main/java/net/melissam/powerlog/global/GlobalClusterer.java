package net.melissam.powerlog.global;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;

import net.melissam.powerlog.clustering.CluStream;
import net.melissam.powerlog.clustering.Cluster;
import net.melissam.powerlog.clustering.ClustreamModifiedKMeansClusterer;
import net.melissam.powerlog.clustering.FeatureVector;
import net.melissam.powerlog.clustering.MicroCluster;
import net.melissam.powerlog.evaluation.SSQEvaluation;
import net.melissam.powerlog.messaging.MicroClusterMessage;
import net.melissam.powerlog.normalisation.SlidingWindowStatisticalDataNormaliser;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

/**
 * Main class for central node processing.
 * 
 * @author melissam
 *
 */
public class GlobalClusterer {
	
	// the clustering learner
	private CluStream learner;
	
	// number of microClusters that have been received, these are the features for the CluStream learner for global clustering
	private int microClustersReceived;
	
	// how many microClusters to receive in order to increment the timestamp
	private int timestamp;

	// snapshot frequencies
	private int snapshotAlpha;
	private int snapshotL;
	
	// the number of features to buffer for initialisation
	private int initNumber;
	
	// MQ connection objects
	private Connection connection;
	private Session session;
	private MessageConsumer messageConsumer;
	
	// control processing
	private boolean running;
	
	// Json writer
	private Gson jsonWriter;
	
	// name of queue to receive micro cluster information on
	private static final String QUEUE_NAME = "mq-microclusters";
	
	// a client id for this consumer off the queue.
	// we will only have one consumer so we'll give it a default
	private static final String CLIENT_ID = "GlobalClusterer"; 
	
	// Class logger
	private static final Logger LOG = LogManager.getLogger(GlobalClusterer.class);
	
	
	public GlobalClusterer() throws ConfigurationException, JMSException{
		
		// read properties
		PropertiesConfiguration config = new PropertiesConfiguration("global.properties");
		
		initNumber = config.getInteger("initNumber", 1000);
		
		setupLeaner(config.getInteger("maxClusters", 100), config.getInteger("maximalBoundaryFactor", 2), config.getInteger("relevanceThreshold", 1000), initNumber);

		// set up stream and snapshot configuration
		// streamSpeed = config.getInteger("streamSpeed", 2000);
		snapshotAlpha = config.getInteger("snapshot.a", 2);
		snapshotL = config.getInteger("snapshot.l", 10);
		
		microClustersReceived = 0;
		timestamp = 0;
		
		// set up queue consumer to read micro-clusters to be used as features
		setupQueueConsumer(config.getInteger("mq.broker.port", 61616));
		
		// initialise the Gson writer for output
		jsonWriter = new Gson();
	
	}
	
	
	private void setupLeaner(int maxClusters, int maximalBoundaryFactor, int relevanceThreshold, int initNumber){
		
		this.learner = new CluStream(maxClusters, maximalBoundaryFactor, relevanceThreshold, initNumber);
		
	}
	
	
	private void setupQueueConsumer(int mqPort) throws JMSException{
		
		// create a Connection Factory
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:" + mqPort + "?jms.prefetchPolicy.queuePrefetch=5000");

        // create a Connection
        connection = connectionFactory.createConnection();
        connection.setClientID(CLIENT_ID);

        // create a Session
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        // create the Queue from which messages will be received
        Queue queue = session.createQueue(QUEUE_NAME);

        // create a MessageConsumer for receiving messages
        messageConsumer = session.createConsumer(queue);

        // start the connection in order to receive messages
        connection.start();
        
        LOG.info("GlobalClusterer initialised.");
		
	}
	
	
	private void start(){
		this.running = true;
		this.train(true);
	}
	
	private void stop(){
		this.running = false;
	}
	
	private void train(boolean evaluate){
		
		// if we are going to evaluate the clustering, we need to keep track of the points that have been clustered
		List<FeatureVector> points = null;
		if (evaluate) points = new ArrayList<FeatureVector>();
		
		int misses = 0;
		
		SlidingWindowStatisticalDataNormaliser normaliser = new SlidingWindowStatisticalDataNormaliser(initNumber, 100);
		List<FeatureVector> buffer = new ArrayList<FeatureVector>();
		boolean initialised = false;
	
		// read messages off the queue 
		while(running){
		
			try{
				
				// read a message off the queue
				Message message = messageConsumer.receive(1000);
				
				if (message != null){
					
					// reset misses
					// if we keep getting misses for a specified time, the clusterer will stop
					misses = 0;
					
					MicroClusterMessage microClusterMessage = (MicroClusterMessage)((ObjectMessage)message).getObject(); 				
					LOG.info("Received message from instanceid={}, timestamp={}", microClusterMessage.getInstanceId(), microClusterMessage.getTimestamp());
					
					timestamp++;								
					
					// go through all micro-clusters sent from remote site
					for (MicroCluster microCluster : microClusterMessage.getMicroClusters()){
						
						// create a feature vector from the centroid of the micro-cluster sent from
						// the remote instance
						FeatureVector fv = new FeatureVector(++microClustersReceived);
						fv.setTimestamp(timestamp);
						fv.addAll(microCluster.getCenter());
						
						// buffer the feature vector until we have enough to base normalisation on
						if (buffer.size() < initNumber){						
							buffer.add(fv);
							// add to normaliser
							normaliser.add(fv);
							continue;
						}
						
						// initialise normalisation
						if (!initialised){
							
							for (FeatureVector buffered : buffer){
								
								// normalise the feature
								normaliser.normalise(buffered);
								
								// add it to a list for evaluation later on
								if (evaluate) points.add(buffered);
								
								// give it to the learner and handle the cluster assignment
								handlePlacement(learner.cluster(buffered), buffered);						
							
							}
							
							initialised = true;
							
						}						
						
						// normalise the current feature
						normaliser.add(fv);
						normaliser.normalise(fv);
						
						// give it to the current feature to learner
						handlePlacement(learner.cluster(fv), fv);
						
						// keep feature for later if we are evaluating
						if (evaluate) points.add(fv);
						
					}
					
					
				}else{ 
					
					if (++misses > 150) running = false;
					
					try{
						Thread.sleep(1000);
					}catch(InterruptedException ex){
						// do nothing
					}
				}
				
			}catch(JMSException ex){
				LOG.error("Error reading message off the queue={}.", QUEUE_NAME, ex);
				if (++misses > 300) running = false;
			}
			
			
		}
		
		LOG.info("Micro-clustering ended.");
		try{
			
			messageConsumer.close();
			connection.close();
			
		}catch(JMSException ex){
			LOG.error("Error whilst closing JMS connection.", ex);
		}			

		LOG.info("clusters=" + jsonWriter.toJson(learner.getClusters()));
		LOG.info("Starting macro-clustering phase using {} micro-clusters.", learner.getClusters().size());
		
		long start = System.currentTimeMillis();
		
		ClustreamModifiedKMeansClusterer clusterer = new ClustreamModifiedKMeansClusterer();
		Map<Cluster, List<MicroCluster>> macroClusters = clusterer.doMacroClusterCreation(learner.getClusters(), 5);
		LOG.info("{} macro clusters in {}ms", macroClusters.size(), System.currentTimeMillis() - start);		
		saveResultsToFile(macroClusters);
		
		if (evaluate){				
			// run evaluations
			LOG.info("SSQ = {} for {} features.", new SSQEvaluation().evaluate(macroClusters.keySet(), points), points.size());				
		}
		
		
	}
	
	
	public static void main(String... args){
		
		long start = System.currentTimeMillis();
		
		try{
			final GlobalClusterer clusterer = new GlobalClusterer();

			Runtime.getRuntime().addShutdownHook(new Thread("ShutdownHook-GlobalCluster"){

				public void run(){

					clusterer.stop();

				}

			});

			clusterer.start();
			
		}catch(JMSException | ConfigurationException ex){
			
			LOG.error("Could not initialise clusterer.", ex);
			
		}
		
		LOG.info("executionTime={}ms", System.currentTimeMillis() - start);
		System.exit(0);
		
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
			
			LOG.error("Could not write results to file.");
			
		}finally{
			
			if (writer != null) try{ writer.close(); } catch(IOException ex) { /* close quietly */ }
			if (fos != null) try{ fos.close(); } catch(IOException ex) { /* close quietly */ }
			
		}
		
	}
	
	
	private void handlePlacement(Map<FeatureVector, Integer> placement, FeatureVector placed){
		
		// record the cluster id the feature was added to
		if (placement != null && !placement.isEmpty()){
			
			FeatureVector fv = null;
			for (Entry<FeatureVector, Integer> entry : placement.entrySet()){
				fv = entry.getKey();
				fv.setInitialCluster(entry.getValue());
				LOG.info("fv=" + jsonWriter.toJson(fv));
			}

			// decide whether it is time to take a snapshot of the clusters
			if (microClustersReceived % 2000 == 0){
			
				List<MicroCluster> clusters = learner.getClusters();
				LOG.info("snapshot-time={}, clusters=" + jsonWriter.toJson(clusters));
				
				// later save for macro-clustering
				
			}
			
		}else{
			LOG.info("featureVector={} used for initialisation.", placed.getId());
		}
	}
}
