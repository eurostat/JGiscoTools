/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.algo.distances;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.distance.DistanceOp;

/**
 * 
 * Compute the Hausdorff distance between two geometries.
 * @see <a href="https://en.wikipedia.org/wiki/Hausdorff_distance">https://en.wikipedia.org/wiki/Hausdorff_distance</a>
 * 
 * @author julien Gaffuri
 *
 */
public class HausdorffDistance {
	private final static Logger LOGGER = Logger.getLogger(HausdorffDistance.class.getName());

	//the input geometries
	private Geometry g0, g1;
	public Geometry getGeom0() { return g0; }
	public Geometry getGeom1() { return g1; }

	public HausdorffDistance(Geometry g0, Geometry g1) {
		this.g0 = g0;
		this.g1 = g1;
	}

	private double distance = Double.NaN;
	/**
	 * @return The hausdorff distance @see <a href="https://en.wikipedia.org/wiki/Hausdorff_distance">https://en.wikipedia.org/wiki/Hausdorff_distance</a>
	 */
	public double getDistance() {
		if(Double.isNaN(this.distance)) compute();
		return this.distance;
	}

	Coordinate c0 = null, c1 = null;
	/**
	 * @return The coordinate of the first geometry where the hausdorff distance is reached.
	 */
	public Coordinate getC0() {
		if(c0 == null) compute();
		return this.c0;
	}
	/**
	 * @return The coordinate of the second geometry where the hausdorff distance is reached.
	 */
	public Coordinate getC1() {
		if(c1 == null) compute();
		return this.c1;
	}





	/**
	 * Compute the Haudorff distance: The max of both max/min distances.
	 */
	private void compute() {

		if(this.g0 == null || this.g1 == null) {
			LOGGER.warn("Could not compute Hausdorff distance with null geometry.");
			return;
		}
		if(this.g0.isEmpty() || this.g1.isEmpty()) {
			LOGGER.warn("Could not compute Hausdorff distance with empty geometry.");
			return;
		}

		//compute two parts
		DistanceOp dop01 = compute_(this.g0, this.g1);
		double d01 = dop01.distance();
		DistanceOp dop10 = compute_(this.g1, this.g0);
		double d10 = dop10.distance();

		//get the max and set result
		if(d01>d10) {
			this.distance = d01;
			Coordinate[] cs = dop01.nearestPoints();
			this.c0 = cs[0];
			this.c1 = cs[1];
		} else {
			this.distance = d10;
			Coordinate[] cs = dop10.nearestPoints();
			this.c0 = cs[1];
			this.c1 = cs[0];
		}
	}

	/**
	 * When moving on gA, computes all shortest distances to gB.
	 * Return the maximum of these shortest distances.
	 * 
	 * @param gA
	 * @param gB
	 * @return
	 */
	private static DistanceOp compute_(Geometry gA, Geometry gB) {
		DistanceOp dopMax = null;
		//go through gA vertices
		for(Coordinate cA : gA.getCoordinates()) {
			//find the shortest distance to gB
			DistanceOp dop = new DistanceOp(gB.getFactory().createPoint(cA), gB);
			if(dopMax == null || dop.distance() > dopMax.distance())
				dopMax = dop;
		}
		return dopMax;
	}


	@Override
	public String toString() {
		return "Dist="+getDistance()+" c0="+getC0()+" c1="+getC1();
	}

}
