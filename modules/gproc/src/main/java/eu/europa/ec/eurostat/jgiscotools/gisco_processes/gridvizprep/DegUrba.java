package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class DegUrba {
	static Logger logger = LogManager.getLogger(DegUrba.class.getName());

	// the target resolutions
	private static int[] resolutions = new int[] { 1000, 2000, 5000, 10000, 20000, 50000, 100000 };
	private static String basePath = "/home/juju/Bureau/gisco/degurba/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) {
		logger.info("Start");
		format();
		//aggregate();
		//tiling();
		logger.info("End");
	}

	private static void format() {
		logger.info("Load");
		ArrayList<Map<String, String>> data = CSVUtil.load(basePath + "DGURBA_LEVEL2.txt",
				CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(";".charAt(0)));
		logger.info(data.size());
		logger.info(data.get(0).keySet());
		//[GRD_ID, DGURBA_LVL2]

		/*
		 uc   30 = Urban Centres <==> clusters (with smoothed boundaries) of contigous cells (4-connectivity cluster) with a total population >= 50,000 inh., each cell having a
		         density >= 1,500 inh./sqKm or, optionally, a built-up surface share > 50%.  
		 du   23 = Dense Urban Clusters <==> density >= 1500 inh./sqKm and population >= 5000 and < 50000.
		 sdu   22 = Semi-Dense Urban Clusters <==> (density >= 300 AND population >= 5000) AND at least 2-km away from borders of Urban Centres or Dense Urban Clusters.
		 sbu   21 = Suburban Cells <==> (density >= 300 AND population >= 5000)  AND  which are not Semi-Dense Urban Clusters.
		 r   13 = Rural Clusters <==> density >= 300 AND population >= 500 and < 5000.
		 lr  12 = Low Density Rural Grid Cells <==> (density >= 50 AND density < 300)   OR   (density >= 300 AND population < 500 ).
		 vlr   11 = Very Low Density Rural Grid Cells <==> density >= 0 AND < 50.
		 */

		for (Map<String, String> d : data) {
			String du = d.get("DGURBA_LVL2");
			System.out.println(du);

			d.put("uc", du.equals("30")? "1" : "0");
			d.put("du", du.equals("23")? "1" : "0");
			d.put("sdu", du.equals("22")? "1" : "0");
			d.put("sbu", du.equals("21")? "1" : "0");
			d.put("r", du.equals("13")? "1" : "0");
			d.put("lr", du.equals("12")? "1" : "0");
			d.put("vlr", du.equals("11")? "1" : "0");

			//clean
			d.remove("DGURBA_LVL2");
		}



	}


}
