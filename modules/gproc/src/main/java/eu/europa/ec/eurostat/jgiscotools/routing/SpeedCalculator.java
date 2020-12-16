/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.routing;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;

public interface SpeedCalculator {
	double getSpeedKMPerHour(Feature f);
}
