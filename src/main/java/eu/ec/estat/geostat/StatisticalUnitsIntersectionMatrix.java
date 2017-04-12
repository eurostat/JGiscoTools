package eu.ec.estat.geostat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;

import org.geotools.data.shapefile.shp.ShapefileException;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import com.vividsolutions.jts.geom.Geometry;

import eu.ec.estat.java4eurostat.base.StatsHypercube;
import eu.ec.estat.java4eurostat.base.StatsIndex;
import eu.ec.estat.java4eurostat.io.CSV;
import eu.ec.estat.java4eurostat.io.DicUtil;

/**
 * @author julien Gaffuri
 *
 */
public class StatisticalUnitsIntersectionMatrix {

	/**
	 * Compute the intersection matrix between two statistical unit datasets of a same area of interest.
	 * This intersection matrix computes the share of a statistical unit which intersects another ones of another statistical units dataset.
	 * 
	 * NB: this operation commutes.
	 * 
	 * @param datasetName1 name of the first dataset (for labelling)
	 * @param shpFilePath1 path of the first dataset, as shapefile
	 * @param idField1 name of the first dataset id attribute
	 * @param datasetName2 name of the second dataset (for labelling)
	 * @param shpFilePath2 path of the second dataset, as shapefile. NB: for performence improvement, a spatial index should be created for this file.
	 * @param idField2 name of the second dataset id attribute
	 * @param outFolder the folder where output files are stored
	 * @throws ShapefileException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public static void compute(String datasetName1, SimpleFeatureStore data1, String idField1, String datasetName2, SimpleFeatureStore data2, String idField2, String outFolder) throws ShapefileException, MalformedURLException, IOException{
		//create out files
		BufferedWriter bw1from2 = createFile(outFolder+"matrix_"+datasetName1+"_from_"+datasetName2+".csv", true);
		bw1from2.write(datasetName1+","+datasetName2+",ratio,intersection_area"); bw1from2.newLine();
		BufferedWriter bw2from1 = createFile(outFolder+"matrix_"+datasetName2+"_from_"+datasetName1+".csv", true);
		bw2from1.write(datasetName2+","+datasetName1+",ratio,intersection_area"); bw2from1.newLine();

		//load shapefile 1
		int nb1 = data1.getFeatures().size();
		FeatureIterator<SimpleFeature> itSu1 = data1.getFeatures().features();

		//(pre)load shapefile 2
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

		//go through shapefile 1
		int counter = 1;
		while (itSu1.hasNext()) {
			SimpleFeature f1 = itSu1.next();
			String id1 = f1.getAttribute(idField1).toString();
			Geometry geom1 = (Geometry) f1.getDefaultGeometryProperty().getValue();
			double a1 = geom1.getArea();

			System.out.println(datasetName1+" - "+id1+" - ("+datasetName2+")" + " " + (counter++) + "/" + nb1 + " " + (Math.round(10000*counter/nb1))*0.01 + "%");

			//get all su2 intersecting the su1 (with spatial index)
			//Filter f = ff.bbox(ff.property("the_geom"), f1.getBounds());
			Filter f = ff.intersects(ff.property("the_geom"), ff.literal(geom1));
			FeatureIterator<SimpleFeature> itSu2 = data2.getFeatures(f).features();
			//System.out.println(" -> "+(data2.getFeatures(f).size())+" "+datasetName2);

			while (itSu2.hasNext()) {
				SimpleFeature f2 = itSu2.next();
				String id2 = f2.getAttribute(idField2).toString();
				Geometry geom2 = (Geometry) f2.getDefaultGeometryProperty().getValue();

				//check intersection
				//if(!geom1.intersects(geom2)) continue;
				double interArea = geom1.intersection(geom2).getArea();
				if(interArea == 0) continue;

				//store relation data
				bw1from2.write( id1+","+id2+","+(interArea/geom2.getArea())+","+interArea );
				bw1from2.newLine();
				bw2from1.write( id2+","+id1+","+(interArea/a1)+","+interArea );
				bw2from1.newLine();
			}
			itSu2.close();
		}
		itSu1.close();
		bw1from2.close();
		bw2from1.close();
	}


	//TODO: document. compute 1 from 2. Can be seen as conversion from 2 to 1.
	public static void computeStatValueFromIntersection(String datasetName1, String idField1, String datasetName2, String idField2, String statValuesFilePath2, String intersectionMatrix1from2, String outFolder) throws IOException {
		//create out file
		BufferedWriter bw1from2 = createFile(outFolder+"stat_values_"+datasetName1+"_from_"+datasetName2+"_intersection.csv", true);
		bw1from2.write(datasetName1+",value_from_"+datasetName2+"_intersection"); bw1from2.newLine();

		//load intersection matrix
		StatsHypercube matrix = CSV.load(intersectionMatrix1from2, "ratio"); matrix.delete("intersection_area");
		StatsIndex matrixI = new StatsIndex(matrix, "municipality", "grid"); matrix = null;
		//matrixI.print();

		//load 2 stat values
		HashMap<String, String> statValues2 = DicUtil.load(statValuesFilePath2, ",");

		for(String id1 : matrixI.getKeys()){
			double statValue1 = 0;
			//compute weigthted average of 2 contributions
			for(String id2 : matrixI.getKeys(id1)){
				String statValue2 = statValues2.get(id2);
				if(statValue2 == null || statValue2.equals("")) continue;

				String weight = matrixI.getSingleValueFlagged(id1, id2);
				if(weight == null || weight.equals("")) continue;

				statValue1 += Double.parseDouble(weight) * Double.parseDouble(statValue2);
			}
			//store
			bw1from2.write( id1+","+statValue1 );
			bw1from2.newLine();
		}
		bw1from2.close();
	}


	//create out file
	private static BufferedWriter createFile(String path, boolean override){
		File f = new File(path);
		if(override && f.exists()) f.delete();
		try {
			return new BufferedWriter(new FileWriter(f, true));
		} catch (IOException e) { e.printStackTrace(); }
		return null;
	}

}
