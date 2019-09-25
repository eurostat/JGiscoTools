/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.accessibilitygrid;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opencarto.datamodel.Feature;
import org.opencarto.io.SHPUtil;
import org.opencarto.util.JTSGeomUtil;
import org.opencarto.util.ProjectionUtil;

/**
 * @author julien Gaffuri
 *
 */
public class Main {
	private static Logger logger = Logger.getLogger(Main.class.getName());

	//example
	//https://krankenhausatlas.statistikportal.de/

	public static void main(String[] args) throws Exception {
		logger.info("Start");
		logger.setLevel(Level.ALL);

		String path = "C:/Users/gaffuju/Desktop/";

		logger.info("Make grid");
		Geometry mask = SHPUtil.loadSHP(path+"CNTR_RG_LAEA/Europe_RG_01M_2016_10km.shp").fs.iterator().next().getDefaultGeometry();
		gridSHP(new Coordinate(500000,140000), new Coordinate(8190000,6030000), 10000, 3035, mask, 10000, path+"out/grid_10km.shp");
		gridSHP(new Coordinate(500000,140000), new Coordinate(8190000,6030000), 5000, 3035, mask, 10000, path+"out/grid_5km.shp");

		logger.info("End");
	}



	public static void gridSHP(Coordinate cMin, Coordinate cMax, double res, int epsg, Geometry mask, double bufferDist, String outFile) {

		logger.debug("create cells");
		Collection<Feature> cells = new ArrayList<Feature>();
		for(double x=cMin.x; x<cMax.x; x+=res)
			for(double y=cMin.y; y<cMax.y; y+=res) {

				//build cell geometry
				Polygon gridCellGeom = JTSGeomUtil.createPolygon( x,y, x+res,y, x+res,y+res, x,y+res, x,y );

				//check intersection
				if(!gridCellGeom.getEnvelopeInternal().intersects(mask.getEnvelopeInternal())) continue;
				if(!gridCellGeom.intersects(mask)) continue;

				//build and keep the cell
				Feature cell = new Feature();
				cell.setDefaultGeometry(gridCellGeom);
				cell.setID( getGridCellId(epsg, res, new Coordinate(x,y)) );
				cell.setAttribute("cellId", cell.getID());
				cells.add(cell);
			}

		logger.info("Save " + cells.size() + " cells");
		SHPUtil.saveSHP(cells, outFile, ProjectionUtil.getCRS(epsg));
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
