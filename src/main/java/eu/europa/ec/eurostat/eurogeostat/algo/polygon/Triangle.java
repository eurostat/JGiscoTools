/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.algo.polygon;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

/**
 * @author julien Gaffuri
 *
 */
public class Triangle {

	public static boolean is(LineString ls) {
		return ls.isClosed() && ls.getNumPoints()<=4 && ls.getFactory().createPolygon(ls.getCoordinates()).getArea()>0;
	}

	public static boolean is(Polygon p) {
		if(p.getNumInteriorRing()>0) return false;
		return p.getExteriorRing().getNumPoints()<=4;
	}

	public static int nb(MultiPolygon mp) {
		int nb=0;
		for(int i=0; i<mp.getNumGeometries(); i++)
			if(is((Polygon)mp.getGeometryN(i))) nb++;
		return nb;
	}

}
