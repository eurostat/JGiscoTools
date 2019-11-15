/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.grid;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.strtree.STRtree;

import eu.europa.ec.eurostat.jgiscotools.algo.base.Union;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.util.Util;

/**
 * A number of functions on grids.
 * 
 * @author julien Gaffuri
 *
 */
public class GridUtil {
	static Logger logger = Logger.getLogger(GridUtil.class.getName());

	/**
	 * Assign region codes to grid cells. These regions could be countries or NUTS regions.
	 * If a grid cell intersects or is nearby the geometry of a region, then an attribute of the cell is assigned with this region code.
	 * For cells that are to be assigned to several regions, several region codes are assigned.
	 * 
	 * @param cells
	 * @param cellRegionAttribute
	 * @param regions
	 * @param toleranceDistance
	 * @param regionCodeAttribute
	 */
	public static void assignRegionCode(Collection<Feature> cells, String cellRegionAttribute, Collection<Feature> regions, double toleranceDistance, String regionCodeAttribute) {

		//initialise cell region attribute
		for(Feature cell : cells)
			cell.setAttribute(cellRegionAttribute, "");

		//index cells
		STRtree index = new STRtree();
		for(Feature cell : cells)
			index.insert(cell.getDefaultGeometry().getEnvelopeInternal(), cell);

		for(Feature reg : regions) {

			//get region cover and code
			Geometry regCover = reg.getDefaultGeometry();
			if(toleranceDistance != 0 ) regCover = regCover.buffer(toleranceDistance);
			String regCode = reg.getAttribute(regionCodeAttribute).toString();

			//get region envelope
			Envelope regCoverEnv = regCover.getEnvelopeInternal();

			//get grid cells around region envelope
			for(Object cell_ : index.query(regCoverEnv)) {
				Feature cell = (Feature)cell_;
				Geometry cellGeom = cell.getDefaultGeometry();

				if( ! regCoverEnv.intersects(cellGeom.getEnvelopeInternal()) ) continue;
				if( ! regCover.intersects(cellGeom) ) continue;

				String att = cell.getAttribute(cellRegionAttribute).toString();
				if("".equals(att))
					cell.setAttribute(cellRegionAttribute, regCode);
				else
					cell.setAttribute(cellRegionAttribute, att+"-"+regCode);
			}
		}

	}


	/**
	 * Remove cells which are not assigned to any region,
	 * that is the ones with attribute 'cellRegionAttribute' null or set to "".
	 * 
	 * @param cells
	 * @param cellRegionAttribute
	 */
	public static void filterCellsWithoutRegion(Collection<Feature> cells, String cellRegionAttribute) {
		Collection<Feature> cellsToRemove = new ArrayList<Feature>();
		for(Feature cell : cells) {
			Object cellReg = cell.getAttribute(cellRegionAttribute);
			if(cellReg==null || "".equals(cellReg.toString()))
				cellsToRemove.add(cell);
		}
		cells.removeAll(cellsToRemove);
	}


	/**
	 * Compute for each cell the proportion of its area which is land area.
	 * The value is stored as a new attribute for each cell. This value is a percentage.
	 * 
	 * @param cells
	 * @param cellLandPropAttribute
	 * @param landGeometry
	 * @param decimalNB The number of decimal places to keep for the percentage
	 */
	public static void assignLandProportion(Collection<Feature> cells, String cellLandPropAttribute, SpatialIndex landGeometries, int decimalNB) {

		//compute cell area once
		double cellArea = cells.iterator().next().getDefaultGeometry().getArea();

		for(Feature cell : cells) {
			logger.debug(cell.getAttribute("GRD_ID"));

			//compute land part
			Geometry landCellGeom = getLandCellGeometry(cell, landGeometries);

			//compute land proportion
			double prop = 100.0 * landCellGeom.getArea() / cellArea;
			prop = Util.round(prop, decimalNB);
			cell.setAttribute(cellLandPropAttribute, prop);
		}

	}


	/**
	 * Compute land geometry of a grid cell
	 * 
	 * @param cell
	 * @param landGeometries
	 * @return
	 */
	public static Geometry getLandCellGeometry(Feature cell, SpatialIndex landGeometries) {

		//get cell geometry
		Geometry cellGeom = cell.getDefaultGeometry();

		//list of land patches
		Collection<Geometry> landCellGeoms = new ArrayList<>();

		for(Object g_ : landGeometries.query(cellGeom.getEnvelopeInternal())) {
			Geometry g = (Geometry) g_;

			//compute intersection on cell geometry with land geometry
			if(!g.intersects(cellGeom)) continue;
			Geometry inter = g.intersection(cellGeom);
			if(inter == null || inter.isEmpty()) continue;

			//add intersection to cell land geometry
			landCellGeoms.add(inter);
		}

		//compute union
		Geometry landCellGeom = Union.polygonsUnionAll(landCellGeoms);

		if(landCellGeom == null) return cellGeom.getFactory().createPolygon();
		return landCellGeom;
	}


}
