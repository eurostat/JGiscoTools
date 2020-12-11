/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.routing;

import org.opengis.feature.simple.SimpleFeature;

public interface SpeedCalculator {
	double getSpeedKMPerHour(SimpleFeature sf);
}
