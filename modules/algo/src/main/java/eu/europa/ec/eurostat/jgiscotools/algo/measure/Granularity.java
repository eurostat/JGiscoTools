/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.algo.measure;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

/**
 * Granularity measure based on segments' length statistics
 * 
 * @author julien Gaffuri
 *
 */
public class Granularity {

	public static class GranularityMeasurement {
		public double min, max, average, averageBelow;
		public int nb, nbBelow;
	}


	public static GranularityMeasurement get(LineString ls, double goalResolution){
		GranularityMeasurement m = new GranularityMeasurement();

		m.nb = ls.getNumPoints()-1;
		m.nbBelow = 0;
		m.average = ls.getLength()/m.nb;
		m.averageBelow = 0;

		m.min = Double.MAX_VALUE; m.max = 0;
		Coordinate c0 = ls.getCoordinateN(0);
		for(int i=1; i<=m.nb; i++){
			Coordinate c1 = ls.getCoordinateN(i);
			double length = c1.distance(c0);

			m.min = Math.min(length, m.min);
			m.max = Math.max(length, m.max);

			if(length < goalResolution) {
				m.nbBelow++;
				m.averageBelow += length;
			}

			c0 = c1;
		}
		m.averageBelow /= m.nbBelow;

		return m;
	}

}
