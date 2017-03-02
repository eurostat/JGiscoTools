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
import org.geotools.swing.JMapFrame;
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

	private MapContent map = null;
	private int level = 3;
	private int lod = 1; //can be 1, 3, 10, 20 or 60

	public NUTSMap(){ this(3, "NUTS map"); }
	public NUTSMap(int level, String title){
		this.level = level;
		map = new MapContent();
		map.setTitle(title);
		map.getViewport().setCoordinateReferenceSystem(LAEA_CRS);
		this.setBounds(2550000.0, 7400000.0, 1200000.0, 5500000.0);
	}

	public NUTSMap setBounds(double x1, double x2, double y1, double y2) {
		map.getViewport().setBounds(new ReferencedEnvelope(y1, y2, x1, x2, LAEA_CRS ));
		return this;
	}

	public NUTSMap produce() {
		//style
		StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();
		FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();

		Stroke stroke = styleFactory.createStroke(filterFactory.literal(Color.WHITE), filterFactory.literal(1));
		Fill fill = styleFactory.createFill(filterFactory.literal(Color.GRAY));
		PolygonSymbolizer sym = styleFactory.createPolygonSymbolizer(stroke, fill, null);

		Rule rule = styleFactory.createRule();
		rule.symbolizers().add(sym);
		FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle(new Rule[]{rule});
		Style style = styleFactory.createStyle();
		style.featureTypeStyles().add(fts);

		//create and add layer
		Layer layer = new FeatureLayer(NUTSShapeFile.getShpFileNUTS().getFeatureCollection(NUTSShapeFile.getFilterLvl(level)), style);
		map.addLayer(layer);
		return this;
	}

	public NUTSMap show() {
		JMapFrame.showMap(map);
		return this;
	}


	public NUTSMap saveAsImage(final String file, final int imageWidth) {
		try {
			//prepare image
			ReferencedEnvelope mapBounds = map.getViewport().getBounds();
			Rectangle imageBounds = new Rectangle(0, 0, imageWidth, (int) Math.round(imageWidth * mapBounds.getSpan(0) / mapBounds.getSpan(1)));
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
		return this;
	}



	public static void main(String[] args) throws Exception {
		System.out.println("Start.");
		new NUTSMap().produce()
		//.show()
		.saveAsImage("H:/desktop/ex.png", 1400);

		System.out.println("End.");
	}

}
