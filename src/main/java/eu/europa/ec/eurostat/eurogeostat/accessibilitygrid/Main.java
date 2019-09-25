/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.accessibilitygrid;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opencarto.datamodel.Feature;
import org.opencarto.io.SHPUtil;
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
		EuroGridBuilder.logger.setLevel(Level.ALL);

		String path = "C:/Users/gaffuju/Desktop/";
		Geometry mask = SHPUtil.loadSHP(path+"CNTR_RG_LAEA/Europe_RG_01M_2016_10km.shp").fs.iterator().next().getDefaultGeometry();
		ArrayList<Feature> cnts = SHPUtil.loadSHP(path+"CNTR_RG_LAEA/CNTR_RG_01M_2016.shp").fs;
		Envelope europeEnvelope = new Envelope(500000, 8190000, 140000, 6030000);

		Collection<Feature> cells;

		logger.info("Make 10km grid...");
		cells = EuroGridBuilder.procceed(europeEnvelope, 10000, 3035, mask, "CNTR_ID", cnts, 1000, "CNTR_ID");
		logger.info("Save " + cells.size() + " cells...");
		SHPUtil.saveSHP(cells, path+"out/grid_10km.shp", ProjectionUtil.getCRS(3035));

		logger.info("Make 5km grid...");
		cells = EuroGridBuilder.procceed(europeEnvelope, 5000, 3035, mask, "CNTR_ID", cnts, 500, "CNTR_ID");
		logger.info("Save " + cells.size() + " cells...");
		SHPUtil.saveSHP(cells, path+"out/grid_5km.shp", ProjectionUtil.getCRS(3035));

		logger.info("End");
	}

}
