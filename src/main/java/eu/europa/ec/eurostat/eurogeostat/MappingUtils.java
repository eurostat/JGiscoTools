/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.geotools.brewer.color.StyleGenerator;
import org.geotools.brewer.styling.builder.FillBuilder;
import org.geotools.brewer.styling.builder.FontBuilder;
import org.geotools.brewer.styling.builder.HaloBuilder;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.function.Classifier;
import org.geotools.filter.function.RangedClassifier;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.MapContent;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Halo;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.TextSymbolizer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;

import eu.europa.ec.eurostat.eurogeostat.util.ProjectionUtil;
import eu.europa.ec.eurostat.eurogeostat.util.Util;

/**
 * 
 * Some generic functions for mapping.
 * 
 * @author Julien Gaffuri
 *
 */
public class MappingUtils {
	//See: http://docs.geotools.org/stable/userguide/library/referencing/order.html
	//TODO: System.setProperty("org.geotools.referencing.forceXY", "true");

	//TODO gif animation on time
	//TODO small multiple

	//compute join
	//TODO improve
	public static SimpleFeatureCollection[] join(SimpleFeatureCollection fc, String idPropName, HashMap<String, Double> statData, String valuePropName) {
		try {
			String geomType = fc.getSchema().getGeometryDescriptor().getType().getName().toString();
			SimpleFeatureType ft = DataUtilities.createType("stat_joined", idPropName+":String,the_geom:"+geomType+","+valuePropName+":Double"); //:srid=3035
			ft = DataUtilities.createSubType(ft, null, fc.getSchema().getCoordinateReferenceSystem());
			DefaultFeatureCollection fcRGJoin = new DefaultFeatureCollection("fc_joined", ft);
			DefaultFeatureCollection fcRGNoData = new DefaultFeatureCollection("fc_nodata", ft);

			SimpleFeatureIterator it = fc.features();
			while (it.hasNext()) {
				SimpleFeature f = it.next();
				String id = (String) f.getAttribute(idPropName);
				Double value = null;
				if(statData != null) value = statData.get(id);
				if(value==null) {
					SimpleFeature f2 = SimpleFeatureBuilder.build( ft, new Object[]{ id, f.getAttribute("the_geom"), 0 }, null);
					fcRGNoData.add(f2);
				} else {
					SimpleFeature f2 = SimpleFeatureBuilder.build( ft, new Object[]{ id, f.getAttribute("the_geom"), value.doubleValue() }, null);
					fcRGJoin.add(f2);
				}
			}
			it.close();
			return new SimpleFeatureCollection[]{fcRGJoin, fcRGNoData};
		} catch (Exception e) { e.printStackTrace(); }
		return null;
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
			gr.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, fontSize));
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


	public static Style getThematicStyle(SimpleFeatureCollection fc, String propName, Classifier classifier, Color[] colors, Stroke stroke){
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

	public static Style getLineStyle(Color col, double width){
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


	public static Stroke getStroke(Color strokeCol, double strokeWidth) {
		StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();
		FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();
		return styleFactory.createStroke( filterFactory.literal(strokeCol), filterFactory.literal(strokeWidth));
	}


	public static Style getPolygonStyle(Color fillCol, Color strokeCol, double strokeWidth){
		return getPolygonStyle(fillCol, strokeWidth>0?getStroke(strokeCol,strokeWidth):null);
	}


	public static Style getPolygonStyle(Color fillCol, Stroke stroke){
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



	//http://docs.geoserver.org/latest/en/user/styling/sld/cookbook/polygons.html
	//test TextSymbolizerBuilder
	public static Style getTextStyle(String propName, Color fontColor, int fontSize, String fontFamilyName, double haloRadius, Color haloColor) {
		StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();
		FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();

		TextSymbolizer txtSymb = styleFactory.createTextSymbolizer();
		txtSymb.setLabel( filterFactory.property(propName) );
		txtSymb.setFill(new FillBuilder().color(fontColor).build());
		//txtSymb.setFont(new Font("Arial",java.awt.Font.BOLD,fontSize)); //use FontBuilder
		txtSymb.setFont(new FontBuilder().familyName(fontFamilyName)/*.weightName("BOLD")*/.size(fontSize).build());

		if(haloRadius>0) {
			Halo halo = new HaloBuilder().radius(haloRadius).build();
			halo.setFill(new FillBuilder().color(haloColor).build());
			txtSymb.setHalo(halo);
		}

		/*TextSymbolizer txtSymb = new TextSymbolizerBuilder()
				.labelText(propName)
				.build();*/

		Rule rule = styleFactory.createRule();
		rule.symbolizers().add(txtSymb);
		FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle(new Rule[]{rule});
		Style s = styleFactory.createStyle();
		s.featureTypeStyles().add(fts);
		return s;
	}






	public static class TitleDisplayParameters{
		public int fontSize = 32;
		public Color fontColor = Color.BLACK;
		public String fontFamily = "Arial";
		public int fontStrength = Font.BOLD;
	}

	public static void saveAsImage(MapContent map, double scaleDenom, Color imgBckgrdColor, int marginPixNb, TitleDisplayParameters titleParams, String outFolder, String outFileName) {
		BufferedImage image = getImage(map, scaleDenom, imgBckgrdColor, marginPixNb, titleParams);
		try { ImageIO.write(image, "png", new File(outFolder+outFileName)); }
		catch (IOException e) { e.printStackTrace(); }
	}

	public static BufferedImage getImage(MapContent map, double scaleDenom, Color imgBckgrdColor, int marginPixNb, TitleDisplayParameters titleParams) {
		try {
			double marginM = marginPixNb*ProjectionUtil.METERS_PER_PIXEL*scaleDenom;
			ReferencedEnvelope mapBounds = map.getViewport().getBounds();
			mapBounds.expandBy(marginM, marginM);
			Rectangle imageBounds = MappingUtils.getImageBounds(mapBounds, scaleDenom, marginPixNb);
			BufferedImage image = new BufferedImage(imageBounds.width, imageBounds.height, BufferedImage.TYPE_INT_RGB);
			Graphics2D gr = image.createGraphics();
			gr.setPaint(imgBckgrdColor);
			gr.fill(imageBounds);
			MappingUtils.getRenderer(map).paint(gr, imageBounds, mapBounds);
			if(titleParams != null && map.getTitle()!=null) {
				gr.setColor(titleParams.fontColor);
				gr.setFont(new Font(titleParams.fontFamily, titleParams.fontStrength, titleParams.fontSize));
				gr.drawString(map.getTitle(), 10, titleParams.fontSize+5);
			}
			return image;
		} catch (Exception e) { e.printStackTrace(); return null; }
	}




	public static Rectangle getImageBounds(ReferencedEnvelope mapBounds, double scaleDenom, int marginPixNb) {
		int imageWidth = (int) (mapBounds.getWidth() / scaleDenom / ProjectionUtil.METERS_PER_PIXEL +1) + 2*marginPixNb;
		int imageHeight = (int) (mapBounds.getHeight() / scaleDenom / ProjectionUtil.METERS_PER_PIXEL +1) + 2*marginPixNb;
		/*int imageWidth = 1000;
		int imageHeight = (int) Math.round(imageWidth * mapBounds.getSpan(1) / mapBounds.getSpan(0));*/
		return new Rectangle(0, 0, imageWidth, imageHeight);
	}



	public static StreamingRenderer getRenderer() {
		StreamingRenderer renderer = new StreamingRenderer();
		renderer.setGeneralizationDistance(-1);
		renderer.setJava2DHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON ));
		Map<Object,Object> renderingHints = new HashMap<Object,Object>();
		renderingHints.put("optimizedDataLoadingEnabled", Boolean.TRUE);
		renderingHints.put(StreamingRenderer.TEXT_RENDERING_KEY, StreamingRenderer.TEXT_RENDERING_STRING);
		renderer.setRendererHints( renderingHints );
		return renderer;
	}
	public static StreamingRenderer getRenderer(MapContent map) {
		StreamingRenderer renderer = getRenderer();
		renderer.setMapContent(map);
		return renderer;
	}

}
