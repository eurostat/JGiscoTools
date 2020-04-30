package eu.europa.ec.eurostat.jgiscotools.algo.deformationgael;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

public class GAELDecomposers {
	private static Logger logger = Logger.getLogger(GAELDecomposers.class.getName());

	public static SMSinglePoint decompose(Collection<GAELPoint> ps, Point pt) {
		GAELPoint gp = new GAELPoint(pt.getCoordinate());
		ps.add(gp);
		return new SMSinglePoint(gp);
	}

	public static void decomposeLimit(Polygon poly, double resolution, Collection<GAELPoint> ps, Collection<SMSegment> segs, boolean buildAngles, Collection<SMAngle> as) {
		decomposerDPL(poly.getExteriorRing().getCoordinates(), resolution, ps, segs, buildAngles, as);
		for (int i = 0; i < poly.getNumInteriorRing(); i++)
			decomposerDPL(poly.getInteriorRingN(i).getCoordinates(), resolution, ps, segs, buildAngles, as);
	}

	public static void decompose(LineString line, double resolution, Collection<GAELPoint> ps, Collection<SMSegment> segs, boolean buildAngles, Collection<SMAngle> as) {
		decomposerDPL(line.getCoordinates(), resolution, ps, segs, buildAngles, as);
	}

	public static Collection<SMSinglePoint> createSinglePoints(Collection<GAELPoint> ps){
		ArrayList<SMSinglePoint> sps = new ArrayList<SMSinglePoint>();
		for (GAELPoint p : ps) sps.add(new SMSinglePoint(p));
		return sps;
	}


	private static void decomposerDPL(Coordinate[] coords, double resolution, Collection<GAELPoint> ps, Collection<SMSegment> segs, boolean buildAngles, Collection<SMAngle> as) {
		int nb = coords.length;
		if(logger.isLoggable(Level.FINEST)) logger.log(Level.FINEST, "point nb=" + nb);

		//if there are less than 2 points, there is a problem
		if (nb < 2) {
			logger.severe("Error when decomposing. Coordinates list must have more than 2 points.");
			logger.severe(coords.toString());
			return;
		}

		//create the two first points and their segment
		GAELPoint p0 = new GAELPoint(coords[0]);
		ps.add(p0);
		GAELPoint p1 = new GAELPoint(coords[1]);
		ps.add(p1);
		segs.add(new SMSegment(p0, p1));

		//store the two first points (usefull at the end, for angle construction)
		GAELPoint p0_ = p0;
		GAELPoint p1_ = p1;

		GAELPoint p2 = null;
		for (int i=2; i<nb-1; i++) {

			//build point
			if(logger.isLoggable(Level.FINEST)) logger.log(Level.FINEST, "(" + coords[i].x + ", " + coords[i].y + ")");
			p2 = new GAELPoint(coords[i]);
			ps.add(p2);

			//build segment
			segs.add(new SMSegment(p1, p2));

			//build angle (if needed)
			if (buildAngles) as.add( new SMAngle(p0, p1, p2) );

			//next
			p0 = p1;
			p1 = p2;
		}

		//test closure
		boolean closed;
		if(coords[0].distance(coords[nb-1]) <= resolution) closed = true; else closed = false;

		if (closed) {
			//build the last segment to close the ring
			segs.add(new SMSegment(p1, p0_));
			//build the two last angles (if needed)
			if (buildAngles) {
				as.add( new SMAngle(p0, p1, p0_) );
				as.add( new SMAngle(p1, p0_, p1_) );
			}
			//possible link between last coordinate and first point agent
			if(coords[0] != coords[nb-1]) p0_.getCoordinates().add( coords[nb-1] );
		} else {
			//build the last point
			if(logger.isLoggable(Level.FINEST)) logger.log(Level.FINEST, "(" + coords[nb-1].x + ", " + coords[nb-1].y + ")");
			p2 = new GAELPoint(coords[nb-1]);
			ps.add(p2);

			//build the last segment
			segs.add(new SMSegment(p1, p2));

			//build the last angle (if needed)
			if (buildAngles) as.add( new SMAngle(p0, p1, p2) );
		}
	}
}
