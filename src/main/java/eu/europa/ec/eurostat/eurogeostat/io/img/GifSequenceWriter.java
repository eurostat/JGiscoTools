package eu.europa.ec.eurostat.eurogeostat.io.img;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

public class GifSequenceWriter {
	private ImageWriter gifWriter;
	private ImageWriteParam imageWriteParam;
	private IIOMetadata imageMetaData;

	/**
	 * Creates a new GifSequenceWriter
	 * 
	 * @param outputStream the ImageOutputStream to be written to
	 * @param imageType one of the imageTypes specified in BufferedImage
	 * @param timeBetweenFramesMS the time between frames in miliseconds
	 * @param loopContinuously wether the gif should loop repeatedly
	 * @throws IIOException if no gif ImageWriters are found
	 *
	 * @author Elliot Kroo (elliot[at]kroo[dot]net)
	 */
	public GifSequenceWriter(
			ImageOutputStream outputStream,
			int imageType,
			int timeBetweenFramesMS,
			boolean loopContinuously) throws IIOException, IOException {
		// my method to create a writer
		gifWriter = getWriter(); 
		imageWriteParam = gifWriter.getDefaultWriteParam();
		ImageTypeSpecifier imageTypeSpecifier =
				ImageTypeSpecifier.createFromBufferedImageType(imageType);

		imageMetaData =
				gifWriter.getDefaultImageMetadata(imageTypeSpecifier,
						imageWriteParam);

		String metaFormatName = imageMetaData.getNativeMetadataFormatName();

		IIOMetadataNode root = (IIOMetadataNode)
				imageMetaData.getAsTree(metaFormatName);

		IIOMetadataNode graphicsControlExtensionNode = getNode(
				root,
				"GraphicControlExtension");

		graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
		graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
		graphicsControlExtensionNode.setAttribute(
				"transparentColorFlag",
				"FALSE");
		graphicsControlExtensionNode.setAttribute(
				"delayTime",
				Integer.toString(timeBetweenFramesMS / 10));
		graphicsControlExtensionNode.setAttribute(
				"transparentColorIndex",
				"0");

		IIOMetadataNode commentsNode = getNode(root, "CommentExtensions");
		commentsNode.setAttribute("CommentExtension", "Created by MAH");

		IIOMetadataNode appEntensionsNode = getNode(
				root,
				"ApplicationExtensions");

		IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");

		child.setAttribute("applicationID", "NETSCAPE");
		child.setAttribute("authenticationCode", "2.0");

		int loop = loopContinuously ? 0 : 1;

		child.setUserObject(new byte[]{ 0x1, (byte) (loop & 0xFF), (byte)
				((loop >> 8) & 0xFF)});
		appEntensionsNode.appendChild(child);

		imageMetaData.setFromTree(metaFormatName, root);

		gifWriter.setOutput(outputStream);

		gifWriter.prepareWriteSequence(null);
	}

	public void writeToSequence(RenderedImage img) throws IOException {
		gifWriter.writeToSequence(
				new IIOImage(
						img,
						null,
						imageMetaData),
						imageWriteParam);
	}

	/**
	 * Close this GifSequenceWriter object. This does not close the underlying
	 * stream, just finishes off the GIF.
	 */
	public void close() throws IOException {
		gifWriter.endWriteSequence();    
	}

	/**
	 * Returns the first available GIF ImageWriter using 
	 * ImageIO.getImageWritersBySuffix("gif").
	 * 
	 * @return a GIF ImageWriter object
	 * @throws IIOException if no GIF image writers are returned
	 */
	private static ImageWriter getWriter() throws IIOException {
		Iterator<ImageWriter> iter = ImageIO.getImageWritersBySuffix("gif");
		if(!iter.hasNext()) {
			throw new IIOException("No GIF Image Writers Exist");
		} else {
			return iter.next();
		}
	}

	/**
	 * Returns an existing child node, or creates and returns a new child node (if 
	 * the requested node does not exist).
	 * 
	 * @param rootNode the <tt>IIOMetadataNode</tt> to search for the child node.
	 * @param nodeName the name of the child node.
	 * 
	 * @return the child node, if found or a new node created with the given name.
	 */
	private static IIOMetadataNode getNode(
			IIOMetadataNode rootNode,
			String nodeName) {
		int nNodes = rootNode.getLength();
		for (int i = 0; i < nNodes; i++) {
			if (rootNode.item(i).getNodeName().compareToIgnoreCase(nodeName)
					== 0) {
				return((IIOMetadataNode) rootNode.item(i));
			}
		}
		IIOMetadataNode node = new IIOMetadataNode(nodeName);
		rootNode.appendChild(node);
		return(node);
	}

	/**
  public GifSequenceWriter(
       BufferedOutputStream outputStream,
       int imageType,
       int timeBetweenFramesMS,
       boolean loopContinuously) {

	 */






	
	
	
	/**
	 * @param imageFilesIn The input images
	 * @param bkgImageFileIn The input background image
	 * @param lapsMs Time between two images (in ms)
	 * @param nbFading Nb of images between two
	 * @param outGif The output file
	 */
	public static void make(String[] imageFilesIn, String bkgImageFileIn, int lapsMs, int nbFading, String outGif){
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
		} catch (Exception e) { e.printStackTrace(); }
	}









	
	public static void main(String[] args) throws Exception {
		if (args.length > 1) {
			// grab the output image type from the first image in the sequence
			BufferedImage firstImage = ImageIO.read(new File(args[0]));

			// create a new BufferedOutputStream with the last argument
			ImageOutputStream output = 
					new FileImageOutputStream(new File(args[args.length - 1]));

			// create a gif sequence with the type of the first image, 1 second
			// between frames, which loops continuously
			GifSequenceWriter writer = 
					new GifSequenceWriter(output, firstImage.getType(), 1, false);

			// write out the first image to our sequence...
			writer.writeToSequence(firstImage);
			for(int i=1; i<args.length-1; i++) {
				BufferedImage nextImage = ImageIO.read(new File(args[i]));
				writer.writeToSequence(nextImage);
			}

			writer.close();
			output.close();
		} else {
			System.out.println(
					"Usage: java GifSequenceWriter [list of gif files] [output file]");
		}
	}
}
