/**
 * 
 */
package eu.ec.estat.geostat;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory2;

import eu.ec.estat.geostat.io.ShapeFile;

/**
 * @author julien Gaffuri
 *
 */
public class TourismUseCase {
	public static String BASE_PATH = "H:/geodata/";
	public static String NUTS_SHP = BASE_PATH + "gisco_stat_units/NUTS_2013_01M_SH/data/NUTS_RG_01M_2013.shp";

	public static void main(String[] args) throws Exception {

		//download/update data for tourism
		//EurobaseIO.update("H:/eurobase/", "tour_occ_nim", "tour_occ_nin2", "tour_occ_nin2d", "tour_occ_nin2c");

		//load tourism data
		/*
		StatsHypercube hc = EurostatTSV.load("H:/eurobase/tour_occ_nin2.tsv", new Selection.And(new Selection.DimValueEqualTo("unit","NR"),new Selection.DimValueEqualTo("nace_r2","I551-I553"),new Selection.DimValueEqualTo("indic_to","B006")));
		hc.delete("unit"); hc.delete("indic_to"); hc.delete("nace_r2");
		hc.printInfo();
		 */

		//load NUTS regions
		ShapeFile shpFileNUTS = new ShapeFile(NUTS_SHP);
		FeatureIterator<SimpleFeature> it = shpFileNUTS.getFeatures(CQL.toFilter("STAT_LEVL_ = 3"));
		while (it.hasNext()) {
			SimpleFeature f = it.next();
			System.out.println(f.getAttribute("STAT_LEVL_"));
		}
		it.close();



		//produce map
		//http://docs.geotools.org/latest/userguide/library/render/gtrenderer.html
		//http://gis.stackexchange.com/questions/123903/how-to-create-a-map-and-save-it-to-an-image-with-geotools


		//load POIs from postgis



		//make dasymetric disaggregation



		//compute validation figures

		//show results on maps

	}

}
