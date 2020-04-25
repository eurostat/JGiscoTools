/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.rail;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opengis.filter.Filter;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.graph.GraphBuilder;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

/**
 * @author julien Gaffuri
 *
 */
public class MainRailwayQuality {
	public final static Logger LOGGER = LogManager.getLogger(MainRailwayQuality.class.getName());

	public static void main(String[] args) throws Exception {

		LOGGER.info("Load input sections");
		String basePath = "/home/juju/Bureau/gisco_rail/";
		String inFile = basePath+"out/EM/RailwayLinkEM.shp";
		//String inFile = basePath+"out/quality/railway.shp";
		Filter fil = null; //CQL.toFilter( "CNTR = 'ES'" );
		Collection<Feature> secs = GeoData.getFeatures(inFile, null, fil);

		//check identifier
		//HashMap<String, Integer> out = FeatureUtil.checkIdentfier(secs, "id");
		//System.out.println(out);

		for(Feature f : secs) f.setID( f.getAttribute("id").toString() );
		LOGGER.info(secs.size()+" sections - " + FeatureUtil.getVerticesNumber(secs)+" vertices.");

		LOGGER.info("Quality fix");
		secs = GraphBuilder.qualityFixForSections(secs);

		//LOGGER.info("Check section intersection");
		//GraphBuilder.checkSectionsIntersection(secs);

		//g = GraphBuilder.buildFromLinearFeaturesPlanar(secs, true);
		//System.out.println("ok!!!");

		LOGGER.info("Save - nb=" + secs.size());
		GeoData.save(secs, basePath+"out/quality/railway.shp", GeoData.getCRS(inFile));

		LOGGER.info("End");
	}

}
