package net.melissam.powerlog.datasource;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import net.melissam.powerlog.clustering.FeatureVector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KDD99FeatureSelector implements FeatureSelector {

	// Reader to go trough dataset file
	private String filename;
	private BufferedReader reader;
	
	// file lines selectors
	private int mod;
	private int remainder;
	
	// keep track of line number
	private int lineNumber;
	
	// class logger
	private static final Logger LOG = LogManager.getLogger(KDD99FeatureSelector.class);
	
	
	public KDD99FeatureSelector(String filename, int mod, int remainder) throws IOException {
		
		this.filename = filename;
		this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
		
		this.mod = mod; 
		this.remainder = remainder;
		
		this.lineNumber = 0;
	}
	

	@Override
	public FeatureVector getNext() {
		
		FeatureVector next = null;
		
		try{
			
			String line = null;
			
			// get the line we're meant to be reading
			do{
				
				line = reader.readLine();
				
			}while(line != null && ++lineNumber % mod == remainder);
			
			if (line != null){
				
				next = getFeatureVector(line);
				
			}else{
				
				// close the file
				close();
			}
			
		}catch(IOException ex){
			
			// stop reding if there is an exception
			LOG.warn("Exception reading features. No more features will be read.", ex);
			
			close();
		}
		
		return next;	
		
	}
	
	public int getFeatureCount(){		
		return this.lineNumber;		
	}
	
	public void restart() throws Exception {
		this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
		this.lineNumber = 0;
	}
	
	private void close(){
		if (reader != null){
			
			try{
				
				reader.close();
				
			}catch(IOException ex){
				
				LOG.warn("Problem closing dataset file.", ex);
			}
			
		}
	}
	
	
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
			fv = new FeatureVector(lineNumber);
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
			
			fv.setGroundTruthLabel(_attributes[41]);
			
			// LOG.info("Created feature vector with {} attributes.", fv.size());
			
		}else{
			LOG.warn("Incorrect number of attributes. expected=42, found={}", _attributes.length);
		}
		
		return fv;
		
	}

}
