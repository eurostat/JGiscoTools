/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.SpatialIndex;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.transfoengine.Constraint;

/**
 * @author julien Gaffuri
 *
 */
public class CUnitOverlap  extends Constraint<AUnit> {
	public final static Logger LOGGER = Logger.getLogger(CUnitOverlap.class.getName());

	private List<Overlap> overlaps;
	private SpatialIndex index;

	public CUnitOverlap(AUnit agent, SpatialIndex index) {
		super(agent);
		this.index = index;
	}

	@Override
	public void computeCurrentValue() {
		LOGGER.debug("CUnitNoOverlap "+getAgent().getObject().getID());

		overlaps = new ArrayList<Overlap>();

		//retrieve all units overlapping, with spatial index
		Geometry geom = getAgent().getObject().getDefaultGeometry();
		for(Feature unit : (List<Feature>)index.query(geom.getEnvelopeInternal())) {
			if(unit == getAgent().getObject()) continue;
			if(!geom.getEnvelopeInternal().intersects(unit.getDefaultGeometry().getEnvelopeInternal())) continue;

			//check overlap
			boolean overlap = false;
			try {
				overlap = geom.overlaps(unit.getDefaultGeometry());
			} catch (Exception e) {
				//overlaps.add(new Overlap(unit.id, null, -1, -1));
				continue;
			}
			if(!overlap) continue;

			Geometry inter = geom.intersection(unit.getDefaultGeometry());
			double interArea = inter.getArea();
			if(interArea == 0) continue;
			overlaps.add(new Overlap(unit.getID(), inter.getCentroid().getCoordinate(), interArea, 100.0*interArea/geom.getArea()));
		}
	}

	@Override
	public void computeSatisfaction() {
		//if(inters.size()!=0) System.out.println(getAgent().getObject().id + " " + inters.size());
		if(overlaps == null || overlaps.size()==0) satisfaction = 10;
		else satisfaction = 0;
	}

	public String getMessage(){
		StringBuffer sb = new StringBuffer(super.getMessage());
		for(Overlap overlap : overlaps)
			sb.append(",").append(overlap.id).append(",").append(overlap.position.toString().replace(",", " ")).append(",").append(overlap.area).append(",").append(overlap.percentage).append("%");
		return sb.toString();
	}


	public class Overlap {
		public Overlap(String id, Coordinate position, double area, double percentage) {
			this.id = id;
			this.position = position;
			this.area = area;
			this.percentage = percentage;
		}
		String id;
		Coordinate position;
		double area;
		double percentage;
	}

}
