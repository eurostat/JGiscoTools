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


	public static void main(String[] args) throws Exception {
		logger.setLevel(Level.ALL);

		//example
		//https://krankenhausatlas.statistikportal.de/
		//resolution: 10 or 5 km?

		logger.info("Start");

		logger.info("Make grid");
		String path = "C:/Users/gaffuju/Desktop/";
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
				//TODO
				cell.setID( "CRS"+Integer.toString((int)epsg)+"RES"+Integer.toString((int)res)+x+y );
				cell.setAttribute("cellId", cell.getID());
				cells.add(cell);
			}

		logger.info("Save " + cells.size() + " cells");
		SHPUtil.saveSHP(cells, outFile, ProjectionUtil.getCRS(epsg));
	}


}
