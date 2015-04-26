package net.melissam.powerlog.utils;

public class MathUtils {

	// The inverse error function of x
	// Needed for calculating the arrival timestamp of a specific percentile
	public static double inverseError(double x) {

		double z = Math.sqrt(Math.PI) * x;

		double ief = (z) / 2;

		double z2 = z * z;
		double zProd = z * z2; // z^3

		ief += (1.0 / 24) * zProd;
		zProd *= z2;  // z^5

		ief += (7.0 / 960) * zProd;
		zProd *= z2;  // z^7

		ief += (127 * zProd) / 80640;
		zProd *= z2;  // z^9

		ief += (4369 * zProd) / 11612160;
		zProd *= z2;  // z^11

		ief += (34807 * zProd) / 364953600;
		zProd *= z2;  // z^13

		ief += (20036983 * zProd) / 797058662400d;

		return ief;

	}

	/**
	 * Calculates the Euclidean distance of a point to a specific center
	 * 
	 * @param p1	The point to calculate the distance of.
	 * @param p2	Where to calculate the center to.
	 * @return The distance between the 2 points.
	 */
	public static double calculateDistance(double[] p1, double[] p2){
		
		double distance = 0.0;
		for (int i = 0; i < p1.length; i++) {
			double d = p1[i] - p2[i];
			distance += d * d;
		}
		
		return Math.sqrt(distance);
		
	}
}
