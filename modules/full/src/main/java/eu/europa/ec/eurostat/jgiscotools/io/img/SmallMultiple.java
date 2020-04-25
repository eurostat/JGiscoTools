/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.io.img;

import java.awt.Color;
import java.awt.Font;

/**
 * Library for the generation of small multiple
 * 
 * @author Julien Gaffuri
 *
 */
public class SmallMultiple {

	/*public static BufferedImage make(BufferedImage[] imgs, int rowNb, int colNb, String out) {
		//it is assumed all images have the same dimension
		//TODO
		return null;
	}*/

	private String[] imgs = null;
	private String[] imageTitles = null;
	private int colNb, rowNb;
	public SmallMultiple(String[] imgs, String[] imageTitles, int colNb, int rowNb){
		this.imgs = imgs;
		this.imageTitles = imageTitles;
		this.colNb = colNb; this.rowNb = rowNb;
	}

	public Color backgroundColor = Color.WHITE;
	public int margin = 5;

	public Color borderColor = Color.BLACK;
	public int colorSize = 3;

	public Color imageTitleFontColor = Color.BLACK;
	public int imageTitleFontSize = 12;
	public String imageTitleFontFamily = "Arial";
	public int imageTitleFontStrength = Font.PLAIN;

	private String title = null;
	public SmallMultiple setTitle(String title) { this.title=title; return this; }
	public Color titleFontColor = Color.BLACK;
	public int titleFontSize = 20;
	public String titleFontFamily = "Arial";
	public int titleFontStrength = Font.BOLD;



	//it is assumed all images have the same dimension
	public void make(String outFile) {
		int i=0;
		for(int col=1; col<=colNb; col++){
			for(int row=1; row<=rowNb; row++){
				//load first image

				if(i==0){
					//initialise data
					//get input image dimension from first image
					//create output image
				}

				//draw image
				//write title

				i++;
			}
		}
	}

}
