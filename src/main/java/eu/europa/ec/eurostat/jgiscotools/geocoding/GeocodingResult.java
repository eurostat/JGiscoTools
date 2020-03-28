/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.geocoding;

import org.locationtech.jts.geom.Coordinate;

/**
 * @author julien Gaffuri
 *
 */
public class GeocodingResult {

	public Coordinate position;
	public String matching;
	public String confidence;
	//-1: unknown
	//1: good
	//2: medium
	//3: low
	public int quality = -1;
}
