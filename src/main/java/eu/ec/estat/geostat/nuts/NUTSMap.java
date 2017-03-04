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
	//TODO colors http://docs.geotools.org/stable/userguide/extension/brewer/index.html

	//TODO show properly borders depending on nuts level

	//TODO thematic mapping
	//http://docs.geotools.org/latest/tutorials/map/style.html   --   http://docs.geotools.org/latest/tutorials/map/style.html#controlling-the-rendering-process
	//http://leafletjs.com/examples/choropleth/
	
	//TODO legend
	//http://gis.stackexchange.com/questions/22962/create-a-color-scale-legend-for-choropleth-map-using-geotools-or-other-open-sou
	
	//TODO show other countries
	
	
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

	public NUTSMap(){ this(3, 1, "NUTS map"); }
	public NUTSMap(int level, int lod, String title){
		this.level = level;
		this.lod = lod;
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
		StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();
		FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();

		//RG style
		Style RGStyle;
		{
			//Stroke stroke = styleFactory.createStroke(filterFactory.literal(Color.WHITE), filterFactory.literal(1));
			Fill fill = styleFactory.createFill(filterFactory.literal(Color.GRAY));
			PolygonSymbolizer polSymb = styleFactory.createPolygonSymbolizer(/*stroke*/null, fill, null);
			Rule rule = styleFactory.createRule();
			rule.symbolizers().add(polSymb);
			FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle(new Rule[]{rule});
			RGStyle = styleFactory.createStyle();
			RGStyle.featureTypeStyles().add(fts);
		}

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
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "RG").getFeatureCollection(NUTSShapeFile.getFilterRGLevel(level)), RGStyle) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection(), BNStyle) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "JOIN").getFeatureCollection(NUTSShapeFile.getFilterSepaJoinLoD(this.lod)), sepaJoinStyle) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "SEPA").getFeatureCollection(NUTSShapeFile.getFilterSepaJoinLoD(this.lod)), sepaJoinStyle) );
		}
		break;
		case 1:
		{
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "RG").getFeatureCollection(NUTSShapeFile.getFilterRGLevel(level)), RGStyle) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection(), BNStyle) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "JOIN").getFeatureCollection(NUTSShapeFile.getFilterSepaJoinLoD(this.lod)), sepaJoinStyle) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "SEPA").getFeatureCollection(NUTSShapeFile.getFilterSepaJoinLoD(this.lod)), sepaJoinStyle) );
		}
		break;
		case 2:
		{
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "RG").getFeatureCollection(NUTSShapeFile.getFilterRGLevel(level)), RGStyle) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection(), BNStyle) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "JOIN").getFeatureCollection(NUTSShapeFile.getFilterSepaJoinLoD(this.lod)), sepaJoinStyle) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "SEPA").getFeatureCollection(NUTSShapeFile.getFilterSepaJoinLoD(this.lod)), sepaJoinStyle) );
		}
		break;
		case 3:
		{
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "RG").getFeatureCollection(NUTSShapeFile.getFilterRGLevel(level)), RGStyle) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection(), BNStyle) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "JOIN").getFeatureCollection(NUTSShapeFile.getFilterSepaJoinLoD(this.lod)), sepaJoinStyle) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "SEPA").getFeatureCollection(NUTSShapeFile.getFilterSepaJoinLoD(this.lod)), sepaJoinStyle) );
		}
		break;
		}
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

		new NUTSMap(3,20,"").produce().show();

		/*new NUTSMap(3,1,"").produce().saveAsImage("H:/desktop/ex3_1.png", 1400);
		new NUTSMap(3,3,"").produce().saveAsImage("H:/desktop/ex3_3.png", 1400);
		new NUTSMap(3,10,"").produce().saveAsImage("H:/desktop/ex3_10.png", 1400);
		new NUTSMap(3,20,"").produce().saveAsImage("H:/desktop/ex3_20.png", 1400);
		new NUTSMap(3,60,"").produce().saveAsImage("H:/desktop/ex3_60.png", 1400);*/

		System.out.println("End.");
	}

}
