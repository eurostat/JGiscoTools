/**
 * 
 */
package eu.ec.estat.geostat;

import eu.ec.estat.geostat.io.ShapeFile;

/**
 * @author julien Gaffuri
 *
 */
public class FRCommunesRectangleInter {

	public static void main(String[] args) {
		try {
			String folder = "C:/Users/gaffuju/C_data/fr/";


			//-Xms2G -Xmx6G
			StatisticalUnitsIntersectionMatrix.compute(
					"commune",
					new ShapeFile(folder+"commune.shp").getFeatureStore(),
					"INSEE_COM",
					"carreau",
					new ShapeFile(folder+"carreau.shp").getFeatureStore(),
					"id",
					folder
					);
		} catch (Exception e) { e.printStackTrace(); }
	}

}
