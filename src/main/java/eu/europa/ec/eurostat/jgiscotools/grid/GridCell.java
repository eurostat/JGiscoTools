/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.grid;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;

/**
 * A grid cell.
 * 
 * @author Julien Gaffuri
 *
 */
public class GridCell {
	//TODO extends Feature ?

	public GridCell(String id) {
		this.id = id;
	}

	public GridCell(String epsgCode, int resolution, int lowerLeftCornerPositionX, int lowerLeftCornerPositionY) {
		this.epsgCode = epsgCode;
		this.resolution = resolution;
		//TODO handle case of geographic coordinates
		this.lowerLeftCornerPosition = new Coordinate(lowerLeftCornerPositionX, lowerLeftCornerPositionY);
	}


	/**
	 * The cell id, as defined in INSPIRE coding system, see <a href="https://inspire.ec.europa.eu/id/document/tg/su">here</a>).
	 * Examples:
	 * - CRS3035RES200mN1453400E1452800
	 * - CRS3035RES100000mN5400000E1200000
	 */
	private String id = null;
	public String getId() {
		if(id==null)
			id = getGridCellId(epsgCode, resolution, lowerLeftCornerPosition);
		return id;
	}

	/**
	 * The EPSG code of the Coordinate Reference System of the grid cell.
	 * See the <a href="https://spatialreference.org/ref/epsg/">EPSG register.</a>
	 */
	private String epsgCode = null;
	public String getEpsgCode() {
		if(epsgCode == null) parseGridCellId();
		return epsgCode;
	}

	/**
	 * The grid cell resolution.
	 */
	private int resolution = -1;
	public int getResolution() {
		if(resolution == -1) parseGridCellId();
		return resolution;
	}

	/**
	 * The grid cell lower left position.
	 */
	private Coordinate lowerLeftCornerPosition = null;
	public int getLowerLeftCornerPositionX() {
		if(lowerLeftCornerPosition == null) parseGridCellId();
		return (int) lowerLeftCornerPosition.getX();
	}
	public int getLowerLeftCornerPositionY() {
		if(lowerLeftCornerPosition == null) parseGridCellId();
		return (int) lowerLeftCornerPosition.getY();
	}

	/**
	 * Parse the grid cell Id to get all information it contains.
	 * Examples:
	 * CRS3035RES200mN1453400E1452800
	 * CRS3035RES100000mN5400000E1200000
	 */
	private void parseGridCellId() {
		String id_ = id.replaceAll("CRS", "");
		String[] sp = id_.split("RES");
		epsgCode = sp[0];
		sp = sp[1].split("mN");
		resolution = Integer.parseInt(sp[0]);
		sp = sp[1].split("E");
		int n = Integer.parseInt(sp[0]);
		int e = Integer.parseInt(sp[1]);
		lowerLeftCornerPosition = new Coordinate(e,n);
	}


	/**
	 * @return The grid cell envelope.
	 */
	public Envelope getEnvelope() {
		int x = getLowerLeftCornerPositionX();
		int y = getLowerLeftCornerPositionY();
		return new Envelope(x, x+getResolution(), y, y+getResolution());
	}

	/**
	 * The type of grid cell geometry: The surface representation (a square) or its center point.
	 * 
	 * @author Julien Gaffuri
	 */
	public static enum GridCellGeometryType {SURFACE, CENTER_POINT};
	private GridCellGeometryType gridCellGeometryType = GridCellGeometryType.SURFACE;
	public GridCellGeometryType getGridCellGeometryType() { return gridCellGeometryType; }

	public Geometry geometry = null;
	public Geometry getGeometry() {
		if(geometry == null) {
			GridCellGeometryType gt = getGridCellGeometryType();
			GeometryFactory gf = new GeometryFactory();
			geometry = gt.equals(GridCellGeometryType.CENTER_POINT)? getPointGeometry(gf ) : getPolygonGeometry(gf);
		}
		return geometry;
	}

	/**
	 * Build grid cell geometry as a polygon.
	 * 
	 * @param gf
	 * @return
	 */
	public Polygon getPolygonGeometry(GeometryFactory gf) {
		int x = getLowerLeftCornerPositionX();
		int y = getLowerLeftCornerPositionY();
		int res = getResolution();
		Coordinate[] cs = new Coordinate[]{new Coordinate(x,y), new Coordinate(x+res,y), new Coordinate(x+res,y+res), new Coordinate(x,y+res), new Coordinate(x,y)};
		return gf.createPolygon(cs);
	}

	/**
	 * Build grid cell geometry as a point (its center point).
	 * 
	 * @param gf
	 * @return
	 */
	public Point getPointGeometry(GeometryFactory gf) {
		int x = getLowerLeftCornerPositionX();
		int y = getLowerLeftCornerPositionY();
		return gf.createPoint(new Coordinate(x+0.5*getResolution(), y+0.5*getResolution()));
	}

	/**
	 * Convert the grid cell into a feature.
	 * 
	 * @return
	 */
	public Feature toFeature() {
		Feature f = new Feature();
		f.setDefaultGeometry(this.getGeometry());
		f.setID(this.getId());
		f.setAttribute("GRD_ID", this.getId());
		f.setAttribute("X", this.getLowerLeftCornerPositionX());
		f.setAttribute("Y", this.getLowerLeftCornerPositionY());
		return f;
	}



	/**
	 * Build a cell code (according to INSPIRE coding system, see <a href="https://inspire.ec.europa.eu/id/document/tg/su">here</a>).
	 * This is valid only for a grids in a cartographic projection.
	 * Examples:
	 * - CRS3035RES200mN1453400E1452800
	 * - CRS3035RES100000mN5400000E1200000
	 * 
	 * @param epsgCode
	 * @param gridResolutionM
	 * @param lowerLeftCornerPosition NB: The coordinates are supposed to be integer
	 * @return
	 */
	public static String getGridCellId(String epsgCode, int gridResolutionM, Coordinate lowerLeftCornerPosition) {
		return 
				"CRS"+epsgCode
				+"RES"+Integer.toString((int)gridResolutionM)+"m"
				+"N"+Integer.toString((int)lowerLeftCornerPosition.getY())
				+"E"+Integer.toString((int)lowerLeftCornerPosition.getX())
				;
	}

	/**
	 * Get the cell of the upper grod cell, whose resolution is the specified one.
	 * This target resolution is expected to be a multiple of the grid cell resolution.
	 * 
	 * @param resolution
	 * @return
	 */
	public GridCell getUpperCell(int resolution) {
		int x = ((int)(getLowerLeftCornerPositionX() / resolution)) * resolution;
		int y = ((int)(getLowerLeftCornerPositionY() / resolution)) * resolution;
		return new GridCell(getEpsgCode(), resolution, x, y);
	}

}
