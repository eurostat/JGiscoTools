/**
 * 
 */
package eu.ec.estat.geostat.nuts;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;

import javax.imageio.ImageIO;

import org.geotools.brewer.color.ColorBrewer;
import org.geotools.brewer.color.StyleGenerator;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.function.Classifier;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.referencing.CRS;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.swing.JMapFrame;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.MultiPolygon;

import eu.ec.estat.java4eurostat.base.StatsHypercube;
import eu.ec.estat.java4eurostat.io.CSV;

/**
 * 
 * Library to produce NUTS based maps.
 * 
 * @author Julien Gaffuri
 *
 */
public class NUTSMap {
	//TODO show properly borders depending on nuts level
	//TODO legend - http://gis.stackexchange.com/questions/22962/create-a-color-scale-legend-for-choropleth-map-using-geotools-or-other-open-sou
	//TODO show other countries
	//TODO handle null values - or do opposite

	private static CoordinateReferenceSystem LAEA_CRS = null;
	static{
		try { LAEA_CRS = CRS.decode("EPSG:3035"); } catch (Exception e) { e.printStackTrace(); }
	}

	private MapContent map = null;
	private int level = 3; //NUTS level. can be 0, 1, 2, 3
	private int lod = 20; //Level of detail / scale. can be 1, 3, 10, 20 or 60
	public String propName = null; //te property to map
	public String classifier = "Quantile"; //EqualInterval, Jenks, Quantile, StandardDeviation, UniqueInterval
	public int classNb = 9;
	public String paletteName = "OrRd"; //see http://colorbrewer2.org

	private HashMap<String, Double> statData = null;
	private SimpleFeatureCollection fcRG;
	private Style RGStyle;

	public Color imgBckgrdColor = Color.WHITE;

	public NUTSMap(String title, int level, int lod, HashMap<String, Double> statData, String propName){
		this.level = level;
		this.lod = lod;
		this.statData = statData;
		this.propName = propName;
		map = new MapContent();
		map.setTitle(title);
		map.getViewport().setCoordinateReferenceSystem(LAEA_CRS);
		this.setBounds(2550000.0, 7400000.0, 1200000.0, 5500000.0);

		//get region features
		fcRG = NUTSShapeFile.get(this.lod, "RG").getFeatureCollection(NUTSShapeFile.getFilterRGLevel(this.level));

		//join stat data, if any
		if(this.statData != null)
			join(this.statData, this.propName);

		//buid region style
		StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();
		FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();
		if(propName == null){
			//Stroke stroke = styleFactory.createStroke(filterFactory.literal(Color.WHITE), filterFactory.literal(1));
			Fill fill = styleFactory.createFill(filterFactory.literal(Color.GRAY));
			PolygonSymbolizer polSymb = styleFactory.createPolygonSymbolizer(/*stroke*/null, fill, null);
			Rule rule = styleFactory.createRule();
			rule.symbolizers().add(polSymb);
			FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle(new Rule[]{rule});
			RGStyle = styleFactory.createStyle();
			RGStyle.featureTypeStyles().add(fts);
		} else
			RGStyle = getThematicStyle(fcRG, propName, classifier, classNb, paletteName);

		//BN style
		Style BNStyle;
		{
			Stroke stroke = styleFactory.createStroke( filterFactory.literal(Color.WHITE), filterFactory.literal(0.4));
			LineSymbolizer sepaJoinSymb = styleFactory.createLineSymbolizer(stroke, null);
			Rule rule = styleFactory.createRule();
			rule.symbolizers().add(sepaJoinSymb);
			FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle(new Rule[]{rule});
			BNStyle = styleFactory.createStyle();
			BNStyle.featureTypeStyles().add(fts);
		}

		//sepa and join style
		Style sepaJoinStyle;
		{
			Stroke stroke = styleFactory.createStroke( filterFactory.literal(Color.GRAY), filterFactory.literal(0.3));
			LineSymbolizer sepaJoinSymb = styleFactory.createLineSymbolizer(stroke, null);
			Rule rule = styleFactory.createRule();
			rule.symbolizers().add(sepaJoinSymb);
			FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle(new Rule[]{rule});
			sepaJoinStyle = styleFactory.createStyle();
			sepaJoinStyle.featureTypeStyles().add(fts);
		}

		//create and add layers
		switch (level) {
		case 0:
		{
			map.addLayer( new FeatureLayer(fcRG, RGStyle) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection(), BNStyle) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "JOIN").getFeatureCollection(NUTSShapeFile.getFilterSepaJoinLoD(this.lod)), sepaJoinStyle) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "SEPA").getFeatureCollection(NUTSShapeFile.getFilterSepaJoinLoD(this.lod)), sepaJoinStyle) );
		}
		break;
		case 1:
		{
			map.addLayer( new FeatureLayer(fcRG, RGStyle) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection(), BNStyle) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "JOIN").getFeatureCollection(NUTSShapeFile.getFilterSepaJoinLoD(this.lod)), sepaJoinStyle) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "SEPA").getFeatureCollection(NUTSShapeFile.getFilterSepaJoinLoD(this.lod)), sepaJoinStyle) );
		}
		break;
		case 2:
		{
			map.addLayer( new FeatureLayer(fcRG, RGStyle) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection(), BNStyle) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "JOIN").getFeatureCollection(NUTSShapeFile.getFilterSepaJoinLoD(this.lod)), sepaJoinStyle) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "SEPA").getFeatureCollection(NUTSShapeFile.getFilterSepaJoinLoD(this.lod)), sepaJoinStyle) );
		}
		break;
		case 3:
		{
			map.addLayer( new FeatureLayer(fcRG, RGStyle) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection(), BNStyle) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "JOIN").getFeatureCollection(NUTSShapeFile.getFilterSepaJoinLoD(this.lod)), sepaJoinStyle) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "SEPA").getFeatureCollection(NUTSShapeFile.getFilterSepaJoinLoD(this.lod)), sepaJoinStyle) );
		}
		break;
		}
	}

	public NUTSMap setBounds(double x1, double x2, double y1, double y2) {
		map.getViewport().setBounds(new ReferencedEnvelope(y1, y2, x1, x2, LAEA_CRS ));
		return this;
	}


	public NUTSMap show() {
		JMapFrame.showMap(map);
		return this;
	}


	private void join(HashMap<String, Double> statData, String propName) {
		try {
			SimpleFeatureType ft = DataUtilities.createType("NUTS_RG_joined","NUTS_ID:String,the_geom:MultiPolygon,"+propName+":Double");
			DefaultFeatureCollection fcRGJoin = new DefaultFeatureCollection("id", ft);

			//TODO handle null values - or do opposite
			SimpleFeatureIterator it = fcRG.features();
			while (it.hasNext()) {
				SimpleFeature f = it.next();
				String id = (String) f.getAttribute("NUTS_ID");
				Double value = statData.get(id);
				SimpleFeature f2 = SimpleFeatureBuilder.build( ft, new Object[]{ id, (MultiPolygon) f.getAttribute("the_geom"), value }, null);
				fcRGJoin.add(f2);
			}
			it.close();
			fcRG = fcRGJoin;
		} catch (Exception e) { e.printStackTrace(); }
	}


	private static Style getThematicStyle(SimpleFeatureCollection fc, String propName, String classifier, int classNb, String paletteName){
		//See http://docs.geotools.org/stable/userguide/extension/brewer/index.html

		//classify
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
		PropertyName propExp = ff.property(propName);
		Classifier groups = (Classifier) (ff.function(classifier, propExp, ff.literal(classNb))).evaluate(fc);

		//get colors
		Color[] colors = ColorBrewer.instance().getPalette(paletteName).getColors(classNb);

		//create style
		FeatureTypeStyle fts = StyleGenerator.createFeatureTypeStyle(
				groups, propExp, colors,
				propName+"-"+classifier+"-"+classNb+"-"+paletteName,
				fc.getSchema().getGeometryDescriptor(),
				StyleGenerator.ELSEMODE_IGNORE,
				1, //opacity
				null //default stroke
				);
		Style sty = CommonFactoryFinder.getStyleFactory().createStyle();
		sty.featureTypeStyles().add(fts);
		return sty;
	}



	public NUTSMap saveAsImage(final String file, final int imageWidth) {
		try {
			//prepare image
			ReferencedEnvelope mapBounds = map.getViewport().getBounds();
			Rectangle imageBounds = new Rectangle(0, 0, imageWidth, (int) Math.round(imageWidth * mapBounds.getSpan(0) / mapBounds.getSpan(1)));
			BufferedImage image = new BufferedImage(imageBounds.width, imageBounds.height, BufferedImage.TYPE_INT_RGB);
			Graphics2D gr = image.createGraphics();
			gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			gr.setPaint(imgBckgrdColor);
			gr.fill(imageBounds);

			GTRenderer renderer = new StreamingRenderer();
			renderer.setMapContent(map);
			renderer.paint(gr, imageBounds, mapBounds);

			ImageIO.write(image, "png", new File(file));
		} catch (Exception e) { e.printStackTrace(); }
		return this;
	}


	public static void main(String[] args) throws Exception {
		System.out.println("Start.");

		//load stat data
		StatsHypercube hc = CSV.load("H:/methnet/geostat/out/tour_occ_nin2_nuts3.csv", "value").selectDimValueEqualTo("nace_r2", "I551-I553").selectDimValueEqualTo("time", "2015 ");
		hc.delete("unit").delete("indic_to").delete("time").delete("nace_r2");
		HashMap<String, Double> statData = hc.toMap();
		hc = null;

		//make map
		NUTSMap map = new NUTSMap("", 3, 20, statData, "geo");
		map.show();
		map.saveAsImage("H:/desktop/ex3_60.png", 1400);

		System.out.println("End.");
	}

}
