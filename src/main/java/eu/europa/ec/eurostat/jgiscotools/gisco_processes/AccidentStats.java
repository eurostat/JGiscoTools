/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.io.CSV;

/**
 * @author gaffuju
 *
 */
public class AccidentStats {

	public static void main(String[] args) {
		System.out.println("Start");

		String filePath = "C:\\Users\\gaffuju\\Desktop\\NUTS_3.csv";

		StatsHypercube hc = CSV.load(filePath, "Victims");
		hc.delete("C - Year");
		hc.delete("A-4 Nuts Level 3 Description");
		hc.delete("C - Country Code (ISO-2)");

		hc.delete("Fatally Injured (as reported)");
		hc.delete("Fatally Injured (at 30 days)");
		hc.delete("Seriously Injured (as reported)");
		hc.delete("Seriously Injured (at 30 days)");
		hc.delete("Slightly Injured");
		hc.delete("Injured (injury severity not known)");
		hc.delete("Injury Type Not Known");
		hc.delete("Not Injured");
		hc.delete("Injured (total)");
		hc.delete("Injured (total as reported)");

		//hc.printInfo(true);

		/*
		P-3 Person Gender
	      Female
	      Male
	      Unknown
	    A-4 Nuts Level 3 (846 dimension values)
	      ? #NA
   Dimension: R-X Area (4 dimension values)
      Motorway
      Rural
      Urban
      Unknown
   Dimension: P-2 Person Age Group (7 dimension values)
      <15
      15 - 17
      18 - 24
      25 - 49
      50 - 64
      65+
      Unknown
   Dimension: U-2 Traffic Unit type (25 dimension values)
      Agricultural tractor
      Bus
      Bus or minibus or coach or trolley
      Coach
      Goods vehicle
      Goods vehicle over 3.5t mgw
      Goods vehicle under 3.5t mgw
      Minibus
      Moped
      Motorcycle not specified
      Motorcycle over 125cc
      Motorcycle up to 125cc
      Other motor vehicle
      Other non-motor vehicle
      Passenger car
      Pedal cycle
      Pedestrian
      Ridden animal
      Road tractor
      Tram/light rail
      Trolley
      Two wheel motor vehicle
      Unknown
      quad over 50cc
      quad up to 50cc
		 */

		hc = hc.selectDimValueEqualTo("U-2 Traffic Unit type","Ridden animal");
		hc.delete("U-2 Traffic Unit type");
		hc.printInfo(true);

		//TODO compute totals by gender, age, area
		
		
		System.out.println("End");
	}

}
