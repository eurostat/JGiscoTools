/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.filter.text.cql2.CQL;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

/**
 * @author Julien Gaffuri
 *
 */
public class CLC2NUTSAggregation {
	private static Logger logger = LogManager.getLogger(CLC2NUTSAggregation.class.getName());

	/*
https://www.eea.europa.eu/data-and-maps/figures/corine-land-cover-1990-by-country/legend/image_large
Artificial areas
1**
Arable land and permanent crops
21*
22*
Pastures and heterogeneous agricultural areas
23*
24*
Forest
31*
Shrubs
32*
Open spaces with little or no vegetation
33*
Wetlands and water bodies
4**
5**

*/

	
	
	//use: -Xms2G -Xmx12G
	public static void main(String[] args) throws Throwable {
		logger.info("Start");

		String nutsFile = "/home/juju/Bureau/gisco/geodata/gisco/GISCO.NUTS_RG_100K_2021_3035.gpkg";
		String clcFile = "/home/juju/Bureau/gisco/clc/u2018_clc2018_v2020_20u1_geoPackage/DATA/U2018_CLC2018_V2020_20u1.gpkg";

		logger.info("Load NUTS level 3");
		ArrayList<Feature> nuts = GeoData.getFeatures(nutsFile, "NUTS_ID", CQL.toFilter("STAT_LEVL_CODE='3' AND SHAPE_AREA<0.01"));
		//[OBJECTID, SHAPE_LEN, STAT_LEVL_CODE, id, NUTS_ID, SHAPE_AREA]
		logger.info(nuts.size());

		for(Feature f : nuts) {
			//Feature f = nuts.get(0);
			String nutsId = f.getID();
			logger.info(nutsId);

			Geometry g = f.getGeometry();
			Envelope e = g.getEnvelopeInternal();

			//load clcs using spatial index
			String filStr = "NOT(Code_18='523') AND BBOX(Shape,"+e.getMinX()+","+e.getMinY()+","+e.getMaxX()+","+e.getMaxY()+")";
			ArrayList<Feature> clcs = GeoData.getFeatures(clcFile, "U2018_CLC2018_V2020_20u1", "ID", CQL.toFilter(filStr));
			//logger.info(clc.size());

			for(Feature clc : clcs) {
				if(! e.intersects(clc.getGeometry().getEnvelopeInternal()))
					continue;
				//compute intersection
				Geometry inter = clc.getGeometry().intersection(g);
				double area = inter.getArea();
				if(area<=0) continue;
				String code = clc.getAttribute("Code_18").toString();

				logger.info("   "+code+"   "+area);
			}

		}

		logger.info("Save CSV");
		
		
		logger.info("End");
	}

}
