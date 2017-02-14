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

import eu.ec.estat.geostat.io.ShapeFile;

/**
 * @author julien Gaffuri
 *
 */
public class TourismUseCase {
	public static String BASE_PATH = "H:/geodata/";
	public static String NUTS_SHP_LVL2 = BASE_PATH + "gisco_stat_units/NUTS_2013_01M_SH/data/NUTS_RG_01M_2013_LAEA_lvl2.shp";
	public static String POI_SHP = BASE_PATH + "eur2016_12/mnpoi.shp";

	public static void main(String[] args) throws Exception {
		System.out.println("Start.");

		//download/update data for tourism
		//EurobaseIO.update("H:/eurobase/", "tour_occ_nim", "tour_occ_nin2", "tour_occ_nin2d", "tour_occ_nin2c");

		/*/load tourism data
		StatsHypercube hc = EurostatTSV.load("H:/eurobase/tour_occ_nin2.tsv",
				new Selection.And(
						new Selection.DimValueEqualTo("unit","NR"), //Number
						new Selection.DimValueEqualTo("nace_r2","I551-I553"), //Hotels; holiday and other short-stay accommodation; camping grounds, recreational vehicle parks and trailer parks
						new Selection.DimValueEqualTo("indic_to","B006"), //Nights spent, total
						//keep only nuts 2 regions
						new Selection.Criteria() { public boolean keep(Stat stat) { return stat.dims.get("geo").length() == 4; } },
						//keep only years after 2010
						new Selection.Criteria() { public boolean keep(Stat stat) { return Integer.parseInt(stat.dims.get("time").replace(" ", "")) >= 2010; } }
						));
		hc.delete("unit"); hc.delete("indic_to"); hc.delete("nace_r2");
		StatsIndex hcI = new StatsIndex(hc, "time", "geo");
		//hc.printInfo(); //hcI.print();
		hc = null;*/


		//dasymetric analysis

		//geo to statistical unit
		//DasymetricMapping.aggregateGeoStatsFromGeoToStatisticalUnits(new ShapeFile(NUTS_SHP_LVL2).getSimpleFeatures(), "NUTS_ID", new ShapeFile(POI_SHP).getFeatureStore(), "H:/methnet/geostat/out/1_geo_to_stats.csv");
		DasymetricMapping.aggregateGeoStatsFromGeoToStatisticalUnits(new ShapeFile(NUTS_SHP_LVL2).getSimpleFeatures(), "NUTS_ID", new ShapeFile(POI_SHP).getSimpleFeatures(), "H:/methnet/geostat/out/1_geo_to_stats.csv");
		//statistical unit to geo
		//DasymetricMapping.allocateGeoStatsFromStatisticalUnitsToGeo(POI_SHP, "ID", NUTS_SHP_LVL2, "NUTS_ID", statUnitValuesPath, "H:/methnet/geostat/out/1_geo_to_stats.csv", "H:/methnet/geostat/out/2_stats_lvl2_to_geo.csv");



		//compute validation figures

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





		System.out.println("End.");
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
