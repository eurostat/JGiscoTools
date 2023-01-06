/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.jgiscotools.CommandUtil;

/**
 * @author julien Gaffuri
 *
 */
public class GDALResampling {
	static Logger logger = LogManager.getLogger(GDALResampling.class.getName());

	//resampling
	public static void resample(String inF, String outF, int res, String method) {
		//https://gdal.org/programs/gdalwarp.html#gdalwarp
		String cmd = "gdalwarp "+ inF +" "+outF+" -tr "+res+" "+res+" -tap -r "+method+" -co TILED=YES";

		logger.info(cmd);
		CommandUtil.run(cmd);		
	}

}
