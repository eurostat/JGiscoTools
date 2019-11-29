/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.deprecated;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.geotools.brewer.color.ColorBrewer;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.function.Classifier;
import org.geotools.filter.function.ExplicitClassifier;
import org.geotools.filter.function.RangedClassifier;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.swing.JMapFrame;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @author Julien Gaffuri
 *
 */
public class StatisticalMap {
	//TODO background layers (countries)
	//TODO legend labels - fix extreme values
	//TODO generic functions - fix
	//TODO nice classes - nice labels
	//TODO show scale bar?

	protected MapContent map = null;

	protected SimpleFeatureCollection statisticalUnits = null;
	private String idPropName = "ID";

	public HashMap<String, Double> statData = null;

	public Classifier classifier;
	public String classifierName = "Quantile"; //EqualInterval, Jenks, Quantile, StandardDeviation, UniqueInterval
	public int classNb = 9;
	public String paletteName = "YlOrRd"; //"OrRd"; //see http://colorbrewer2.org
	public Color[] colors = null;

	public Color noDataColor = Color.GRAY;
	public StatisticalMap setNoDataColor(Color noDataColor){ this.noDataColor=noDataColor; return this; }

	private SimpleFeatureCollection borders = null;
	public Color borderColor = Color.WHITE;
	public double borderWidth = 0.8;

	//public Color imgBckgrdColor = Color.WHITE;
	//public Color imgBckgrdColor = new Color(240,248,255); //aliceblue
	public Color imgBckgrdColor = new Color(173,216,230); //lightblue
	//public Color imgBckgrdColor = new Color(70,130,180); //steelblue
	public Color fontColor = Color.BLACK;
	public int fontSize = 30;
	public String fontFamily = "Arial";
	public int fontStrength = Font.PLAIN;

	public int legendWidth = 200;
	public int legendHeightPerClass = 20;
	public int legendPadding = 5;
	public int legendRoundingDecimalNB = 3;


	public SimpleFeatureCollection graticulesFS = null;
	public Color graticulesColor = new Color(200,200,200);
	public double graticulesWidth = 0.4;

	public StatisticalMap setGraticule(){
		graticulesFS = NUTSShapeFile.getGraticules().getFeatureCollection(NUTSShapeFile.GRATICULE_FILTER_5);
		return this;
	}

	public StatisticalMap(SimpleFeatureCollection statisticalUnits, String idPropName, HashMap<String, Double> statData, SimpleFeatureCollection borders, Classifier classifier){
		this.statisticalUnits = statisticalUnits;
		this.idPropName = idPropName;
		this.statData = statData;
		this.borders = borders;
		this.classifier = classifier;

		this.map = new MapContent();
		if(statisticalUnits != null){
			CoordinateReferenceSystem crs = statisticalUnits.getSchema().getCoordinateReferenceSystem();
			this.map.getViewport().setCoordinateReferenceSystem(crs);
			this.map.getViewport().setBounds(statisticalUnits.getBounds());
		}
	}

	public StatisticalMap dispose() { this.map.dispose(); return this; }
	public StatisticalMap setTitle(String title) { map.setTitle(title); return this; }
	public StatisticalMap setClassifier(Classifier classifier) { this.classifier = classifier; return this; }
	public StatisticalMap show() { JMapFrame.showMap(map); return this; }

	public StatisticalMap setBounds(double x1, double x2, double y1, double y2){ return setBounds(x1,x2,y1,y2,this.statisticalUnits.getSchema().getCoordinateReferenceSystem()); }
	public StatisticalMap setBounds(double x1, double x2, double y1, double y2, CoordinateReferenceSystem crs){
		this.map.getViewport().setBounds(new ReferencedEnvelope(x1,x2,y1,y2, crs));
		return this;
	}

	public StatisticalMap make(){

		//graticules
		if(graticulesFS != null)
			map.addLayer( new FeatureLayer(graticulesFS, MappingUtils.getLineStyle(this.graticulesColor, this.graticulesWidth)) );

		//join stat data, if any
		String valuePropName = "Prop"+((int)(1000000*Math.random()));
		SimpleFeatureCollection[] out = MappingUtils.join(statisticalUnits, this.idPropName, this.statData, valuePropName);
		SimpleFeatureCollection fs = out[0], fsNoDta = out[1];

		StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();
		FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();

		//add layer for no data
		map.addLayer( new FeatureLayer(fsNoDta, MappingUtils.getPolygonStyle(noDataColor, null)) );

		//add layer for data
		if(fs.size()>0){
			Stroke stroke = styleFactory.createStroke( filterFactory.literal(Color.WHITE), filterFactory.literal(0.0001), filterFactory.literal(0));
			this.colors = ColorBrewer.instance().getPalette(paletteName).getColors(classNb);
			if(this.classifier == null) this.classifier = MappingUtils.getClassifier(fs, valuePropName, classifierName, classNb);
			Style s = MappingUtils.getThematicStyle(fs, valuePropName, this.classifier, this.colors, stroke);
			map.addLayer( new FeatureLayer(fs, s) );
		}

		//borders
		if(this.borders != null)
			map.addLayer( new FeatureLayer(this.borders, MappingUtils.getLineStyle(this.borderColor, this.borderWidth)) );

		return this;
	}

	public StatisticalMap makeDark() {
		this.borderColor = Color.BLACK;
		this.imgBckgrdColor = Color.BLACK;
		this.fontColor = Color.WHITE;
		this.graticulesColor = new Color(40,40,40);
		return this;
	}

	public StatisticalMap printClassification() {
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


	public StatisticalMap saveAsImage(String file) { return saveAsImage(file, 1000, true, true); }
	public StatisticalMap saveAsImage(String file, int imageWidth, boolean withTitle, boolean withLegend) {
		try {
			//prepare image
			ReferencedEnvelope mapBounds = this.map.getViewport().getBounds();
			Rectangle imageBounds = new Rectangle(0, 0, imageWidth, (int) Math.round(imageWidth * mapBounds.getSpan(1) / mapBounds.getSpan(0)));
			BufferedImage image = new BufferedImage(imageBounds.width, imageBounds.height, BufferedImage.TYPE_INT_RGB);
			Graphics2D gr = image.createGraphics();
			gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			//draw background
			gr.setPaint(imgBckgrdColor);
			gr.fill(imageBounds);

			//paint map
			GTRenderer renderer = new StreamingRenderer();
			renderer.setMapContent(this.map);
			renderer.setJava2DHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON ));
			Map<Object,Object> rendererParams = new HashMap<Object,Object>();
			rendererParams.put("optimizedDataLoadingEnabled", Boolean.FALSE );
			renderer.setRendererHints( rendererParams );
			renderer.paint(gr, imageBounds, mapBounds);

			//TODO bug here. Title and legend do not draw !
			/*gr.setColor(fontColor);
			gr.setFont(new Font(fontFamily, fontStrength, fontSize));
			gr.drawString("AAAAAAAAA", 50, 50);*/

			//write title
			if(withTitle && this.map.getTitle()!=null) {
				//System.out.println( this.map.getTitle() );
				gr.setColor(fontColor);
				gr.setFont(new Font(fontFamily, fontStrength, fontSize));
				gr.drawString(this.map.getTitle(), 10, fontSize+5);
			}

			//draw legend
			if(withLegend && this.classifier !=null) {
				//System.out.println( this.classifier );
				MappingUtils.drawLegend(gr, (RangedClassifier)this.classifier, this.colors, this.legendRoundingDecimalNB, imageWidth-legendWidth-legendPadding, legendPadding, legendWidth, legendHeightPerClass, legendPadding);
			}

			ImageIO.write(image, "png", new File(file));
		} catch (Exception e) { e.printStackTrace(); }
		return this;
	}

	public StatisticalMap saveLegendAsImage(String file) { saveLegendAsImage(file, 3, 100, 20, 5); return this; }
	public StatisticalMap saveLegendAsImage(String file, int decimalNB, int width, int heightPerClass, int padding) {
		MappingUtils.saveAsImage((RangedClassifier) this.classifier, this.colors, file, decimalNB, width, heightPerClass, padding);
		return this;
	}

}
