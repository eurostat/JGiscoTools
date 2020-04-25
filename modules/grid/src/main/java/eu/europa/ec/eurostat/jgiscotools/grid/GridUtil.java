/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.grid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;

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
	public static void assignRegionCode(Collection<Feature> cells, String cellRegionAttribute, Collection<Feature> regions, double toleranceDistance, String regionCodeAttribute) {

		//initialise cell region attribute
		for(Feature cell : cells)
			cell.setAttribute(cellRegionAttribute, "");

		//index cells
		STRtree index = new STRtree();
		for(Feature cell : cells)
			index.insert(cell.getGeometry().getEnvelopeInternal(), cell);

		//get codes
		for(Feature reg : regions) {

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
	 * @param landGeometries
	 * @param inlandWaterGeometriesIndex
	 * @param decimalNB The number of decimal places to keep for the percentage
	 */
	public static void assignLandProportion(Collection<Feature> cells, String cellLandPropAttribute, SpatialIndex landGeometries, SpatialIndex inlandWaterGeometriesIndex, int decimalNB) {

		//compute cell area once
		double cellArea = cells.iterator().next().getGeometry().getArea();

		for(Feature cell : cells) {
			logger.debug(cell.getAttribute("GRD_ID"));

			//compute land part
			Geometry landCellGeom = getLandCellGeometry(cell, landGeometries, inlandWaterGeometriesIndex);

			//compute land proportion
			double prop = 100.0 * landCellGeom.getArea() / cellArea;
			prop = round(prop, decimalNB);
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
		Geometry landCellGeom = polygonsUnionAll(patches);

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
			Geometry inlandWater = polygonsUnionAll(patches);
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
	public static void assignDistanceToLines(Collection<Feature> cells, String distanceAttribute, STRtree linesInd, int decimalNB) {

		//go through the list of cells
		for(Feature cell : cells) {

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
		}

	}
	private static ItemDistance itemDist = new ItemDistance() {
		@Override
		public double distance(ItemBoundable item1, ItemBoundable item2) {
			Geometry g1 = (Geometry) item1.getItem();
			Geometry g2 = (Geometry) item2.getItem();
			return g1.distance(g2);
		}
	};




	private static Geometry polygonsUnionAll(Collection<Geometry> polys) {
		Geometry union = null;
		try {
			if(logger.isTraceEnabled()) logger.trace("Try union with CascadedPolygonUnion");
			union = CascadedPolygonUnion.union(polys);
		} catch (Exception e) {
			try {
				if(logger.isTraceEnabled()) logger.trace("Try union with PolygonUnion");
				union = getPolygonUnion(polys);
			} catch (Exception e1) {
				try {
					if(logger.isTraceEnabled()) logger.trace("Try buffer(0)");
					GeometryCollection gc = new GeometryFactory().createGeometryCollection(polys.toArray(new Geometry[polys.size()]));
					union = gc.buffer(0);
				} catch (Exception e2) {
					if(logger.isTraceEnabled()) logger.trace("Try iterative union");
					for(Geometry poly : polys)
						union = union==null? poly : union.union(poly);
				}
			}
		}
		return union;
	}
	
	private static Geometry getPolygonUnion(Collection<Geometry> geoms) {
		return getPolygon( union_(geoms) );
	}

	private static <T extends Geometry> ArrayList<Geometry> union_(Collection<T> geoms) {
		ArrayList<Geometry> geoms_ = new ArrayList<Geometry>();
		geoms_.addAll(geoms);

		final int cellSize = 1 + (int)Math.sqrt(geoms_.size());

		Comparator<Geometry> comparator =  new Comparator<Geometry>(){
			public int compare(Geometry geom1, Geometry geom2) {
				if (geom1==null || geom2==null) return 0;
				Envelope env1 = geom1.getEnvelopeInternal();
				Envelope env2 = geom2.getEnvelopeInternal();
				double i1 = env1.getMinX() / cellSize + cellSize*( (int)env1.getMinY() / cellSize );
				double i2 = env2.getMinX() / cellSize + cellSize*( (int)env2.getMinY() / cellSize );
				return i1>=i2? 1 : i1<i2? -1 : 0;
			}
		};

		int i = 1;
		int nb = 1 + (int)( Math.log(geoms_.size()) / Math.log(4) );
		TreeSet<Geometry> treeSet;
		while (geoms_.size() > 1) {
			i++;
			if(logger.isTraceEnabled()) logger.trace("Union (" + (i-1) + "/" + nb + ")");
			//System.out.println( "Union (" + (i-1) + "/" + nb + ")" );
			treeSet = new TreeSet<Geometry>(comparator);
			treeSet.addAll(geoms_);
			geoms_ = union(treeSet, 4);
		}
		return geoms_;
	}
	

	private static ArrayList<Geometry> union(TreeSet<Geometry> treeSet, int groupSize) {
		ArrayList<Geometry> unions = new ArrayList<Geometry>();
		Geometry union = null;
		int i=0;
		for (Geometry geom : treeSet) {
			if ((union==null)||(i%groupSize==0)) union = geom;
			else {
				union = union.union(geom);
				if (groupSize-i%groupSize==1) unions.add(union);
			}
			i++;
			if(logger.isTraceEnabled()) logger.trace(" " + i + " - " + treeSet.size() + " geometries");
		}
		if (groupSize-i%groupSize!=0) unions.add(union);
		return unions;
	}

	private static Geometry getPolygon(ArrayList<Geometry> geoms_) {
		List<Polygon> polys = new ArrayList<Polygon>();
		for (Geometry geom : geoms_) {
			if (geom instanceof Polygon) polys.add((Polygon) geom);
			else if (geom instanceof MultiPolygon) {
				MultiPolygon mp = (MultiPolygon) geom;
				for (int k=0; k<mp.getNumGeometries(); k++)
					polys.add((Polygon)mp.getGeometryN(k));
			} else logger.error("Error in polygon union: geometry type not supported: " + geom.getGeometryType());
		}
		if (polys.size()==1) return polys.get(0);
		if (geoms_.isEmpty()) return new GeometryFactory().createGeometryCollection(new Geometry[0]);
		return geoms_.iterator().next().getFactory().createMultiPolygon(polys.toArray(new Polygon[0]));

	}

	private static double round(double x, int decimalNB) {
		double pow = Math.pow(10, decimalNB);
		return ( (int)(x * pow + 0.5) ) / pow;
	}

}
