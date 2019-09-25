/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.accessibilitygrid;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.strtree.STRtree;
import org.opencarto.datamodel.Feature;
import org.opencarto.util.JTSGeomUtil;

/**
 * @author julien Gaffuri
 *
 */
public class EuroGridBuilder {
	static Logger logger = Logger.getLogger(EuroGridBuilder.class.getName());

	public static Collection<Feature> procceed(Coordinate cMin, Coordinate cMax, double res, int epsg, Geometry mask, String cntStampAtt, Collection<Feature> countries, double cntBufferDist, String cntIdAtt) {

		if(logger.isDebugEnabled()) logger.debug("Build cells...");
		Collection<Feature> cells = EuroGridBuilder.makeGrid(cMin, cMax, res, epsg, mask);
		if(logger.isDebugEnabled()) logger.debug(cells.size() + " cells built");

		if(logger.isDebugEnabled()) logger.debug("Stamp country...");
		EuroGridBuilder.addCountryStamp(cells, cntStampAtt, countries, cntBufferDist, cntIdAtt);

		if(logger.isDebugEnabled()) logger.debug("Filtering...");
		EuroGridBuilder.filterCountryStamp(cells, cntStampAtt);
		if(logger.isDebugEnabled()) logger.debug(cells.size() + " cells left");
		return cells;
	}

	public static Collection<Feature> makeGrid(Coordinate cMin, Coordinate cMax, double res, int epsg, Geometry mask) {
		logger.debug("create cells");
		Collection<Feature> cells = new ArrayList<Feature>();
		for(double x=cMin.x; x<cMax.x; x+=res)
			for(double y=cMin.y; y<cMax.y; y+=res) {

				//build cell geometry
				Polygon gridCellGeom = JTSGeomUtil.createPolygon( x,y, x+res,y, x+res,y+res, x,y+res, x,y );

				if(mask != null) {
					//check intersection with mask
					if(!gridCellGeom.getEnvelopeInternal().intersects(mask.getEnvelopeInternal())) continue;
					if(!gridCellGeom.intersects(mask)) continue;
				}

				//build and keep the cell
				Feature cell = new Feature();
				cell.setDefaultGeometry(gridCellGeom);
				cell.setID( getGridCellId(epsg, res, new Coordinate(x,y)) );
				cell.setAttribute("cellId", cell.getID());
				cells.add(cell);
			}
		return cells;
	}


	//assign grid cells to countries
	public static void addCountryStamp(Collection<Feature> cells, String cntStampAtt, Collection<Feature> countries, double cntBufferDist, String cntIdAtt) {

		//initialise cell country stamp
		for(Feature cell : cells)
			cell.setAttribute(cntStampAtt, "");

		//index cells
		STRtree index = new STRtree();
		for(Feature cell : cells)
			index.insert(cell.getDefaultGeometry().getEnvelopeInternal(), cell);

		for(Feature cnt : countries) {
			//get cnt geometry and code
			Geometry cntGeom = cnt.getDefaultGeometry();
			String cntCode = cnt.getAttribute(cntIdAtt).toString();

			//apply buffer
			if(cntBufferDist >= 0)
				cntGeom = cntGeom.buffer(cntBufferDist);

			//get grid cells intersecting
			Envelope cntEnv = cntGeom.getEnvelopeInternal();
			for(Object cell_ : index.query(cntEnv)) {
				Feature cell = (Feature)cell_;
				if(!cntEnv.intersects(cell.getDefaultGeometry().getEnvelopeInternal())) continue;
				if(!cntGeom.intersects(cell.getDefaultGeometry())) continue;
				String csa = cell.getAttribute(cntStampAtt).toString();
				if("".equals(csa))
					cell.setAttribute(cntStampAtt, cntCode);
				else
					cell.setAttribute(cntStampAtt, csa+"-"+cntCode);
			}
		}

	}

	//remove cells which are not assigned to any country
	public static void filterCountryStamp(Collection<Feature> cells, String cntStampAtt) {
		Collection<Feature> toRemove = new ArrayList<Feature>();
		for(Feature cell : cells) {
			String cellCnt = cell.getAttribute(cntStampAtt).toString();
			if(cellCnt.equals("")) toRemove.add(cell);
		}
		cells.removeAll(toRemove);
	}


	/**
	 * Build a cell code (according to INSPIRE coding system).
	 * This is valid only for a grids in a cartographic projection.
	 * Examples:
	 * - CRS3035RES200mN1453400E1452800
	 * - CRS3035RES100000mN5400000E1200000
	 * 
	 * @param epsg
	 * @param resolution
	 * @param lowerLeftCornerPosition
	 * @return
	 */
	public static String getGridCellId(int epsg, double resolution, Coordinate lowerLeftCornerPosition) {
		return 
				"CRS"+Integer.toString((int)epsg)
				+"RES"+Integer.toString((int)resolution)+"m"
				+"N"+Integer.toString((int)lowerLeftCornerPosition.getX())
				+"E"+Integer.toString((int)lowerLeftCornerPosition.getY())
				;
	}

}
