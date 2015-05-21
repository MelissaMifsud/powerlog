package net.melissam.powerlog.global;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.melissam.powerlog.clustering.MicroCluster;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

public class MacroClusterCreator {
		
	
	private static final String MICROCLUSTER_RESULT = "results\\microcluster.result";
	
	private static final Logger LOG = LogManager.getLogger(MacroClusterCreator.class);
	
	
	public static void main(String[] args){

		BufferedReader br = null;	
		
		try{

			// read the microclusters from the dataset
			FileInputStream fis = new FileInputStream(new File(MICROCLUSTER_RESULT));
			
			// read the first line
			br = new BufferedReader(new InputStreamReader(fis));
			String line = br.readLine();
			
			if (line != null && !line.isEmpty()){
				
				//  result is clusters=[]
				MicroCluster[] clusters = new Gson().fromJson(line.substring(line.indexOf("[")), MicroCluster[].class);
				
				if (clusters != null){
					
					LOG.info("Found {} clusters in results file.", clusters.length);
					
					long start = System.currentTimeMillis();
					
					ClustreamModifiedKMeansClusterer clusterer = new ClustreamModifiedKMeansClusterer();
					List<MicroCluster> _microClusters = new ArrayList<MicroCluster>(clusters.length);
					Collections.addAll(_microClusters, clusters);
					
					Map<MicroCluster, List<MicroCluster>> macroClusters = clusterer.doMacroClusterCreation(_microClusters, 5);
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
					
					
				}else{
					
					LOG.warn("No clusters found in results file.");
				}
				
			}else{
				
				LOG.warn("Empty micro-cluster result.");
				
			}
			

		}catch(IOException ex){

			LOG.error("Error reading micro-cluster result.", ex);
			System.exit(-1);
			
		}finally{
			
			if (br != null) try{ br.close(); } catch(IOException ex) { /* do nothing */}
		}

		System.exit(0);

		
	}

}
