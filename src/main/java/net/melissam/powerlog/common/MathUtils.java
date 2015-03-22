package net.melissam.powerlog.common;

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

}
