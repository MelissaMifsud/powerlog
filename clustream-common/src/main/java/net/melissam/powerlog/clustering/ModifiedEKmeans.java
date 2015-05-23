package net.melissam.powerlog.clustering;

import java.util.Arrays;

/**
 * Adapted from EKmeans: https://code.google.com/p/ekmeans/
 * - Fixed some NaN problems in Euclidean distance function.
 * - Added comments
 * 
 * @author pbelanger
 *
 */
public class ModifiedEKmeans {
	
	/** 
	 * Listener interface for adding functionality after each iteration. This helps with feedback
	 * during the kmeans process.
	 *  
	 * @author pbelanger
	 *
	 */
    public static interface Listener {

        void iteration(int iteration, int move);
    }

    /**
     * The distance function to use during kmeans can be customised by implementing this interface.
     * 
     * @author pbelanger
     *
     */
    public static interface DistanceFunction {

        double distance(double[] p1, double[] p2);
    }

    /**
     * Euclidean distance implementation.
     */
    public static final DistanceFunction EUCLIDEAN_DISTANCE_FUNCTION = new DistanceFunction() {

        @Override
        public double distance(double[] p1, double[] p2) {
            double s = 0;
            for (int d = 0; d < p1.length; d++) {            	
            	double diff = Math.abs(p1[d] - p2[d]);
                s += diff != 0 ? Math.pow(Math.abs(p1[d] - p2[d]), 2) : 0;
            }
            
            double d = s > 0 ? Math.sqrt(s) : 0;
            return d;
        }
    };

    /** Keep track of centroids. Represent partitions. */
    protected double[][] centroids;
    
    /** Points to be clustered. */
    protected double[][] points;
    
    protected int idealCount;
    
    /** Point distances to centroids. */
    protected double[][] distances;
    
    /** Which partition each point was assigned to. */
    protected int[] assignments;
    
    /** Changes in centroids. */
    protected boolean[] changes;
    
    /** Size of partitions. */
    protected int[] counts;
        
    protected boolean[] dones;
    
    /** Keeps track of how many iterations have been performed. */
    protected int iteration;
    
    /** Whether to form partitions with equal weight. */
    protected boolean equal;
    
    /** Which distance function to use. */
    protected DistanceFunction distanceFunction;
    
    /** Listener to notify after each iteration. */
    protected Listener listener;

    /**
     * Class constructor.
     * 
     * @param centroids	Initial seeds to use.
     * @param points	The points to be clustered.
     */
    public ModifiedEKmeans(double[][] centroids, double[][] points) {
        this.centroids = centroids;
        this.points = points;
        if (centroids.length > 0) {
            idealCount = points.length / centroids.length;
        } else {
            idealCount = 0;
        }
        distances = new double[centroids.length][points.length];
        assignments = new int[points.length];
        Arrays.fill(assignments, -1);
        changes = new boolean[centroids.length];
        Arrays.fill(changes, true);
        counts = new int[centroids.length];
        dones = new boolean[centroids.length];
        iteration = 128;
        equal = false;
        distanceFunction = EUCLIDEAN_DISTANCE_FUNCTION;
        listener = null;
    }

    /**
     * Returns the centroids (seeds) at any given time during the process.	
     * @return The centroids (seeds) at any given time during the process.
     */
    public double[][] getCentroids() {
        return centroids;
    }

    /**
     * Returns the points to be clustered.
     * @return The points to be clustered.
     */
    public double[][] getPoints() {
        return points;
    }

    /**
     * Current distances of points from centroids.
     * @return Distances of points from centroids.
     */
    public double[][] getDistances() {
        return distances;
    }

    /**
     * Returns which partition each point was put in to.
     * @return The partition each point was put in to.
     */
    public int[] getAssignments() {
        return assignments;
    }

    /**
     * Returns which partitions have changed in the last iteration.
     * @return The partitions have changed in the last iteration.
     */
    public boolean[] getChanges() {
        return changes;
    }

    /**
     * Returns the size of each partition.
     * @return The size of each partition.
     */
    public int[] getCounts() {
        return counts;
    }

    /**
     * Returns the number of iterations that have been performed.
     * @return The number of iterations that have been performed.
     */
    public int getIteration() {
        return iteration;
    }

    /**
     * Set the number of iterations that should be performed.
     * @param iteration The number of iterations that should be performed.
     */
    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    /**
     * Whether the class was set up to generate partitions with equal weight.
     * @return	Whether the class was set up to generate partitions with equal weight.
     */
    public boolean isEqual() {
        return equal;
    }

    /**
     * Set class to return partitions with equal weight or not.
     * @param equal Set class to return partitions with equal weight or not.
     */
    public void setEqual(boolean equal) {
        this.equal = equal;
    }

    /**
     * Return the distance function that is set for clustering.
     * @return The distance function that is set for clustering.
     */
    public DistanceFunction getDistanceFunction() {
        return distanceFunction;
    }

    /**
     * Sets which distance function should be used.
     * @param distanceFunction
     */
    public void setDistanceFunction(DistanceFunction distanceFunction) {
        this.distanceFunction = distanceFunction;
    }

    /**
     * Returns the listener.
     * @return The listener.
     */
    public Listener getListener() {
        return listener;
    }

    /**
     * Sets a listener to be notified after each iteration.
     * @param listener
     */
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    
    public void run() {
        calculateDistances();
        int move = makeAssignments();
        int i = 0;
        while (move > 0 && i++ < iteration) {
            if (points.length >= centroids.length) {
                move = fillEmptyCentroids();
//                calculateDistances();
            }
            moveCentroids();
            calculateDistances();
            move += makeAssignments();
            if (listener != null) {
                listener.iteration(i, move);
            }
        }
    }

    protected void calculateDistances() {
        for (int c = 0; c < centroids.length; c++) {
            if (!changes[c]) {
                continue;
            }
            double[] centroid = centroids[c];
            for (int p = 0; p < points.length; p++) {
                double[] point = points[p];
                distances[c][p] = distanceFunction.distance(centroid, point);
            }
            changes[c] = false;
        }
    }

    protected int makeAssignments() {
        int move = 0;
        Arrays.fill(counts, 0);
        for (int p = 0; p < points.length; p++) {
            int nc = nearestCentroid(p);
            if (nc == -1) {
                continue;
            }
            if (assignments[p] != nc) {
                if (assignments[p] != -1) {
                    changes[assignments[p]] = true;
                }
                changes[nc] = true;
                assignments[p] = nc;
                move++;
            }
            counts[nc]++;
            if (equal && counts[nc] > idealCount) {
                move += remakeAssignments(nc);
            }
        }
        return move;
    }

    protected int remakeAssignments(int cc) {
        int move = 0;
        double md = Double.MAX_VALUE;
        int nc = -1;
        int np = -1;
        for (int p = 0; p < points.length; p++) {
            if (assignments[p] != cc) {
                continue;
            }
            for (int c = 0; c < centroids.length; c++) {
                if (c == cc || dones[c]) {
                    continue;
                }
                double d = distances[c][p];
                if (d < md) {
                    md = d;
                    nc = c;
                    np = p;
                }
            }
        }
        if (nc != -1 && np != -1) {
            if (assignments[np] != nc) {
                if (assignments[np] != -1) {
                    changes[assignments[np]] = true;
                }
                changes[nc] = true;
                assignments[np] = nc;
                move++;
            }
            counts[cc]--;
            counts[nc]++;
            if (counts[nc] > idealCount) {
                dones[cc] = true;
                move += remakeAssignments(nc);
                dones[cc] = false;
            }
        }
        return move;
    }

    protected int nearestCentroid(int p) {
        double md = Double.MAX_VALUE;
        int nc = -1;
        for (int c = 0; c < centroids.length; c++) {
            double d = distances[c][p];
            if (d < md) {
                md = d;
                nc = c;
            }
        }
        return nc;
    }

    protected int nearestPoint(int inc, int fromc) {
        double md = Double.MAX_VALUE;
        int np = -1;
        for (int p = 0; p < points.length; p++) {
            if (assignments[p] != inc) {
                continue;
            }
            double d = distances[fromc][p];
            if (d < md) {
                md = d;
                np = p;
            }
        }
        return np;
    }

    protected int largestCentroid(int except) {
        int lc = -1;
        int mc = 0;
        for (int c = 0; c < centroids.length; c++) {
            if (c == except) {
                continue;
            }
            if (counts[c] > mc) {
                lc = c;
            }
        }
        return lc;
    }

    protected int fillEmptyCentroids() {
        int move = 0;
        for (int c = 0; c < centroids.length; c++) {
            if (counts[c] == 0) {
                int lc = largestCentroid(c);
                int np = nearestPoint(lc, c);
                assignments[np] = c;
                counts[c]++;
                counts[lc]--;
                changes[c] = true;
                changes[lc] = true;
                move++;
            }
        }
        return move;
    }

    protected void moveCentroids() {
        for (int c = 0; c < centroids.length; c++) {
            if (!changes[c]) {
                continue;
            }
            double[] centroid = centroids[c];
            int n = 0;
            Arrays.fill(centroid, 0);
            for (int p = 0; p < points.length; p++) {
                if (assignments[p] != c) {
                    continue;
                }
                double[] point = points[p];
                n++;
                for (int d = 0; d < centroid.length; d++) {
                    centroid[d] += point[d];
                }
            }
            if (n > 0) {
                for (int d = 0; d < centroid.length; d++) {
                    centroid[d] /= n;
                }
            }
        }
    }
}
