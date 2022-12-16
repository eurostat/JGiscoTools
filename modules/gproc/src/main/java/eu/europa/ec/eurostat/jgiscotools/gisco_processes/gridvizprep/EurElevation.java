package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.util.ArrayList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.coverage.grid.GridCoverage2D;
import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.GeoTiffUtil;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler;

public class EurElevation {
	static Logger logger = LogManager.getLogger(EurElevation.class.getName());

	//https://docs.geotools.org/stable/userguide/library/coverage/geotiff.html
	//https://docs.qgis.org/testing/en/docs/user_manual/working_with_raster/raster_analysis.html#raster-calculator
	//https://docs.qgis.org/3.22/en/docs/gentle_gis_introduction/index.html

	//https://qgis.org/pyqgis/3.16/analysis/QgsAlignRaster.html
	//or https://qgis.org/pyqgis/3.16/analysis/QgsRasterCalculator.html

	//*******************
	//resampling with GDAL

	//gdal
	//https://gdal.org/programs/gdalwarp.html#gdalwarp
	//https://gdal.org/programs/gdalwarp.html#cmdoption-gdalwarp-tr
	//https://gdal.org/programs/gdalwarp.html#cmdoption-gdalwarp-r
	//gdalwarp eudem_dem_3035_europe.tif 1000.tif -tr 1000 1000 -r average
	//*******************


	// the target resolutions
	//private static int[] resolutions = new int[] { 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000 };
	private static int[] resolutions = new int[] { /*100000, 50000, 20000, 10000, 5000, 2000, 1000, 500,*/ 200 /*, 100*/ };
	private static String basePath = "/home/juju/Bureau/gisco/elevation/EU_DEM_mosaic_1000K/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) throws Throwable {
		logger.info("Start");
		tiling();
		logger.info("End");
	}



	// tile all resolutions
	private static void tiling() {

		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");

			String f = basePath + res+".tif";

			logger.info("Load geoTiff");
			GridCoverage2D coverage = GeoTiffUtil.getGeoTIFFCoverage(f);

			logger.info("Load grid cells");
			ArrayList<Map<String, String>> cells = GeoTiffUtil.loadCells(coverage, new String[] {"elevation"}, (v)->{ return v[0]==0 || Double.isNaN(v[0]); }, true );
			logger.info(cells.size());

			//logger.info("Round");
			//for(Map<String, String> cell : cells)
			//	cell.put("elevation", "" + (int)Double.parseDouble(cell.get("elevation")));

			logger.info("Build tiles");
			GridTiler gst = new GridTiler(cells, "GRD_ID", new Coordinate(0, 0), 128);

			gst.createTiles();
			logger.info(gst.getTiles().size() + " tiles created");

			logger.info("Save");
			String outpath = basePath + "out/tiled/" + res + "m";
			gst.saveCSV(outpath);
			gst.saveTilingInfoJSON(outpath, "EU DEM Europe elevation " + res + "m");
		}
	}












	/*
	private static void resampleTiff(String inTiff, String outCSV, int ratio, String outProp) throws Throwable {

		//get coverage from tiff file
		File file = new File(inTiff);
		AbstractGridFormat format = GridFormatFinder.findFormat( file );
		GridCoverage2DReader reader = format.getReader( file );
		GridCoverage2D coverage = (GridCoverage2D) reader.read(null);

		//get envelopes
		Envelope envG = coverage.getEnvelope();
		GridEnvelope2D env = coverage.getGridGeometry().getGridRange2D();
		//System.out.println(envG);
		//System.out.println(env);

		//compute and check resolution
		double resX = (envG.getMaximum(0) - envG.getMinimum(0)) / env.getWidth();
		double resY = (envG.getMaximum(1) - envG.getMinimum(1)) / env.getHeight();
		if(resX != resY)
			throw new Error("Different X/Y resolutions: "+resX + " and "+resY);
		//System.out.println(resX);

		//
		int resT = (int) (ratio * resX);
		//logger.info("Resampling from "+resX+" to "+resT);

		//output
		Collection<Map<String, String>> data = new ArrayList<>();

		int nb = 1;
		int ratio2 = (int) (ratio*0.5);
		int[] dest = new int[nb];
		//GridCoordinates2D gco = new GridCoordinates2D(0,0);

		IntStream.rangeClosed(0, env.width/ratio -1).parallel().forEach(i -> {
			for(int j=0; j<env.height/ratio; j++){

				//sample point
				int iS = i*ratio + ratio2,
						jS = j*ratio + ratio2;

				//find how to boost that. Index ?
				//GridCoordinates2D gc = new GridCoordinates2D(iS,jS);
				//gco.setLocation(iS,jS);
				//System.out.println(gco);
				coverage.evaluate(new GridCoordinates2D(iS,jS), dest);
				int v = dest[0];
				if(v==0) continue;
				//if(v<0) System.out.println(v);
				//System.out.println(v);

				int x = (int)(envG.getMinimum(0) + i*resT);
				int y = (int)(envG.getMaximum(1) - (j+1)*resT);
				//GridCell gc = new GridCell("3035", resT, x, y);
				//String id = GridCell.getGridCellId("3035", resT, Coordinate(x,y))
				//System.out.println(gc.getId());

				Map<String, String> d = new HashMap<>();
				d.put("GRD_ID", "CRS3035RES"+resT+"m"+"N"+y+"E"+x);
				//d.put("x", x + "");
				//d.put("y", y + "");
				d.put(outProp, v + "");
				data.add(d);
			}
		});

		logger.info("save " + data.size());
		CSVUtil.save(data, outCSV);
	}
	 */

}
