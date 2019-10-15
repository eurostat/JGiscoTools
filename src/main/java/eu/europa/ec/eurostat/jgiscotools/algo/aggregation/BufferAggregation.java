/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.algo.aggregation;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.buffer.BufferParameters;

import eu.europa.ec.eurostat.eurogeostat.algo.polygon.HolesDeletion;
import eu.europa.ec.eurostat.jgiscotools.algo.base.Closure;
import eu.europa.ec.eurostat.jgiscotools.algo.base.Union;
import eu.europa.ec.eurostat.jgiscotools.algo.line.DouglasPeuckerRamerFilter;

/**
 * 
 * Aggregation of a set of geometry buffers
 * 
 * @author julien Gaffuri
 *
 */
public class BufferAggregation{
	public final static Logger LOGGER = Logger.getLogger(BufferAggregation.class.getName());

	private double bufferDist;
	private double closureDist;
	private int qSegs;
	private double dPThreshold;
	private boolean withHoleDeletion;

	public BufferAggregation(double bufferDist, double closureDist, int qSegs, double dPThreshold, boolean withHoleDeletion) {
		super();
		this.bufferDist = bufferDist;
		this.closureDist = closureDist;
		this.qSegs = qSegs;
		this.dPThreshold = dPThreshold;
		this.withHoleDeletion = withHoleDeletion;
	}

	public Geometry aggregateGeometries(Collection<Geometry> geoms){

		if(LOGGER.isTraceEnabled()) LOGGER.debug("Compute buffers. Number of geometries: "+geoms.size());
		ArrayList<Geometry> buffs = new ArrayList<Geometry>();
		for(Geometry geom : geoms)
			buffs.add( geom.buffer(bufferDist, qSegs) );

		if(LOGGER.isTraceEnabled()) LOGGER.debug("Union of buffers");
		Geometry out = Union.getPolygonUnion(buffs);
		buffs.clear();

		if(dPThreshold>0) {
			if(LOGGER.isTraceEnabled()) LOGGER.debug("First filtering");
			out = DouglasPeuckerRamerFilter.get(out, dPThreshold);
		}
		if(closureDist>0) {
			if(LOGGER.isTraceEnabled()) LOGGER.debug("Closure");
			out = Closure.get(out, closureDist, qSegs, BufferParameters.CAP_ROUND );
		}
		if(dPThreshold>0) {
			if(LOGGER.isTraceEnabled()) LOGGER.debug("Second filtering");
			out = DouglasPeuckerRamerFilter.get(out, dPThreshold);
		}
		out = out.buffer(0);

		if(withHoleDeletion) {
			if(LOGGER.isTraceEnabled()) LOGGER.debug("Holes deletion");
			if (out instanceof Polygon) out = HolesDeletion.get((Polygon)out);
			else if (out instanceof MultiPolygon) out = HolesDeletion.get((MultiPolygon)out);
			else return null;
		}

		return out;
	}

	/*
	private static Feature aggregate(Feature f1, Feature f2, int z, double dist) {
		Feature fag = new Feature();

		Geometry g1 = f1.getGeom(z);
		Geometry g2 = f2.getGeom(z);

		if(g1.getArea()==0) g1=g1.buffer(dist);
		if(g2.getArea()==0) g2=g1.buffer(dist);
		Geometry g = (g1.buffer(dist).union(g2.buffer(dist))).buffer(-dist);
		g = DouglasPeuckerRamerFilter.get(g, dist);

		fag.setGeom(g);
		fag.setGeom(g,z);

		//merge component's lists
		fag.components = new ArrayList<Feature>();
		if(f1.components==null) fag.components.add(f1); else fag.components.addAll(f1.components);
		if(f2.components==null) fag.components.add(f2); else fag.components.addAll(f2.components);

		fag.desc = fag.components.size() + " objects.";

		return fag;
	}*/
}
