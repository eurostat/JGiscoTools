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

import javax.imageio.ImageIO;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.referencing.CRS;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * 
 * Library to produce NUTS based maps.
 * 
 * @author Julien Gaffuri
 *
 */
public class NUTSMap {
	//https://github.com/geotools/geotools/blob/master/docs/src/main/java/org/geotools/tutorial/style/StyleLab.java
	//http://docs.geotools.org/latest/userguide/library/render/gtrenderer.html
	//http://docs.geotools.org/latest/userguide/library/render/index.html
	//http://gis.stackexchange.com/questions/123903/how-to-create-a-map-and-save-it-to-an-image-with-geotools

	private static CoordinateReferenceSystem LAEA_CRS = null;
	static{
		try { LAEA_CRS = CRS.decode("EPSG:3035"); } catch (Exception e) { e.printStackTrace(); }
	}

	MapContent map = null;

	public NUTSMap(){
		this("NUTS map");
	}

	public NUTSMap(String title){
		map = new MapContent();
		map.setTitle(title);
		map.getViewport().setCoordinateReferenceSystem(LAEA_CRS);
		map.getViewport().setBounds(new ReferencedEnvelope(2500000.0,5600000.0,1700000.0,4700000.0, LAEA_CRS ));
	}


	public void produce() {

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
		Layer layer = new FeatureLayer(NUTSShapeFile.getShpFileNUTS().getFeatureCollection(NUTSShapeFile.getFilterLvl(2)), style);
		map.addLayer(layer);

		//
		//JMapFrame.showMap(map);
		saveImage(map, "H:/desktop/ex.png", 800);
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
