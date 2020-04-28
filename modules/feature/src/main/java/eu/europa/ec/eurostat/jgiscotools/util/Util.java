
package eu.europa.ec.eurostat.jgiscotools.util;

/**
 * @author julien Gaffuri
 *
 */
public class Util {

	//round a double
	public static double round(double x, int decimalNB) {
		double pow = Math.pow(10, decimalNB);
		return ( (int)(x * pow + 0.5) ) / pow;
	}

}
