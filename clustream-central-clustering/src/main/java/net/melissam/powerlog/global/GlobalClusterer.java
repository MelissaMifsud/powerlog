package net.melissam.powerlog.global;

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

import net.melissam.powerlog.clustering.*;
import net.melissam.powerlog.messaging.MicroClusterMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

public class GlobalClusterer {
	
	// the clustering learner
	private CluStream learner;
	
	// number of microClusters that have been received, these are the features for the CluStream learner for global clustering
	private int microClustersReceived;
	
	// how many microClusters to receive in order to increment the timestamp
	private int streamSpeed;
	private int timestamp;

	// snapshot frequencies
	private int snapshotAlpha;
	private int snapshotL;
	
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
		
		setupLeaner(config.getInteger("maxClusters", 100), config.getInteger("maximumBoundaryFactor", 2), config.getInteger("relevanceThreshold", 1000), config.getInteger("initNumber", 1000));

		// set up stream and snapshot configuration
		streamSpeed = config.getInteger("streamSpeed", 2000);
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
		this.train();
	}
	
	private void stop(){
		this.running = false;
	}
	
	private void train(){
		
		int misses = 0;
		
		// read messages off the queue 
		while(running){
		
			try{
				// read a message
				Message message = messageConsumer.receive(1000);
				
				if (message != null){
					
					// reset misses
					misses = 0;
					
					MicroClusterMessage microClusterMessage = (MicroClusterMessage)((ObjectMessage)message).getObject(); 				
					LOG.info("Received message from instanceid={}, timestamp={}", microClusterMessage.getInstanceId(), microClusterMessage.getTimestamp());
					
					timestamp++;
					
					Map<FeatureVector, Integer> placement = null; // keep info about where CluStream puts the feature vector
					for (MicroCluster microCluster : microClusterMessage.getMicroClusters()){
						
						FeatureVector fv = new FeatureVector(microClustersReceived);
						fv.setTimestamp(timestamp);
						fv.addAll(microCluster.getCenter());
						
						// give it to the learner
						placement = learner.cluster(fv);
						
						++microClustersReceived;
						
						// record the cluster id the feature was added to
						if (placement != null && !placement.isEmpty()){
							
							for (Entry<FeatureVector, Integer> entry : placement.entrySet()){
								fv = entry.getKey();
								fv.setInitialCluster(entry.getValue());
								LOG.info("fv=" + jsonWriter.toJson(fv));
							}

							// decide whether it is time to take a snapshot of the clusters
							if (microClustersReceived % 2000 == 0){
							
								List<MicroCluster> clusters = learner.getClusters();
								LOG.info("clusters=" + jsonWriter.toJson(clusters));
								
								// later save for macro-clustering
								
							}
							
						}else{
							LOG.info("featureVector={} used for initialisation.", fv.getId());
						}
						
					}
					
					
				}else{ 
					
					if (++misses > 300) running = false;
					
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
		
		Map<MicroCluster, List<MicroCluster>> macroClusters = new ClustreamModifiedKMeansClusterer().doMacroClusterCreation(learner.getClusters(), 5);
		LOG.info("{} macro clusters in {}ms", macroClusters.size(), System.currentTimeMillis() - start);
		
		int mc = 0;
		for (Entry<MicroCluster, List<MicroCluster>> entry : macroClusters.entrySet()){
			
			MicroCluster macroCluster = entry.getKey();						
			LOG.info("MacroCluster={}, center={}, radius={}, deviation={}, microClusters={}",++mc, macroCluster.getCenter(), macroCluster.getRadius(), macroCluster.getDeviation(), entry.getValue().size());
			
			int _mc = 0;
			for (MicroCluster microCluster : entry.getValue()){
				LOG.info("MicroCluster={}, center={}, radius={}, deviation={}",++_mc, microCluster.getCenter(), microCluster.getRadius(),microCluster.getDeviation());
			}
			
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
}
