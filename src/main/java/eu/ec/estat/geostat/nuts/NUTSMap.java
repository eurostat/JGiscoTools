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

import eu.ec.estat.java4eurostat.io.EurostatTSV;

/**
 * 
 * Library to easily produce maps based on NUTS regions.
 * 
 * @author Julien Gaffuri
 *
 */
public class NUTSMap {
	//TODO print classification/legend
	//TODO see how to fix classification/legend
	//TODO show other countries + blue see
	//TODO legend - http://gis.stackexchange.com/questions/22962/create-a-color-scale-legend-for-choropleth-map-using-geotools-or-other-open-sou
	//TODO borders: coastal, etc
	//TODO show DOM

	private static CoordinateReferenceSystem LAEA_CRS = null;
	static{
		//try { LAEA_CRS = CRS.decode("EPSG:3035"); } catch (Exception e) { e.printStackTrace(); }
		LAEA_CRS = NUTSShapeFile.get(60, "RG").getCRS();
	}

	private MapContent map = null;
	private int level = 3; //NUTS level. can be 0, 1, 2, 3
	private int lod = 20; //Level of detail / scale. can be 1, 3, 10, 20 or 60
	public String propName = null; //the property to map
	public String classifier = "Quantile"; //EqualInterval, Jenks, Quantile, StandardDeviation, UniqueInterval
	public int classNb = 9;
	public String paletteName = "OrRd"; //see http://colorbrewer2.org
	boolean showJoin=false, showSepa=false;

	private HashMap<String, Double> statData = null;

	//public Color imgBckgrdColor = Color.WHITE;
	//public Color imgBckgrdColor = new Color(240,248,255); //aliceblue
	public Color imgBckgrdColor = new Color(173,216,230); //lightblue
	//public Color imgBckgrdColor = new Color(70,130,180); //steelblue

	public NUTSMap(String title, int level, int lod, String propName, HashMap<String, Double> statData){
		this.level = level;
		this.lod = lod;
		this.statData = statData;
		this.propName = propName;
		map = new MapContent();
		map.setTitle(title);
		map.getViewport().setCoordinateReferenceSystem(LAEA_CRS);
		this.setBounds(1340000.0, 5450000.0, 2580000.0, 7350000.0);



		//get region features
		SimpleFeatureCollection fcRG = NUTSShapeFile.get(this.lod, "RG").getFeatureCollection(NUTSShapeFile.getFilterByLevel(this.level));
		SimpleFeatureCollection fcRGNoDta = null;

		//join stat data, if any
		SimpleFeatureCollection[] out = join(fcRG, this.statData, this.propName);
		fcRG = out[0]; fcRGNoDta = out[1];

		StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();
		FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();

		//buid region style
		Style RGStyleNoData;
		{
			//Stroke stroke = styleFactory.createStroke(filterFactory.literal(Color.WHITE), filterFactory.literal(1));
			Fill fill = styleFactory.createFill(filterFactory.literal(Color.GRAY));
			PolygonSymbolizer polSymb = styleFactory.createPolygonSymbolizer(/*stroke*/null, fill, null);
			Rule rule = styleFactory.createRule();
			rule.symbolizers().add(polSymb);
			FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle(new Rule[]{rule});
			RGStyleNoData = styleFactory.createStyle();
			RGStyleNoData.featureTypeStyles().add(fts);
		}
		Style RGStyle = null;
		if(fcRG.size()>0){
			Stroke stroke = styleFactory.createStroke( filterFactory.literal(Color.WHITE), filterFactory.literal(0.0001), filterFactory.literal(0));
			RGStyle = getThematicStyle(fcRG, propName, classifier, classNb, paletteName, stroke);
		}

		map.addLayer( new FeatureLayer(fcRGNoDta, RGStyleNoData) );
		map.addLayer( new FeatureLayer(fcRG, RGStyle) );



		//BN style
		//TODO propose generic border display pattern - level-width-color
		if(this.level == 0){
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection("STAT_LEVL_<=0 AND COAS_FLAG='F'"), getLineStyle(Color.WHITE, 0.8)) );
		} else if(this.level == 1){
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection("STAT_LEVL_<=1 AND COAS_FLAG='F'"), getLineStyle(Color.LIGHT_GRAY, 0.3)) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection("STAT_LEVL_<=0 AND COAS_FLAG='F'"), getLineStyle(Color.WHITE, 1)) );
		} else if(this.level == 2){
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection("STAT_LEVL_<=2 AND COAS_FLAG='F'"), getLineStyle(Color.LIGHT_GRAY, 0.3)) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection("STAT_LEVL_<=0 AND COAS_FLAG='F'"), getLineStyle(Color.WHITE, 1)) );
		} else if(this.level == 3){
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection("STAT_LEVL_<=2 AND COAS_FLAG='F'"), getLineStyle(Color.LIGHT_GRAY, 0.5)) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection("STAT_LEVL_<=0 AND COAS_FLAG='F'"), getLineStyle(Color.WHITE, 1)) );
		}



		//sepa and join
		if(showJoin || showSepa){
			Style sepaJoinStyle;
			Stroke stroke = styleFactory.createStroke( filterFactory.literal(Color.GRAY), filterFactory.literal(0.3));
			LineSymbolizer sepaJoinSymb = styleFactory.createLineSymbolizer(stroke, null);
			Rule rule = styleFactory.createRule();
			rule.symbolizers().add(sepaJoinSymb);
			FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle(new Rule[]{rule});
			sepaJoinStyle = styleFactory.createStyle();
			sepaJoinStyle.featureTypeStyles().add(fts);

			if(showJoin) map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "JOIN").getFeatureCollection(NUTSShapeFile.getFilterSepaJoinLoD(this.lod)), sepaJoinStyle) );
			if(showSepa) map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "SEPA").getFeatureCollection(NUTSShapeFile.getFilterSepaJoinLoD(this.lod)), sepaJoinStyle) );
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



	private SimpleFeatureCollection[] join(SimpleFeatureCollection fc, HashMap<String, Double> statData, String propName) {
		try {
			SimpleFeatureType ft = DataUtilities.createType("NUTS_RG_joined","NUTS_ID:String,the_geom:MultiPolygon,"+propName+":Double"); //:srid=3035
			ft = DataUtilities.createSubType(ft, null, fc.getSchema().getCoordinateReferenceSystem());
			DefaultFeatureCollection fcRGJoin = new DefaultFeatureCollection("fc_joined", ft);
			DefaultFeatureCollection fcRGNoData = new DefaultFeatureCollection("fc_nodata", ft);

			SimpleFeatureIterator it = fc.features();
			while (it.hasNext()) {
				SimpleFeature f = it.next();
				String id = (String) f.getAttribute("NUTS_ID");
				Double value = null;
				if(statData != null) value = statData.get(id);
				if(value==null) {
					SimpleFeature f2 = SimpleFeatureBuilder.build( ft, new Object[]{ id, (MultiPolygon)f.getAttribute("the_geom"), 0 }, null);
					fcRGNoData.add(f2);
				} else {
					SimpleFeature f2 = SimpleFeatureBuilder.build( ft, new Object[]{ id, (MultiPolygon)f.getAttribute("the_geom"), value.doubleValue() }, null);
					fcRGJoin.add(f2);
				}
			}
			it.close();
			return new SimpleFeatureCollection[]{fcRGJoin, fcRGNoData};
		} catch (Exception e) { e.printStackTrace(); }
		return null;
	}


	private static Style getThematicStyle(SimpleFeatureCollection fc, String propName, String classifier, int classNb, String paletteName, Stroke stroke){
		//See http://docs.geotools.org/stable/userguide/extension/brewer/index.html

		//classify
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
		PropertyName propExp = ff.property(propName);
		Classifier groups = (Classifier) (ff.function(classifier, propExp, ff.literal(classNb))).evaluate(fc);
		//TODO System.out.println(groups);

		//get colors
		Color[] colors = ColorBrewer.instance().getPalette(paletteName).getColors(classNb);

		//create style
		FeatureTypeStyle fts = StyleGenerator.createFeatureTypeStyle(
				groups, propExp, colors,
				propName+"-"+classifier+"-"+classNb+"-"+paletteName,
				fc.getSchema().getGeometryDescriptor(),
				StyleGenerator.ELSEMODE_IGNORE,
				1, //opacity
				stroke
				);
		Style sty = CommonFactoryFinder.getStyleFactory().createStyle();
		sty.featureTypeStyles().add(fts);
		return sty;
	}

	private static Style getLineStyle(Color col, double width){
		StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();
		FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();

		Stroke stroke = styleFactory.createStroke( filterFactory.literal(col), filterFactory.literal(width));
		LineSymbolizer symb = styleFactory.createLineSymbolizer(stroke, null);
		Rule rule = styleFactory.createRule();
		rule.symbolizers().add(symb);
		FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle(new Rule[]{rule});
		Style s = styleFactory.createStyle();
		s.featureTypeStyles().add(fts);
		return s;
	}




	public NUTSMap saveAsImage(final String file, final int imageWidth) {
		try {
			//prepare image
			ReferencedEnvelope mapBounds = map.getViewport().getBounds();
			Rectangle imageBounds = new Rectangle(0, 0, imageWidth, (int) Math.round(imageWidth * mapBounds.getSpan(1) / mapBounds.getSpan(0)));
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
		HashMap<String, Double> statData = EurostatTSV.load("H:/eurobase/tour_occ_nin2.tsv").selectDimValueEqualTo("unit","NR","nace_r2","I551-I553","indic_to","B006","time","2015 ")
				.delete("unit").delete("nace_r2").delete("indic_to").delete("time").toMap();
		NUTSMap map = new NUTSMap("", 2, 60, "geo", statData);
		map.saveAsImage("H:/desktop/map.png", 1000);

		/*HashMap<String, Double> statData =
				CSV.load("H:/methnet/geostat/out/tour_occ_nin2_nuts3.csv", "value").selectDimValueEqualTo("nace_r2", "I551-I553", "time", "2015 ")
				.delete("unit").delete("indic_to").delete("time").delete("nace_r2")
				.toMap();
		NUTSMap map = new NUTSMap("", 3, 60, "geo", statData);
		//map.show();
		map.saveAsImage("H:/desktop/map.png", 1000);
		//map.saveAsImage("/home/juju/Bureau/map.png", 1000);*/

		System.out.println("End.");
	}

}
