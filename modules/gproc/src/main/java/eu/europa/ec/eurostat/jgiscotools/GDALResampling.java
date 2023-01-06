/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author julien Gaffuri
 *
 */
public class GDALResampling {
	static Logger logger = LogManager.getLogger(GDALResampling.class.getName());

	//*******************
	//resampling with GDAL
	//https://gdal.org/programs/gdalwarp.html#gdalwarp
	//https://gdal.org/programs/gdalwarp.html#cmdoption-gdalwarp-tr
	//https://gdal.org/programs/gdalwarp.html#cmdoption-gdalwarp-r
	//gdalwarp eudem_dem_3035_europe.tif 1000.tif -tr 1000 1000 -r average
	//*******************

	//resampling
	public static void resample(String inF, String outF, int res, String method) {
		//https://gdal.org/programs/gdalwarp.html#gdalwarp
		String cmd = "gdalwarp "+ inF +" "+outF+" -tr "+res+" "+res+" -tap -r "+method+" -co TILED=YES";

		logger.info(cmd);
		CommandUtil.run(cmd);		
	}

}
