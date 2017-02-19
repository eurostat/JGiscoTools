/**
 * 
 */
package eu.ec.estat.geostat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.MapContent;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;

import eu.ec.estat.geostat.dasymetric.DasymetricMapping;
import eu.ec.estat.geostat.io.ShapeFile;
import eu.ec.estat.java4eurostat.analysis.Validation;
import eu.ec.estat.java4eurostat.base.Selection;
import eu.ec.estat.java4eurostat.base.Stat;
import eu.ec.estat.java4eurostat.base.StatsHypercube;
import eu.ec.estat.java4eurostat.base.StatsIndex;
import eu.ec.estat.java4eurostat.io.CSV;
import eu.ec.estat.java4eurostat.io.EurostatTSV;

/**
 * @author julien Gaffuri
 *
 */
public class TourismUseCase {
	public static String BASE_PATH = "H:/geodata/";
	public static String NUTS_SHP_LVL2 = BASE_PATH + "gisco_stat_units/NUTS_2013_01M_SH/data/NUTS_RG_01M_2013_LAEA_lvl2.shp";
	public static String NUTS_SHP_LVL3 = BASE_PATH + "gisco_stat_units/NUTS_2013_01M_SH/data/NUTS_RG_01M_2013_LAEA_lvl3.shp";
	public static String POI_TOURISEM_SHP_BASE = BASE_PATH + "eur2016_12/mnpoi_";


	//TODO validation with E4 figures
	//check values of the validation are the right ones. camping, etc included? Check on maximum values.
	//futher analyse maximum errors
	//better analyse validation results: show on map !
	//check consistency of validation data: compute aggregation of NUTS3 values to NUTS2
	//TODO show maps - make generic library

	//TODO aggregate at 10km grid level

	//TODO run use case on urban audit data? Use for validation?
	//TODO focus on FR.

	public static void main(String[] args) throws Exception {
		System.out.println("Start.");

		//download/update data for tourism
		//EurobaseIO.update("H:/eurobase/", "tour_occ_nim", "tour_occ_nin2", "tour_occ_nin2d", "tour_occ_nin2c", "urb_ctour");

		//runDasymetric();
		//computeValidationData();
		//analyseValidationData();
		//produceMaps();

		System.out.println("End.");
	}


	public static void runDasymetric(){

		//load tourism data
		StatsHypercube hc = EurostatTSV.load("H:/eurobase/tour_occ_nin2.tsv",
				new Selection.And(
						new Selection.DimValueEqualTo("unit","NR"), //Number
						//new Selection.DimValueEqualTo("nace_r2","I551-I553"), //Hotels; holiday and other short-stay accommodation; camping grounds, recreational vehicle parks and trailer parks
						new Selection.DimValueEqualTo("indic_to","B006"), //Nights spent, total
						//keep only nuts 2 regions
						new Selection.Criteria() { public boolean keep(Stat stat) { return stat.dims.get("geo").length() == 4; } },
						//keep only years after 2010
						new Selection.Criteria() { public boolean keep(Stat stat) { return Integer.parseInt(stat.dims.get("time").replace(" ", "")) >= 2010; } }
						));
		hc.delete("unit"); hc.delete("indic_to");
		StatsIndex hcI = new StatsIndex(hc, "nace_r2", "time", "geo");
		//hc.printInfo();
		//hcI.print();
		hc = null;

		//save as csv
		//String time = "2015 ", outFile = "H:/methnet/geostat/out/stats_lvl2_"+time+".csv";
		//statIndexToCSV(hcI.getSubIndex(time), "NUTS_ID", outFile);

		//output structure
		StatsHypercube out = new StatsHypercube("geo", "time", "unit", "nace_r2", "indic_to");

		//go through nace codes
		for(String nace : new String[]{"I551-I553","I551","I552","I553"}){

			//create dasymetric analysis object
			DasymetricMapping dm = new DasymetricMapping(
					new ShapeFile(NUTS_SHP_LVL2).getFeatureStore(),
					"NUTS_ID",
					null,
					new ShapeFile(POI_TOURISEM_SHP_BASE+nace+".shp").getFeatureStore(),
					"ID",
					new ShapeFile(NUTS_SHP_LVL3).getFeatureStore(),
					"NUTS_ID"
					);

			//dm.computeGeoStatInitial();   CSV.save(dm.geoStatsInitialHC, "value", "H:/methnet/geostat/out/", "1_geo_to_ini_stats_"+nace+".csv");
			dm.geoStatsInitialHC = CSV.load("H:/methnet/geostat/out/POI_to_NUTS_2___"+nace+".csv", "value");

			//dm.computeGeoStatFinal();   CSV.save(dm.geoStatsFinalHC, "value", "H:/methnet/geostat/out/", "1_geo_to_fin_stats_"+nace+".csv");
			dm.geoStatsFinalHC = CSV.load("H:/methnet/geostat/out/POI_to_NUTS_3___"+nace+".csv", "value");



			//compute values for all years
			for(String time : hcI.getKeys(nace)){
				//get stat values
				dm.statValuesInitial = hcI.getSubIndex(nace, time);
				if(dm.statValuesInitial == null) continue;

				//compute values
				dm.computeFinalStat();

				//
				for(Stat s : dm.finalStatsSimplifiedHC.stats) {
					s.dims.put("time", time);
					s.dims.put("unit", "NR");
					s.dims.put("nace_r2", nace);
					s.dims.put("indic_to", "B006");
				}
				out.stats.addAll(dm.finalStatsSimplifiedHC.stats);
			}
		}
		CSV.save(out, "value", "H:/methnet/geostat/out/", "tour_occ_nin2_nuts3.csv");

	}


	private static void computeValidationData() {

		//load data
		StatsHypercube hc = CSV.load("H:/methnet/geostat/out/tour_occ_nin2_nuts3.csv", "value").selectDimValueEqualTo("nace_r2", "I551-I553");
		hc.delete("unit"); hc.delete("indic_to"); hc.delete("nace_r2");
		StatsIndex hcI = new StatsIndex(hc, "geo", "time");
		hc = null;

		//load validation data
		StatsIndex hcIval = new StatsIndex(EurostatTSV.load("H:/methnet/geostat/validation/validation_data.tsv"), "geo", "time");

		//time
		//[2010 , 2011 , 2012 , 2013 , 2014 , 2015 ]
		//[2009, 2008, 2007, 2006, 2005, 2013, 2012, 2011, 2010]
		// -> [2010 , 2011 , 2012 , 2013 ]


		StatsHypercube diffHC = new StatsHypercube("geo","time");
		StatsHypercube diffPercHC = new StatsHypercube("geo","time");
		for(String geo : hcI.getKeys()){
			if(hcIval.getKeys(geo) == null) continue;
			for(String time : hcI.getKeys(geo)){

				//retrieve both values to compare
				double valVal = hcIval.getSingleValue(geo, time.replace(" ", ""));
				if(Double.isNaN(valVal) || valVal == 0) continue;
				double val = hcI.getSingleValue(geo, time);
				if(Double.isNaN(val) || val == 0) continue;

				//store comparison figures
				diffHC.stats.add(new Stat(Math.abs(val-valVal), "geo", geo, "time", time));
				diffPercHC.stats.add(new Stat(100*Math.abs(val-valVal)/valVal, "geo", geo, "time", time));
			}
		}

		CSV.save(diffHC, "diff", "H:/methnet/geostat/validation/", "validation_result_diff.csv");
		CSV.save(diffPercHC, "diffPerc", "H:/methnet/geostat/validation/", "validation_result_diff_perc.csv");
	}

	private static void analyseValidationData() {

		//load data
		StatsHypercube diffHC = CSV.load("H:/methnet/geostat/validation/validation_result_diff.csv", "diff");
		StatsHypercube diffPercHC = CSV.load("H:/methnet/geostat/validation/validation_result_diff_perc.csv", "diffPerc");

		System.out.println();
		Validation.printBasicStatistics(diffHC);
		System.out.println();
		Validation.printBasicStatistics(diffPercHC);
		System.out.println();

	}




	private static void produceMaps() {
		//show results on maps

		/*/produce map
		//https://github.com/geotools/geotools/blob/master/docs/src/main/java/org/geotools/tutorial/style/StyleLab.java
		//http://docs.geotools.org/latest/userguide/library/render/gtrenderer.html
		//http://docs.geotools.org/latest/userguide/library/render/index.html
		//http://gis.stackexchange.com/questions/123903/how-to-create-a-map-and-save-it-to-an-image-with-geotools

		// Create a map content and add our shapefile to it
		MapContent map = new MapContent();
		map.setTitle("NUTS Tourism");
		//MapViewport vp = new MapViewport(new ReferencedEnvelope(-5.0, 18.0, 42.0, 55.0, shpFileNUTS.getCRS() ));
		//vp.setCoordinateReferenceSystem(CRS.decode("EPSG:3035"));
		//map.setViewport(vp);
		MapViewport vp = map.getViewport();
		vp.setCoordinateReferenceSystem(CRS.decode("EPSG:3035"));
		vp.setBounds(new ReferencedEnvelope(2500000.0,5600000.0,1700000.0,4700000.0, CRS.decode("EPSG:3035") ));

		//style
		// create a partially opaque outline stroke
		StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();
		FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();

		Stroke stroke = styleFactory.createStroke(
				filterFactory.literal(Color.WHITE),
				filterFactory.literal(1),
				filterFactory.literal(0.5));

		// create a partial opaque fill
		Fill fill = styleFactory.createFill(
				filterFactory.literal(Color.gray),
				filterFactory.literal(0.5));
		PolygonSymbolizer sym = styleFactory.createPolygonSymbolizer(stroke, fill, null);
		Rule rule = styleFactory.createRule();
		rule.symbolizers().add(sym);
		FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle(new Rule[]{rule});
		Style style = styleFactory.createStyle();
		style.featureTypeStyles().add(fts);

		//create and add layer
		Layer layer = new FeatureLayer(shpFileNUTS.getFeatureCollection(f2), style);
		map.addLayer(layer);

		//
		//JMapFrame.showMap(map);
		saveImage(map, "H:/desktop/ex.png", 800);
		 */
	}





	public static void saveImage(final MapContent map, final String file, final int imageWidth) {
		try {
			ReferencedEnvelope mapBounds = map.getViewport().getBounds();
			Rectangle imageBounds = new Rectangle(0, 0, imageWidth, (int) Math.round(imageWidth * mapBounds.getSpan(1) / mapBounds.getSpan(0)));
			BufferedImage image = new BufferedImage(imageBounds.width, imageBounds.height, BufferedImage.TYPE_INT_RGB);

			Graphics2D gr = image.createGraphics();
			gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			gr.setPaint(Color.WHITE);
			gr.fill(imageBounds);

			GTRenderer renderer = new StreamingRenderer();
			renderer.setMapContent(map);
			renderer.paint(gr, imageBounds, mapBounds);

			ImageIO.write(image, "png", new File(file));
		} catch (Exception e) { e.printStackTrace(); }
	}

}
