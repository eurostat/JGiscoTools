/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.io.img;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

/**
 * @author julien Gaffuri
 *
 */
public class GifSequenceWriterFading {

	/**
	 * @param imageFilesIn The input images
	 * @param bkgImageFileIn The input background image
	 * @param lapsMs Time between two images (in ms)
	 * @param nbFading Nb of images between two
	 * @param outGif The output file
	 */
	public static void buildGIFWithFading(String[] imageFilesIn, String bkgImageFileIn, int lapsMs, int nbFading, String outGif){
		try {
			System.out.println("Load background");
			BufferedImage bgImage = null;
			if(bkgImageFileIn!=null) bgImage = ImageIO.read(new File(bkgImageFileIn));

			System.out.println("Load and draw first image");
			BufferedImage img1 = ImageIO.read(new File(imageFilesIn[0]));
			ImageOutputStream output = new FileImageOutputStream(new File(outGif ));
			GifSequenceWriter writer = new GifSequenceWriter(output, img1.getType(), lapsMs/nbFading, true);
			int w=img1.getWidth(), h=img1.getHeight();
			writer.writeToSequence(img1);

			double step=1.0/nbFading;
			BufferedImage img2;
			for(int i=1;i<imageFilesIn.length;i++){
				System.out.println(imageFilesIn[i-1]+" to "+imageFilesIn[i]);
				img2 = ImageIO.read(new File(imageFilesIn[i]));
				for(double f=step; f<=1.0; f+=step){
					//f within [0,1]

					BufferedImage img = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
					Graphics2D gr = (Graphics2D)img.getGraphics();
					gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					if(bgImage!=null) gr.drawImage(bgImage, 0, 0, null);
					for(int m=0;m<w;m++){
						for(int n=0;n<h;n++){
							//read pixel of img1
							int col1 = img1.getRGB(m,n);
							int r1 = (col1 & 0x00ff0000) >> 16;
						int g1 = (col1 & 0x0000ff00) >> 8;
				int b1 = col1 & 0x000000ff;
				int a1 = (col1>>24) & 0xff;

				//read pixel of img2
				int col2 = img2.getRGB(m,n);
				int r2 = (col2 & 0x00ff0000) >> 16;
				int g2 = (col2 & 0x0000ff00) >> 8;
				int b2 = col2 & 0x000000ff;
				int a2 = (col2>>24) & 0xff;

				//draw pixel
				Color col=new Color((int)((1-f)*r1+f*r2), (int)((1-f)*g1+f*g2), (int)((1-f)*b1+f*b2), (int)((1-f)*a1+f*a2));
				img.setRGB(m, n, col.hashCode());
						}
					}

					writer.writeToSequence(img);
				}
				img1=img2;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
