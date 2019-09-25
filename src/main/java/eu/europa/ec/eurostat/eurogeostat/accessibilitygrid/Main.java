/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.accessibilitygrid;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opencarto.io.SHPUtil;

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
		EuroGridSHPBuilder.gridSHP(new Coordinate(500000,140000), new Coordinate(8190000,6030000), 10000, 3035, mask, 10000, path+"out/grid_10km.shp");
		EuroGridSHPBuilder.gridSHP(new Coordinate(500000,140000), new Coordinate(8190000,6030000), 5000, 3035, mask, 10000, path+"out/grid_5km.shp");

		logger.info("End");
	}

}
