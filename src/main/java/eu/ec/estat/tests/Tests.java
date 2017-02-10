/**
 * 
 */
package eu.ec.estat.tests;

import eu.ec.estat.java4eurostat.base.StatsHypercube;
import eu.ec.estat.java4eurostat.io.EurobaseIO;
import eu.ec.estat.java4eurostat.io.EurostatTSV;

/**
 * @author julien Gaffuri
 *
 */
public class Tests {

	public static void main(String[] args) {




		//download/update data for tourism
		EurobaseIO.update("H:/eurobase/", "tour_occ_nim", "tour_occ_nin2", "tour_occ_nin2d", "tour_occ_nin2c");

		System.out.println("Loading...");
		StatsHypercube hc = EurostatTSV.load("H:/eurobase/tour_occ_nin2.tsv");
		hc.printInfo();

		//load NUTS regions
		//

	}

}
