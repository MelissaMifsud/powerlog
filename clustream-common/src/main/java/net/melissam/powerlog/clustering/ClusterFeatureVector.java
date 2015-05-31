package net.melissam.powerlog.clustering;

/**
 * Representation of a cluster feature vector which keeps track of cluster sum of values and sum of square
 * values of cluster features.
 * 
 * @author melissam
 *
 */
public class ClusterFeatureVector extends Cluster {

	/** Serial UUID. */
	private static final long serialVersionUID = 8568584828041493998L;

	/** Number of elements in the cluster. */
	protected double size;
	
	/** Sum of squares of the values of each feature vector added to the micro-cluster. */
	//vector(CF2x)
	protected double[] sumOfSquaresOfValues;
	
	/** Linear sum of values of each feature vector added to the micro-cluster. */
	// vector(CF1x)
	protected double[] sumOfValues;	
	
	/**
	 * Constructs a cluster feature vector with the given center points.
	 * 
	 * @param center The center to assign.
	 */
	public ClusterFeatureVector(double[] center){

		this.size = 1;

		// nothing to add with
		sumOfValues	= center;

		// sum the values of center and populate sumOfSquares
		sumOfSquaresOfValues = new double[center.length];
		for (int i = 0; i<center.length; i++){
			sumOfSquaresOfValues[i] = Math.pow(center[i], 2);
		}
	}

	
	/**
	 * Adds a feature to this cluster.
	 * 
	 * @param featureVector The feature vector to add.
	 */
	public void addFeature(double[] featureVector){
		
		assert(featureVector.length == sumOfValues.length);
				
		for (int i = 0; i < sumOfValues.length; i++){
			// adjust sumOfValues
			sumOfValues[i] += featureVector[i];		
			// adjust sumOfSquares
			sumOfSquaresOfValues[i] += Math.pow(featureVector[i], 2);
		}
	
		size++;
		
	}
	
	
	/**
	 * Adds a feature to this cluster.
	 * 
	 * @param featureVector The feature vector to add.
	 */
	public void addFeature(ClusterFeatureVector other){
		
		// use CF additivity property
		this.size += other.getSize();
		
		for ( int i = 0; i < this.sumOfValues.length; i++ ) {
		    this.sumOfValues[i] += other.sumOfValues[i];
		    this.sumOfSquaresOfValues[i] += other.sumOfSquaresOfValues[i];
		}
		
	}

	
	/**
	 * Get the center of the micro-cluster from the sum of all feature vectors the cluster contains.
	 * 
	 * @return A vector of center values.
	 */
	@Override
	public double[] getCenter(){
		
		assert (size > 0);
		double center[] = new double[sumOfValues.length];
		for (int i = 0; i < sumOfValues.length; i++) {
			center[i] = sumOfValues[i] / size;
		}
		return center;
		
	}
	
	
	/**
	 * Override the {@link Cluster#getWeight()} for Cluster Feature vectors to return the number of 
	 * elements in this cluster.
	 */
	@Override
	public double getWeight(){
		return size;
	}
	
	/**
	 * Returns the number of features represented by this cluster.
	 * @return the number of features represented by this cluster.
	 */
	public double getSize() {
		return size;
	}

	/**
	 * Returns the sum of squares.
	 * @return The sum of squares.
	 */
	public double[] getSumOfSquaresOfValues() {
		return sumOfSquaresOfValues;
	}

	
	/**
	 * Returns the sum of values. 
	 * @return The sum of values.
	 */
	public double[] getSumOfValues() {
		return sumOfValues;
	}
	
}
