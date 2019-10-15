/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.algo.polygon;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.buffer.BufferOp;

/**
 * 
 * @author julien Gaffuri
 *
 */
public class HolesDeletion {

	public static Polygon get(Polygon poly, GeometryFactory gf){
		return gf.createPolygon((LinearRing)poly.getExteriorRing(), null);
	}

	public static Polygon get(Polygon poly){
		return get(poly, poly.getFactory());
	}

	public static MultiPolygon get(MultiPolygon mp, GeometryFactory gf){
		Polygon[] ps = new Polygon[mp.getNumGeometries()];
		for(int i=0; i<mp.getNumGeometries(); i++) ps[i] = get((Polygon)mp.getGeometryN(i));
		return (MultiPolygon)BufferOp.bufferOp(gf.createMultiPolygon(ps), 0);
	}

	public static MultiPolygon get(MultiPolygon mp){
		return get(mp, mp.getFactory());
	}
}
