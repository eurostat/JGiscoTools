/**
 * 
 */
package eu.ec.estat.geostat;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;

import javax.imageio.ImageIO;

import org.geotools.filter.function.Classifier;
import org.geotools.filter.function.ExplicitClassifier;
import org.geotools.filter.function.RangedClassifier;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.MapContent;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.swing.JMapFrame;

/**
 * @author Julien Gaffuri
 *
 */
public class StatisticalMap {
	//TODO legend labels - fix extreme values
	//TODO generic functions - fix
	//TODO nice classes - nice labels
	//TODO show scale bar?

	protected MapContent map = null;
	public HashMap<String, Double> statData = null;
	public String propName = null; //the property to map

	public Classifier classifier;
	public String classifierName = "Quantile"; //EqualInterval, Jenks, Quantile, StandardDeviation, UniqueInterval
	public int classNb = 9;
	public String paletteName = "YlOrRd"; //"OrRd"; //see http://colorbrewer2.org
	public Color[] colors = null;

	public Color borderColor = Color.WHITE;

	public StatisticalMap setBounds(double x1, double x2, double y1, double y2) {
		this.map.getViewport().setBounds(new ReferencedEnvelope(y1, y2, x1, x2, this.map.getCoordinateReferenceSystem() ));
		return this;
	}

	public StatisticalMap dispose() { this.map.dispose(); return this; }
	public StatisticalMap setTitle(String title) { map.setTitle(title); return this; }
	public StatisticalMap setClassifier(Classifier classifier) { this.classifier = classifier; return this; }
	public StatisticalMap show() { JMapFrame.showMap(map); return this; }

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

	public int legendWidth = 200;
	public int legendHeightPerClass = 20;
	public int legendPadding = 5;
	public int legendRoundingDecimalNB = 3;


	public StatisticalMap(String propName, HashMap<String, Double> statData, Classifier classifier){
		this.statData = statData;
		this.propName = propName;
		this.classifier = classifier;
		this.map = new MapContent();
	}

	public StatisticalMap make(){
		//TODO
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


	public StatisticalMap saveAsImage(final String file) { return saveAsImage(file, 1000, true, true); }
	public StatisticalMap saveAsImage(final String file, final int imageWidth, boolean withTitle, boolean withLegend) {
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
			if(withTitle && map.getTitle()!=null){
				gr.setColor(fontColor);
				gr.setFont(new Font(fontFamily, fontStrength, fontSize));
				gr.drawString(map.getTitle(), 10, fontSize+5);
			}

			if(withLegend)
				MappingUtils.drawLegend(gr, (RangedClassifier)this.classifier, this.colors, this.legendRoundingDecimalNB, imageWidth-legendWidth-legendPadding, legendPadding, legendWidth, legendHeightPerClass, legendPadding);

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
