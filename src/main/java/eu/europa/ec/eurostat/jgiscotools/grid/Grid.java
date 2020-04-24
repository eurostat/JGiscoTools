/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.grid;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.strtree.STRtree;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.JTSGeomUtil;
import eu.europa.ec.eurostat.jgiscotools.grid.GridCell.GridCellGeometryType;

/**
 * Build a grid.
 * The resolution, coordinate reference system, extent and cell geometry types can be defined by th users.
 * Both cartographic and geographical grids are supported.
 * 
 * @author julien Gaffuri
 *
 */
public class Grid {
	public static Logger logger = LogManager.getLogger(Grid.class.getName());

	/**
	 * The grid resolution (pixel size).
	 * NB: The unit of measure should be the same as the one of the Coordinate Reference System.
	 */
	private int resolution = 100000;
	public double getResolution() { return resolution; }
	public Grid setResolution(int resolution) {
		this.resolution = resolution;
		cells = null;
		return this;
	}

	/**
	 * The EPSG code of the Coordinate Reference System of the grid.
	 * See the <a href="https://spatialreference.org/ref/epsg/">EPSG register.</a>
	 */
	private String epsgCode = "3035";
	public String getEPSGCode() { return epsgCode; }
	public Grid setEPSGCode(String epsgCode) {
		this.epsgCode = epsgCode;
		cells = null;
		return this;
	}

	/**
	 * The geometries the grid should cover, taking into account also the 'toleranceDistance' parameter.
	 * NB: Of course, the geometry should be defined in the Coordinate Reference System of the grid.
	 */
	private Collection<Geometry> geometriesToCover;
	public Collection<Geometry> getGeometriesToCover() {
		if(geometriesToCover == null) {
			geometriesToCover = new ArrayList<Geometry>();
			geometriesToCover.add( JTSGeomUtil.getGeometry(new Envelope(0.0, 10000000.0, 0.0, 10000000.0), new GeometryFactory()) );
		}
		return geometriesToCover;
	}

	public Grid setGeometryToCover(Geometry geometryToCover) {
		geometriesToCover = new ArrayList<Geometry>();
		geometriesToCover.addAll( JTSGeomUtil.getGeometries(geometryToCover) );
		cells = null;
		return this;
	}
	public Grid setGeometryToCover(Envelope envelopeToCover) {
		return setGeometryToCover(JTSGeomUtil.getGeometry(envelopeToCover, new GeometryFactory()));
	}
	public Grid addGeometryToCover(Geometry geometryToCover) {
		if(geometriesToCover == null) geometriesToCover = new ArrayList<Geometry>();
		geometriesToCover.addAll( JTSGeomUtil.getGeometries(geometryToCover) );
		cells = null;
		return this;
	}
	public Grid addGeometryToCover(Collection<Geometry> gs) {
		for(Geometry g : gs)
			addGeometryToCover(g);
		return this;
	}

	/**
	 * All cells within this tolerance distance to 'geometryToCover' will be included in the grid.
	 * NB 1: The unit of measure should be the same as the one of the Coordinate Reference System.
	 * NB 2: This distance can be negative.
	 */
	private double toleranceDistance = 0.0;
	public double getToleranceDistance() { return toleranceDistance; }
	public Grid setToleranceDistance(double toleranceDistance) {
		this.toleranceDistance = toleranceDistance;
		cells = null;
		return this;
	}

	/**
	 * The grid cell geometry type.
	 * @see GridCellGeometryType
	 */
	private GridCellGeometryType gridCellGeometryType = GridCellGeometryType.SURFACE;
	public GridCellGeometryType getGridCellGeometryType() { return gridCellGeometryType; }
	public Grid setGridCellGeometryType(GridCellGeometryType geomType) {
		this.gridCellGeometryType = geomType;
		cells = null;
		return this;
	}

	/**
	 * The grid cells, as features.
	 */
	private Collection<Feature> cells = null;
	public Collection<Feature> getCells() {
		if(cells == null) buildCells();
		return cells;
	}



	/**
	 * Build the grid cells.
	 * 
	 * @return this object
	 */
	private Grid buildCells() {
		if(logger.isDebugEnabled()) logger.debug("Build grid cells...");
		GeometryFactory gf = getGeometriesToCover().iterator().next().getFactory();

		//get geometries to cover
		Collection<Geometry> geometriesToCoverBuff;
		if( toleranceDistance == 0 ) {
			geometriesToCoverBuff = getGeometriesToCover();
		} else {
			if(logger.isDebugEnabled()) logger.debug("   (make buffer...)");
			geometriesToCoverBuff = new ArrayList<>();
			for(Geometry g : getGeometriesToCover())
				geometriesToCoverBuff.add(g.buffer(toleranceDistance));
		}

		//TODO tile geometriesToCoverBuff to improve efficientcy ?

		if(logger.isDebugEnabled()) logger.debug("   Make spatial index from geometriesToCoverBuff...");
		SpatialIndex index = new STRtree();
		for(Geometry g : geometriesToCoverBuff) index.insert(g.getEnvelopeInternal(), g);

		if(logger.isDebugEnabled()) logger.debug("   Get envelope to cover...");
		Envelope envCovBuff = JTSGeomUtil.getEnvelopeInternal( geometriesToCoverBuff );
		envCovBuff = ensureGrid(envCovBuff, resolution);
		geometriesToCoverBuff = null;

		if(logger.isDebugEnabled()) logger.debug("   Build grid cells...");
		cells = new ArrayList<Feature>();
		for(int x = (int) envCovBuff.getMinX(); x<envCovBuff.getMaxX(); x += resolution)
			for(int y = (int) envCovBuff.getMinY(); y<envCovBuff.getMaxY(); y += resolution) {

				//build grid cell
				GridCell cell = new GridCell(epsgCode, resolution, x, y);

				//check intersection with envCovBuff
				if( ! envCovBuff.intersects(cell.getEnvelope()) ) continue;

				//get cell geometry
				Geometry gridCellGeom = cell.getPolygonGeometry(gf);
				//check intersection with geometryToCover
				if( ! JTSGeomUtil.intersects(index, gridCellGeom) ) continue;

				//build the cell
				cells.add(cell.toFeature());
			}
		if(logger.isDebugEnabled()) logger.debug(cells.size() + " cells built");
		return this;
	}

	private static Envelope ensureGrid(Envelope env, double res) {
		double xMin = env.getMinX() - env.getMinX()%res;
		double xMax = (1+(int)(env.getMaxX()/res))*res;
		double yMin = env.getMinY() - env.getMinY()%res;
		double yMax = (1+(int)(env.getMaxY()/res))*res;
		return new Envelope(xMin, xMax, yMin, yMax);
	}

}
