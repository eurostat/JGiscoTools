/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.accessibilitygrid;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.opencarto.datamodel.Feature;
import org.opencarto.io.SHPUtil;
import org.opencarto.util.ProjectionUtil;

import eu.europa.ec.eurostat.eurogeostat.cntr.CountriesUtil;

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
		Geometry area = CountriesUtil.getEuropeMask();
		ArrayList<Feature> cnts = CountriesUtil.getEuropeanCountries();

		Collection<Feature> cells;

		logger.info("Make 10km grid...");
		cells = EuroGridBuilder.proceed(area, 10000, 3035, "CNTR_ID", cnts, 1000, "CNTR_ID");
		logger.info("Save " + cells.size() + " cells...");
		SHPUtil.saveSHP(cells, path+"out/grid_10km.shp", ProjectionUtil.getCRS(3035));

		logger.info("Make 5km grid...");
		cells = EuroGridBuilder.proceed(area, 5000, 3035, "CNTR_ID", cnts, 500, "CNTR_ID");
		logger.info("Save " + cells.size() + " cells...");
		SHPUtil.saveSHP(cells, path+"out/grid_5km.shp", ProjectionUtil.getCRS(3035));

		logger.info("End");
	}

}
