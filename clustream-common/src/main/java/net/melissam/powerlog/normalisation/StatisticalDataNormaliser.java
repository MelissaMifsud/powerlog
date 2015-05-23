package net.melissam.powerlog.normalisation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.melissam.powerlog.clustering.FeatureVector;
import net.melissam.powerlog.datasource.FeatureSelector;

/**
 * Implements statistical data normalisation using standard normal distribution with mean 0 and unit variance.
 * 
 * @author mmelissam
 *
 */
public class StatisticalDataNormaliser implements DataNormaliser {
	
	// the mean of each attribute
	private double[] mean;
	
	// standard deviation of each attribute
	private double[] sd;
	
	// number of features the mean was taken on
	private int size;
	
	// feature selector
	private FeatureSelector featureReader;
	
	// class logger
	private static final Logger LOG = LogManager.getLogger(StatisticalDataNormaliser.class);
	
	/**
	 * Initilise the class with parameters to choose data from file to build normalisation on.
	 * 
	 * @param mod			The mod of the entry number to take for selection.
	 * @param remainder		The modulus remainder for selection.
	 */
	public StatisticalDataNormaliser(FeatureSelector featureReader) {
		this.featureReader = featureReader;
	}

	@Override
	public void setup() {
		
		long start = System.currentTimeMillis();
		
		double[] sum = null;
		
		FeatureVector fv = null;
		
		while((fv = featureReader.getNext()) != null){
			
			++size;
			
			// initialise the sum of all attribute values 
			if (sum == null) {
				sum = new double[fv.getDimension()];
				for (int i = 0; i < sum.length; i++){
					sum[i] = 0;
				}
			}
			
			double[] rawAttributes = fv.getPoint();
			for (int i = 0; i < fv.getDimension(); i++){
				sum[i] += rawAttributes[i];
			}
		}
		
		
		// calculate the mean from the sum
		this.mean = new double[sum.length];
		this.sd = new double[sum.length];
		for (int i = 0; i < sum.length; i++){
			this.mean[i] = sum[i] / size;
			this.sd[i] = (sum[i] - mean[i]) / size;
		}
		
		LOG.info("Normalisation initialised from {} items in {}ms.", size, System.currentTimeMillis()-start);
		
	}

	@Override
	public void normalise(FeatureVector featureVector) {
		
		assert(featureVector.getDimension() == mean.length);
		
		for(int i = 0; i < featureVector.getDimension(); i++){
			if (sd[i] != 0) featureVector.set(i, (featureVector.get(i) - mean[i]) / sd[i]);
			else featureVector.set(i, (featureVector.get(i) - mean[i]));
		}
	
	}

}
