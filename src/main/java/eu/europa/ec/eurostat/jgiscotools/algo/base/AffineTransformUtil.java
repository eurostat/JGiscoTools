/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.algo.base;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.util.AffineTransformation;

/**
 * Few basic transformation functions.
 * 
 * @author julien Gaffuri
 *
 */
public class AffineTransformUtil {

	public static void applyScaling(Coordinate coord, Coordinate center, double coef){
		coord.x = center.x + coef*(coord.x-center.x);
		coord.y = center.y + coef*(coord.y-center.y);
	}

	public static AffineTransformation getStretchTransformation(Coordinate c, double angle, double scale ) {
		AffineTransformation at = new AffineTransformation();
		at.translate(-c.x, -c.y);
		at.rotate(-angle);
		at.scale(1.0, scale);
		at.rotate(angle);
		at.translate(c.x, c.y);
		return at;
	}

	public static void transform(AffineTransformation at, Coordinate[] cs) {
		for(Coordinate c : cs)
			at.transform(c, c);
	}

}
