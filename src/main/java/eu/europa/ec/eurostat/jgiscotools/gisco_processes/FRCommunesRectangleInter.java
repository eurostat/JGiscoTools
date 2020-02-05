/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import eu.europa.ec.eurostat.java4eurostat.io.CSV;
import eu.europa.ec.eurostat.jgiscotools.geostat.StatisticalUnitsIntersectionMatrix;
import eu.europa.ec.eurostat.jgiscotools.io.SHPUtil;

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
					SHPUtil.getFeatureSource(folder+"commune.shp", true), "INSEE_COM", true,
					SHPUtil.getFeatureSource(folder+"carreau.shp", true), "id", true
					//new ShapeFile(folder+"commune.shp", true).getFeatureSource(), "INSEE_COM", true,
					//new ShapeFile(folder+"carreau.shp", true).getFeatureSource(), "id", true
					)
					.compute();

			//save
			CSV.save(suInter.interSectionMatrix.selectDimValueEqualTo("type", "intersection_area").shrinkDimensions(), "intersection_area", folder, "intersection_matrix_commune_carreau.csv");

			System.out.println("End");
		} catch (Exception e) { e.printStackTrace(); }
	}

}
