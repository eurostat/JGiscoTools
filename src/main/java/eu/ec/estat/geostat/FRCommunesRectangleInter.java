/**
 * 
 */
package eu.ec.estat.geostat;

import eu.ec.estat.geostat.io.ShapeFile;
import eu.ec.estat.java4eurostat.io.CSV;

/**
 * @author julien Gaffuri
 *
 */
public class FRCommunesRectangleInter {

	//-Xms2G -Xmx6G
	public static void main(String[] args) {
		try {
			String folder = "C:/Users/gaffuju/C_data/fr/";

			//compute matrix
			StatisticalUnitsIntersectionMatrix suInter = new StatisticalUnitsIntersectionMatrix(
					new ShapeFile(folder+"commune.shp").getFeatureStore(), "INSEE_COM",
					new ShapeFile(folder+"carreau.shp").getFeatureStore(), "id"
					)
					.compute();

			//save
			CSV.save(suInter.interSectionMatrix.selectDimValueEqualTo("type", "intersection_area").shrinkDims(), "intersection_area", folder, "intersection_matrix_commune_carreau.csv");

		} catch (Exception e) { e.printStackTrace(); }
	}

}
