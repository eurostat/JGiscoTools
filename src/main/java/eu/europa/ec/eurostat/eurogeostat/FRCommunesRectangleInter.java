/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat;

import eu.europa.ec.eurostat.eurogeostat.intersectionmatrix.StatisticalUnitsIntersectionMatrix;
import eu.europa.ec.eurostat.eurogeostat.io.ShapeFile;
import eu.europa.ec.eurostat.java4eurostat.io.CSV;

/**
 * @author julien Gaffuri
 *
 */
public class FRCommunesRectangleInter {

	//-Xms2G -Xmx6G
	public static void main(String[] args) {
		try {
			System.out.println("Start");

			String folder = "C:/Users/gaffuju/C_data/fr/";

			//compute matrix
			StatisticalUnitsIntersectionMatrix suInter = new StatisticalUnitsIntersectionMatrix(
					new ShapeFile(folder+"commune.shp", true).getFeatureSource(), "INSEE_COM", true,
					new ShapeFile(folder+"carreau.shp", true).getFeatureSource(), "id", true
					)
					.compute();

			//save
			CSV.save(suInter.interSectionMatrix.selectDimValueEqualTo("type", "intersection_area").shrinkDims(), "intersection_area", folder, "intersection_matrix_commune_carreau.csv");

			System.out.println("End");
		} catch (Exception e) { e.printStackTrace(); }
	}

}
