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
import org.opencarto.algo.base.Union;
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


		//create xkm grid
		String path = "C:/Users/gaffuju/Desktop/";

		Collection<Geometry> geoms = new ArrayList<Geometry>();
		for(Feature f : SHPUtil.loadSHP(path+"CNTR_RG_LAEA/CNTR_RG_01M_2016.shp").fs)
			geoms.add(f.getDefaultGeometry());

		logger.info("Make grid");
		gridSHP(new Coordinate(3540000,2890000), new Coordinate(4050000,3430000), 10000, 3035, geoms, 10000, path+"out/grid_OC.shp");

		logger.info("End");
	}






	public static void gridSHP(Coordinate cMin, Coordinate cMax, double res, int epsg, Collection<Geometry> geoms, double bufferDist, String outFile) {

		logger.debug("union");
		Geometry union = Union.getPolygonUnion(geoms);
		logger.debug("buffer");
		union = union.buffer(bufferDist, 4);

		/*
		logger.debug("index geom buffers");
		STRtree index = new STRtree();
		for(Geometry geom : geoms) {
			geom = geom.buffer(bufferDist, 4);
			index.insert(geom.getEnvelopeInternal(), geom);
		}*/

		logger.debug("create cells");
		Collection<Feature> cells = new ArrayList<Feature>();
		for(double x=cMin.x; x<cMax.x; x+=res)
			for(double y=cMin.y; y<cMax.y; y+=res) {

				//build cell geometry
				Polygon gridCellGeom = JTSGeomUtil.createPolygon( x,y, x+res,y, x+res,y+res, x,y+res, x,y );

				//check intersection
				if(!gridCellGeom.getEnvelopeInternal().intersects(union.getEnvelopeInternal())) continue;
				if(!gridCellGeom.intersects(union)) continue;

				//check if there is any geometry intersecting the grid cell
				/*boolean inter = false;
				Envelope env = gridCellGeom.getEnvelopeInternal();
				for(Object g_ : index.query(env)) {
					Geometry g = (Geometry) g_;
					if(!env.intersects(g.getEnvelopeInternal())) continue;
					if(!gridCellGeom.intersects(g)) continue;
					inter = true;
					break;
				}
				if(!inter) continue;*/

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
