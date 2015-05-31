package net.melissam.powerlog.normalisation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.melissam.powerlog.clustering.FeatureVector;
import net.melissam.powerlog.datasource.FeatureSelector;

/**
 * Performs attribute scaling using max and min values of attributes.
 * 
 * @author melissam
 *
 */
public class MeanRangeDataNormalizer implements DataNormaliser{

	// the difference between the minimum value and maximum value of an attribute
	double[] maxValues;
	
	// minimum values of attributes in the data set
	double[] minValues;	
	
	// number of features the mean was taken on
	private int size;
	
	// feature selector
	private FeatureSelector featureReader;
	
	// class logger
	private static final Logger LOG = LogManager.getLogger(MeanRangeDataNormalizer.class);
	
	public MeanRangeDataNormalizer(FeatureSelector featureReader) {
		this.featureReader = featureReader;
	}
	
	@Override
	public void setup() {
		
		long start = System.currentTimeMillis();
		
		FeatureVector fv = null;
		
		while((fv = featureReader.getNext()) != null){
			
			++size;
			
			// initialise the arrays of all attribute values 
			if (maxValues == null) {
				maxValues = new double[fv.getDimension()];
				for (int i = 0; i < maxValues.length; i++){
					// this works because we know that all values are positive numbers
					maxValues[i] = 0; 
				}
			}
			
			// initial min value array
			if (minValues == null){
				minValues = new double[fv.getDimension()];
				for (int i = 0; i < minValues.length; i++){
					minValues[i] = Double.MAX_VALUE;
				}
			}
			
			double[] rawAttributes = fv.getPoint();
			for (int i = 0; i < fv.getDimension(); i++){
				if (rawAttributes[i] < minValues[i]) minValues[i] = rawAttributes[i];
				if (rawAttributes[i] > maxValues[i]) maxValues[i] = rawAttributes[i];
			}
		}
		
		LOG.info("Normalisation initialised from {} items in {}ms.", size, System.currentTimeMillis()-start);
		
	}

	@Override
	public void normalise(FeatureVector featureVector) {
		
		for (int i = 0; i < featureVector.getDimension(); i++){
			if (maxValues[i] == minValues[i]) featureVector.set(i, 0.0);
			else featureVector.set(i, (featureVector.get(i) - minValues[i]) / (maxValues[i] - minValues[i]));
		}
		
	}

}
