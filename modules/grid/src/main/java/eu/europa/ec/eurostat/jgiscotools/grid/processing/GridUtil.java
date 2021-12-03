/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.grid.processing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;

import eu.europa.ec.eurostat.jgiscotools.algo.base.Union;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;

/**
 * A number of functions on grids.
 * 
 * @author julien Gaffuri
 *
 */
public class GridUtil {
	static Logger logger = LogManager.getLogger(GridUtil.class.getName());

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
	public static void assignRegionCode(Collection<Feature> cells, String cellRegionAttribute, Collection<Feature> regions, double toleranceDistance, String regionCodeAttribute, boolean parallel) {

		//initialise cell region attribute
		for(Feature cell : cells)
			cell.setAttribute(cellRegionAttribute, "");

		//index cells
		STRtree index = new STRtree();
		for(Feature cell : cells)
			index.insert(cell.getGeometry().getEnvelopeInternal(), cell);

		//get codes
		//for(Feature reg : regions) {
		Stream<Feature> st = regions.stream(); if(parallel) st = st.parallel();
		st.forEach(reg -> {

			//get region cover and code
			Geometry regCover = reg.getGeometry();
			if(toleranceDistance != 0 ) regCover = regCover.buffer(toleranceDistance);
			String regCode = reg.getAttribute(regionCodeAttribute).toString();

			//get region envelope
			Envelope regCoverEnv = regCover.getEnvelopeInternal();

			//get grid cells around region envelope
			for(Object cell_ : index.query(regCoverEnv)) {
				Feature cell = (Feature)cell_;
				Geometry cellGeom = cell.getGeometry();

				if( ! regCoverEnv.intersects(cellGeom.getEnvelopeInternal()) ) continue;
				if( ! regCover.intersects(cellGeom) ) continue;

				String att = cell.getAttribute(cellRegionAttribute).toString();
				if("".equals(att))
					cell.setAttribute(cellRegionAttribute, regCode);
				else
					cell.setAttribute(cellRegionAttribute, att+"-"+regCode);
			}
		});

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
	 * @param landGeometries
	 * @param inlandWaterGeometriesIndex
	 * @param decimalNB The number of decimal places to keep for the percentage
	 */
	public static void assignLandProportion(Collection<Feature> cells, String cellLandPropAttribute, SpatialIndex landGeometries, SpatialIndex inlandWaterGeometriesIndex, int decimalNB, boolean parallel) {

		//compute cell area once
		double cellArea = cells.iterator().next().getGeometry().getArea();

		Stream<Feature> st = cells.stream(); if(parallel) st = st.parallel();
		st.forEach(cell -> {
			logger.debug(cell.getAttribute("GRD_ID"));

			//compute land part
			Geometry landCellGeom = getLandCellGeometry(cell, landGeometries, inlandWaterGeometriesIndex);

			//compute land proportion
			double prop = 100.0 * landCellGeom.getArea() / cellArea;
			prop = round(prop, decimalNB);
			cell.setAttribute(cellLandPropAttribute, prop);
		});

	}


	/**
	 * Compute land geometry of a grid cell
	 * 
	 * @param cell
	 * @param landGeometries
	 * @return
	 */
	public static Geometry getLandCellGeometry(Feature cell, SpatialIndex landGeometries, SpatialIndex inlandWaterGeometriesIndex) {

		//get cell geometry
		Geometry cellGeom = cell.getGeometry();

		//list of land patches
		Collection<Geometry> patches = new ArrayList<>();

		for(Object g_ : landGeometries.query(cellGeom.getEnvelopeInternal())) {
			Geometry g = (Geometry) g_;

			//compute intersection on cell geometry with land geometry
			if(!g.intersects(cellGeom)) continue;
			Geometry inter = g.intersection(cellGeom);
			if(inter == null || inter.isEmpty()) continue;

			//add intersection to cell land geometry
			patches.add(inter);
		}

		//compute union
		Geometry landCellGeom = Union.polygonsUnionAll(patches);

		if(landCellGeom == null)
			return cellGeom.getFactory().createPolygon();
		if(landCellGeom.isEmpty())
			return landCellGeom;


		//remove inland water

		//get inland water areas intersecting land geometry
		patches.clear();
		for(Object g_ : inlandWaterGeometriesIndex.query(landCellGeom.getEnvelopeInternal())) {
			Geometry g = (Geometry) g_;
			if( ! g.getEnvelopeInternal().intersects(landCellGeom.getEnvelopeInternal()) ) continue;
			patches.add(g);
		}
		if( patches.size() > 0 ) {
			//compute inland water as union
			Geometry inlandWater = Union.polygonsUnionAll(patches);
			//compute geom difference
			landCellGeom = landCellGeom.difference( inlandWater );
		}


		if(landCellGeom == null) return cellGeom.getFactory().createPolygon();
		return landCellGeom;
	}




	/**
	 * For eache cell, compute
	 * 
	 * @param cells
	 * @param distanceAttribute
	 * @param linesInd
	 * @param decimalNB
	 */
	public static void assignDistanceToLines(Collection<Feature> cells, String distanceAttribute, STRtree linesInd, int decimalNB, boolean parallel) {

		//go through the list of cells
		Stream<Feature> st = cells.stream(); if(parallel) st = st.parallel();
		st.forEach(cell -> {

			//get the lines that are nearby the cell
			Envelope netEnv = cell.getGeometry().getEnvelopeInternal();
			Object[] candidateLines = linesInd.nearestNeighbour(netEnv, cell.getGeometry(), itemDist, 10);

			//find the closest line to the cell center and compute minimum distance
			double minDist = -1;
			for(Object line : candidateLines) {
				Geometry lineG = (Geometry)line;
				double dist = lineG.distance( cell.getGeometry().getCentroid() );
				if(minDist<0 || dist<minDist) minDist = dist;
			}

			//store the distance
			minDist = round(minDist, decimalNB);
			cell.setAttribute(distanceAttribute, minDist);
		});

	}
	private static ItemDistance itemDist = new ItemDistance() {
		@Override
		public double distance(ItemBoundable item1, ItemBoundable item2) {
			Geometry g1 = (Geometry) item1.getItem();
			Geometry g2 = (Geometry) item2.getItem();
			return g1.distance(g2);
		}
	};

	private static double round(double x, int decimalNB) {
		double pow = Math.pow(10, decimalNB);
		return ( (int)(x * pow + 0.5) ) / pow;
	}

}
