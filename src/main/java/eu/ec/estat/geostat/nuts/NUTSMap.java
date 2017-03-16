/**
 * 
 */
package eu.ec.estat.geostat.nuts;

import java.awt.Color;
import java.awt.Font;
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
import org.geotools.filter.function.ExplicitClassifier;
import org.geotools.filter.function.RangedClassifier;
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

import eu.ec.estat.java4eurostat.base.StatsHypercube;
import eu.ec.estat.java4eurostat.io.EurostatTSV;
import eu.ec.estat.java4eurostat.util.Util;

/**
 * 
 * Library to easily produce maps based on NUTS regions.
 * 
 * @author Julien Gaffuri
 *
 */
public class NUTSMap {
	//TODO fix problems in quantiles
	//TODO nice classes - nice labels

	//TODO small multiple
	//TODO gif animation on time

	//TODO show DOM
	//TODO logo + copyright text "Administrative boundaries: (C) Eurogeographics (C) UN-FAO (C) Turksat"
	//TODO show scale bar?

	private static CoordinateReferenceSystem LAEA_CRS = null;
	static{
		//try { LAEA_CRS = CRS.decode("EPSG:3035"); } catch (Exception e) { e.printStackTrace(); }
		LAEA_CRS = NUTSShapeFile.get(60, "RG").getCRS();
	}

	private MapContent map = null;

	public int level = 3; //NUTS level. can be 0, 1, 2, 3
	public int lod = 20; //Level of detail / scale. can be 1, 3, 10, 20 or 60

	public HashMap<String, Double> statData = null;
	public String propName = null; //the property to map

	public Classifier classifier;
	public String classifierName = "Quantile"; //EqualInterval, Jenks, Quantile, StandardDeviation, UniqueInterval
	public int classNb = 9;
	public String paletteName = "YlOrRd"; //"OrRd"; //see http://colorbrewer2.org
	public Color[] colors = null;

	boolean showJoin=false, showSepa=false;
	public boolean showGraticules = true;
	public Color graticulesColor = new Color(200,200,200);
	public double graticulesWidth = 0.4;

	//public Color imgBckgrdColor = Color.WHITE;
	//public Color imgBckgrdColor = new Color(240,248,255); //aliceblue
	public Color imgBckgrdColor = new Color(173,216,230); //lightblue
	//public Color imgBckgrdColor = new Color(70,130,180); //steelblue
	public Color fontColor = Color.BLACK;
	public int fontSize = 30;
	public String fontFamily = "Arial";
	public int fontStrength = Font.PLAIN;

	public Color cntrRGColor = Color.LIGHT_GRAY;
	public Color cntrBNColor = Color.WHITE;
	public Color nutsBNColor1 = Color.LIGHT_GRAY;
	public Color nutsBNColor2 = Color.WHITE;

	public int legendWidth = 200;
	public int legendHeightPerClass = 20;
	public int legendPadding = 5;
	public int legendRoundingDecimalNB = 3;


	public NUTSMap(int level, int lod, String propName, HashMap<String, Double> statData, Classifier classifier){
		this.level = level;
		this.lod = lod;
		this.statData = statData;
		this.propName = propName;
		this.classifier = classifier;
		map = new MapContent();
	}

	public NUTSMap setBounds(double x1, double x2, double y1, double y2) {
		map.getViewport().setBounds(new ReferencedEnvelope(y1, y2, x1, x2, LAEA_CRS ));
		return this;
	}

	public NUTSMap dispose() { this.map.dispose(); return this; }
	public NUTSMap setTitle(String title) { map.setTitle(title); return this; }
	public NUTSMap setClassifier(Classifier classifier) { this.classifier = classifier; return this; }
	public NUTSMap show() { JMapFrame.showMap(map); return this; }

	public NUTSMap make(){
		map.getViewport().setCoordinateReferenceSystem(LAEA_CRS);
		this.setBounds(1340000.0, 5450000.0, 2580000.0, 7350000.0);


		//graticules
		if(this.showGraticules)
			map.addLayer( new FeatureLayer(NUTSShapeFile.getGraticules().getFeatureCollection(NUTSShapeFile.GRATICULE_FILTER_5), getLineStyle(this.graticulesColor, this.graticulesWidth)) );


		//countries
		map.addLayer( new FeatureLayer(NUTSShapeFile.getCNTR(lod, "RG").getFeatureCollection(NUTSShapeFile.CNTR_NEIG_CNTR), getPolygonStyle(cntrRGColor, null)) );
		map.addLayer( new FeatureLayer(NUTSShapeFile.getCNTR(lod, "BN").getFeatureCollection("COAS_FLAG='F'"), getLineStyle(cntrBNColor, 0.2)) );


		//get region features
		SimpleFeatureCollection fcRG = NUTSShapeFile.get(this.lod, "RG").getFeatureCollection(NUTSShapeFile.getFilterByLevel(this.level));
		SimpleFeatureCollection fcRGNoDta = null;

		//join stat data, if any
		SimpleFeatureCollection[] out = join(fcRG, this.statData, this.propName);
		fcRG = out[0]; fcRGNoDta = out[1];

		StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();
		FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();

		//RG
		Style RGStyleNoData = getPolygonStyle(Color.GRAY, null);
		Style RGStyle = RGStyleNoData;
		if(fcRG.size()>0){
			Stroke stroke = styleFactory.createStroke( filterFactory.literal(Color.WHITE), filterFactory.literal(0.0001), filterFactory.literal(0));
			this.colors = ColorBrewer.instance().getPalette(paletteName).getColors(classNb);
			if(this.classifier==null) this.classifier = getClassifier(fcRG, propName, classifierName, classNb);
			RGStyle = getThematicStyle(fcRG, propName, this.classifier, this.colors, stroke);
		}

		map.addLayer( new FeatureLayer(fcRGNoDta, RGStyleNoData) );
		map.addLayer( new FeatureLayer(fcRG, RGStyle) );

		//BN
		//TODO propose generic border display pattern - level-width-color
		if(this.level == 0){
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection("STAT_LEVL_<=0 AND COAS_FLAG='F'"), getLineStyle(nutsBNColor2, 0.8)) );
		} else if(this.level == 1){
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection("STAT_LEVL_<=1 AND COAS_FLAG='F'"), getLineStyle(nutsBNColor1, 0.3)) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection("STAT_LEVL_<=0 AND COAS_FLAG='F'"), getLineStyle(nutsBNColor2, 1)) );
		} else if(this.level == 2){
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection("STAT_LEVL_<=2 AND COAS_FLAG='F'"), getLineStyle(nutsBNColor1, 0.3)) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection("STAT_LEVL_<=0 AND COAS_FLAG='F'"), getLineStyle(nutsBNColor2, 1)) );
		} else if(this.level == 3){
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection("STAT_LEVL_<=2 AND COAS_FLAG='F'"), getLineStyle(nutsBNColor1, 0.5)) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection("STAT_LEVL_<=0 AND COAS_FLAG='F'"), getLineStyle(nutsBNColor2, 1)) );
		}

		//sepa and join
		if(showJoin || showSepa){
			Style sepaJoinStyle = getLineStyle(Color.GRAY, 0.3);
			if(showJoin) map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "JOIN").getFeatureCollection(NUTSShapeFile.getFilterSepaJoinLoD(this.lod)), sepaJoinStyle) );
			if(showSepa) map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "SEPA").getFeatureCollection(NUTSShapeFile.getFilterSepaJoinLoD(this.lod)), sepaJoinStyle) );
		}

		return this;
	}

	public NUTSMap makeDark() {
		this.imgBckgrdColor = Color.BLACK;
		this.cntrRGColor = Color.DARK_GRAY;
		this.cntrBNColor = Color.BLACK;
		this.nutsBNColor1 = Color.DARK_GRAY;
		this.nutsBNColor2 = Color.BLACK;
		this.fontColor = Color.WHITE;
		this.graticulesColor = new Color(40,40,40);
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

	public NUTSMap printClassification() {
		System.out.println("Classifier "+ this.classifier.getSize());
		if(this.classifier instanceof RangedClassifier){
			RangedClassifier rc = (RangedClassifier)this.classifier;
			for(int slot=0; slot<rc.getSize(); slot++){
				System.out.println("From " + rc.getMin(slot) + " to " + rc.getMax(slot)+ "    - title: " + rc.getTitle(slot));
			}
		} else if(this.classifier instanceof ExplicitClassifier){
			System.out.println("ExplicitClassifier not handled yet");
		}
		return this;
	}





	public static Classifier getClassifier(SimpleFeatureCollection fc, String propName, String classifierName, int classNb){
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
		PropertyName propExp = ff.property(propName);
		return (Classifier) (ff.function(classifierName, propExp, ff.literal(classNb))).evaluate(fc);
	}

	public static RangedClassifier getClassifier(double... breaks) {
		Double[] min = new Double[breaks.length+1]; min[0]=-Double.MAX_VALUE; for(int i=1; i<breaks.length+1; i++) min[i]=breaks[i-1];
		Double[] max = new Double[breaks.length+1]; for(int i=0; i<breaks.length; i++) max[i]=breaks[i]; max[breaks.length]=Double.MAX_VALUE;
		RangedClassifier rc = new RangedClassifier(min, max);
		return rc;
	}

	public static void drawLegend(Graphics2D gr, RangedClassifier classifier, Color[] colors, int decimalNB, int offsetX, int offsetY, int width, int heightPerClass, int padding) {
		int colorRampWidth = 50;
		int nb = classifier.getSize();
		int height = heightPerClass * nb + 2*padding;
		gr.setColor(Color.WHITE); gr.fillRect(offsetX, offsetY, width, height);
		gr.setColor(Color.BLACK); gr.drawRect(offsetX, offsetY, width-1, height-1);
		for(int slot=0; slot<nb; slot++) {
			gr.setColor(colors[slot]);
			gr.fillRect(offsetX+padding, offsetY+padding+slot*heightPerClass, colorRampWidth, heightPerClass);
			gr.setColor(Color.BLACK);
			int fontSize = heightPerClass-4;
			gr.setFont(new Font("Arial", Font.BOLD, fontSize));
			double val = Util.round((Double)classifier.getMax(slot), decimalNB); //TODO make it nice
			if(slot!=nb-1) gr.drawString(""+val, offsetX+padding+colorRampWidth+padding, (int)(offsetY+padding+(slot+1)*heightPerClass+fontSize*0.5));
		}
		gr.setColor(Color.BLACK); gr.drawRect(offsetX+padding, offsetY+padding, colorRampWidth, height-2*padding);
	}

	public static void saveAsImage(RangedClassifier classifier, Color[] colors, String file) { saveAsImage(classifier, colors, file, 3, 200, 20, 5); }
	public static void saveAsImage(RangedClassifier classifier, Color[] colors, String file, int decimalNB, int width, int heightPerClass, int padding) {
		try {
			int height = heightPerClass * classifier.getSize() + 2*padding;
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D gr = image.createGraphics();
			gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			drawLegend(gr, classifier, colors, decimalNB, 0, 0, width, heightPerClass, padding);
			ImageIO.write(image, "png", new File(file));
		} catch (Exception e) { e.printStackTrace(); }
	}


	private static Style getThematicStyle(SimpleFeatureCollection fc, String propName, Classifier classifier, Color[] colors, Stroke stroke){
		//See http://docs.geotools.org/stable/userguide/extension/brewer/index.html

		//create style
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
		PropertyName propExp = ff.property(propName);
		FeatureTypeStyle fts = StyleGenerator.createFeatureTypeStyle(
				classifier, propExp, colors,
				propName,
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

	private static Style getPolygonStyle(Color fillCol, Stroke stroke){
		StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();
		FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();

		Fill fill = styleFactory.createFill(filterFactory.literal(fillCol));
		PolygonSymbolizer polSymb = styleFactory.createPolygonSymbolizer(stroke, fill, null);
		Rule rule = styleFactory.createRule();
		rule.symbolizers().add(polSymb);
		FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle(new Rule[]{rule});
		Style s = styleFactory.createStyle();
		s.featureTypeStyles().add(fts);
		return s;
	}




	public NUTSMap saveAsImage(final String file) { return saveAsImage(file, 1000, true, true); }
	public NUTSMap saveAsImage(final String file, final int imageWidth, boolean withTitle, boolean withLegend) {
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

			//write title
			if(withTitle){
				gr.setColor(fontColor);
				gr.setFont(new Font(fontFamily, fontStrength, fontSize));
				gr.drawString(map.getTitle(), 10, fontSize+5);
			}

			if(withLegend)
				drawLegend(gr, (RangedClassifier)this.classifier, this.colors, this.legendRoundingDecimalNB, imageWidth-legendWidth-legendPadding, legendPadding, legendWidth, legendHeightPerClass, legendPadding);

			ImageIO.write(image, "png", new File(file));
		} catch (Exception e) { e.printStackTrace(); }
		return this;
	}

	public NUTSMap saveLegendAsImage(String file) { saveLegendAsImage(file, 3, 100, 20, 5); return this; }
	public NUTSMap saveLegendAsImage(String file, int decimalNB, int width, int heightPerClass, int padding) {
		saveAsImage((RangedClassifier) this.classifier, this.colors, file, decimalNB, width, heightPerClass, padding);
		return this;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("Start.");
		//String outPath = "H:/desktop";
		String outPath = "/home/juju/Bureau/";
		String dataPath = "stat_cache/";

		//EurobaseIO.update(dataPath, "tour_occ_nim", "tour_occ_nin2");

		//load stat data
		StatsHypercube data = EurostatTSV.load(dataPath+"tour_occ_nin2.tsv").selectDimValueEqualTo("unit","NR","nace_r2","I551-I553","indic_to","B006")
				.delete("unit").delete("nace_r2").delete("indic_to");
		//data = NUTSUtils.computePopRatioFigures(data, 1000, true);
		data = NUTSUtils.computeDensityFigures(data);

		RangedClassifier cl = getClassifier(data.getQuantiles(8));
		for(int year = 2010; year<=2015; year++) {
			new NUTSMap(2, 60, "geo", data.selectDimValueEqualTo("time",year+" ").delete("time").toMap(), null)
			.makeDark()
			.setTitle(year+"")
			.setClassifier(cl)
			.make()
			//.printClassification()
			.saveAsImage(outPath + "map_"+year+".png", 1000, true, true)
			//.saveLegendAsImage(outPath + "legend.png")
			.dispose()
			;
		}

		/*HashMap<String, Double> statData =
				CSV.load("H:/methnet/geostat/out/tour_occ_nin2_nuts3.csv", "value").selectDimValueEqualTo("nace_r2", "I551-I553", "time", "2015 ")
				.delete("unit").delete("indic_to").delete("time").delete("nace_r2")
				.toMap();
		NUTSMap map = new NUTSMap("", 3, 60, "geo", statData);
		//map.show();
		map.saveAsImage("H:/desktop/map.png", 1000); */

		System.out.println("End.");
	}

}
