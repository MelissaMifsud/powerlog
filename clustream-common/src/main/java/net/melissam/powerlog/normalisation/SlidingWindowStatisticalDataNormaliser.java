package net.melissam.powerlog.normalisation;

import java.util.LinkedList;

import net.melissam.powerlog.clustering.FeatureVector;

/**
 * Implements a sliding window normalisation technique that is adjusted as the stream evolves.
 * @author melissam
 *
 */
public class SlidingWindowStatisticalDataNormaliser implements DataNormaliser  {

	private int windowSize;
	private static final int WINDOW_SIZE = 2000;
	
	private int slidingRate;
	private static final int SLIDING_RATE = 1000;
	
	private LinkedList<double[]> window;
		
	private int currentWindowSize;
	
	private double[] sd;
	private double[] mean;
		
	
	public SlidingWindowStatisticalDataNormaliser(){
		
		// use default values
		this(WINDOW_SIZE, SLIDING_RATE);
		
	}
	
	public SlidingWindowStatisticalDataNormaliser(int windowSize, int slidingRate){
		
		this.windowSize = windowSize;
		this.slidingRate = slidingRate;
		
		this.window = new LinkedList<double[]>();
		this.currentWindowSize = 0;
		
	}
	
	public void add(FeatureVector fv){
		
		// manage window size
		if (window.size() == windowSize){
			window.removeFirst();
		}
		
		window.add(fv.getPoint());
		++currentWindowSize;
		
		// is it time to re-normalize?
		if (window.size() == windowSize && currentWindowSize >= slidingRate){
			recalculate(fv.getDimension());
		}
				
	}
	
	/**
	 * Recalculates the standard deviation and mean based on the current window values.
	 * 
	 * @param dimension	The dimension of the points in the window.
	 */
	private void recalculate(int dimension){
		
		// reset current calculations
		sd = new double[dimension];
		mean = new double[dimension];
		
		// we need the sum for calculating the mean
		double[] sum = new double[dimension];
		
		// recalculate on current window
		for(double[] point : window){						
			for (int i = 0; i < point.length; i++){
				sum[i] += point[i];
			}
		}
				
		// calculate the mean from the sum
		for (int i = 0; i < sum.length; i++){
			mean[i] = sum[i] / window.size();
			sd[i] = (sum[i] - mean[i]) / window.size();
		}
		
		// reset sliding window
		currentWindowSize = 0;
		
	}
	
	@Override
	public void normalise(FeatureVector featureVector) {
		
		assert(featureVector.getDimension() == mean.length);
		
		for(int i = 0; i < featureVector.getDimension(); i++){
			if (sd[i] != 0) featureVector.set(i, (featureVector.get(i) - mean[i]) / sd[i]);
			else featureVector.set(i, (featureVector.get(i) - mean[i]));
		}
	
	}
	
	
	@Override
	public void setup(){
		// nothing to do
	}
	
	
}
