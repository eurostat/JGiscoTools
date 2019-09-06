/**
 * 
 */
package eu.ec.estat.geostat.core;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;

import javax.imageio.ImageIO;

import org.geotools.brewer.color.StyleGenerator;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.function.Classifier;
import org.geotools.filter.function.RangedClassifier;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;

import eu.europa.ec.eurostat.java4eurostat.util.Util;

/**
 * 
 * Some generic functions for mapping.
 * 
 * @author Julien Gaffuri
 *
 */
public class MappingUtils {
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



}
