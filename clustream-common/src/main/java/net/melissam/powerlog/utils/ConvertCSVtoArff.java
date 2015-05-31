package net.melissam.powerlog.utils;

import java.io.File;

import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;

/**
 * Converts the normalised KDD Cup 99 data set to an arff file that can be used with MOA.
 * 
 * @author melissam
 *
 */
public class ConvertCSVtoArff {

	  /**
	   * takes 2 arguments:
	   * - CSV input file
	   * - ARFF output file
	   */
	  public static void main(String[] args) throws Exception {
	 
	    // load CSV
	    CSVLoader loader = new CSVLoader();
	    loader.setSource(new File("kdd-data-withlabels.csv"));
	    Instances data = loader.getDataSet();
	 
	    // save ARFF
	    ArffSaver saver = new ArffSaver();
	    saver.setInstances(data);
	    saver.setFile(new File("kdd-data-withlabels.arff"));
	    saver.writeBatch();
	  }
	
}
