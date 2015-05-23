package net.melissam.powerlog.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import net.melissam.powerlog.clustering.MicroCluster;
import net.melissam.powerlog.local.LocalClusterer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

public class MicroClusteringAnalyser {

	private String resultsFilename;
	private String datasetFilename;

	// map feature type (from training set) to feature number (line number)
	private Map<Integer, String> featureTypes;
	
	// map of which cluster each feature was put in
	private Map<Integer, Set<Integer>> featureCluster;
	
	// list of clusters by time
	private TreeMap<Long, MicroCluster[]> clusterHistory;
	
	// Class logger
	// private static final Logger LOG = LogManager.getLogger(MicroClusteringAnalyser.class);

	public MicroClusteringAnalyser(String resultsFilename, String datasetFilename){
		this.resultsFilename = resultsFilename;
		this.datasetFilename = datasetFilename;
	}

	public void mapFeatureTypes() throws IOException{

		System.out.println("Creating a map of feature id - type");
		
		featureTypes = new HashMap<Integer, String>();

		// read feature from dataset file
		LineIterator it = FileUtils.lineIterator(new File(datasetFilename), "UTF-8");
		try {
			
			// dataset line
			String line = null;
			// feature counter
			int i = 0;

			while (it.hasNext()) {

				line = it.nextLine();				
				featureTypes.put(++i, line.substring(line.lastIndexOf(",") + 1));
			}

		} finally {
			LineIterator.closeQuietly(it);
		}

		System.out.println("Map creation ready.");
	}
	
	
	
	public void processResults() throws IOException{
		
		System.out.println("Processing results.");
		
		featureCluster = new HashMap<Integer, Set<Integer>>();
		clusterHistory = new TreeMap<Long, MicroCluster[]>();
		
		Gson gson = new Gson();
		
		LineIterator it = FileUtils.lineIterator(new File(resultsFilename), "UTF-8");
		try {
			
			// dataset line
			String line = null;
			// feature counter
			int i = 0;
			
			String clusterSummary = null;

			while (it.hasNext()) {

				line = it.nextLine();		
				
				// check what type of line this is 
				if (line.contains("fv={")){
					
					// feature vector placement 
					LinkedTreeMap<String, Object> feature = gson.fromJson(line.substring(line.indexOf("{")), LinkedTreeMap.class); 
					
					int clusterId = ((Double)feature.get("initialCluster")).intValue();					
					Set<Integer> clusterFeatures = featureCluster.get(clusterId);
					
					if (clusterFeatures == null) clusterFeatures = new HashSet<Integer>();
					clusterFeatures.add(i++);					
					featureCluster.put(clusterId, clusterFeatures);
					
				}else if(line.contains("clusters=[{")) {
					
					// cluster summary line
					// clusterHistory.put(new Long(i), gson.fromJson(line.substring(line.indexOf("[{")), MicroCluster[].class));
					clusterSummary = line;
					
				}
										
			}
			
			clusterHistory.put(new Long(i), gson.fromJson(clusterSummary.substring(clusterSummary.indexOf("[{")), MicroCluster[].class));

		} finally {
			LineIterator.closeQuietly(it);
		}
		
		System.out.println("Processing results ready.");
		System.out.println("-------------------------");
		
	}
	
	
	public void displayResults(){
		
		Map<Integer, Set<Integer>> clusterFeatures = new HashMap<Integer, Set<Integer>>();
		
		// get the final cluster states
		MicroCluster[] finalClusters = clusterHistory.lastEntry().getValue();
		System.out.println(finalClusters.length + " finalClusters");
		
		Gson gson = new Gson();
		for (MicroCluster cluster : finalClusters){
			
			// output the cluster if it is not empty
			if (cluster.getSize() > 0){
				
				System.out.println(cluster.getIdList().get(0) + " - " + gson.toJson(cluster));
				
				Set<String> featureTypes = new HashSet<String>();
				
				// get the feature type
				Set<Integer> allFeatures = new HashSet<Integer>();
				
				Set<Integer> features = new HashSet<Integer>();
				for (int id : cluster.getIdList()){
					features = featureCluster.get(id);
					if (features != null){
						for (int feature : features){
							featureTypes.add(this.featureTypes.get(feature));
						}
						
						allFeatures.addAll(features);
					}					
					
				}
				
				clusterFeatures.put(cluster.getIdList().get(0), allFeatures);
				
				// output feature types
				for (String type : featureTypes){
					System.out.print(type + " ");
				}
			}
			
			System.out.println("\n-------------------------------------");
		}
		
		// verify contents
		// check that there are no duplicate features in clusters
		Set<Integer> allFeatures = new HashSet<Integer>();
		for (Entry<Integer, Set<Integer>> entry : clusterFeatures.entrySet()){
			
			int currentSize = allFeatures.size();
			System.out.print("Microcluster " + entry.getKey() + " = " + entry.getValue().size());
			allFeatures.addAll(entry.getValue());
			System.out.print("; allFeatures = " + allFeatures.size());
			System.out.println(allFeatures.size() == currentSize + entry.getValue().size() ? "OK!" : "DUPLICATES!");
		}
		
	}
	


	public static void main(String... args) throws IOException{

		MicroClusteringAnalyser analyser = new MicroClusteringAnalyser("logs/powerlog-3.log", "datasets/kddcup.data_10_percent_corrected");
		analyser.mapFeatureTypes();
		analyser.processResults();
		analyser.displayResults();

	}

}
