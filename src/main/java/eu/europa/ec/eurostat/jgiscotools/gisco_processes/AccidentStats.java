/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import java.util.HashSet;

import eu.europa.ec.eurostat.java4eurostat.analysis.Operations;
import eu.europa.ec.eurostat.java4eurostat.analysis.Selection.Criteria;
import eu.europa.ec.eurostat.java4eurostat.base.Stat;
import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.io.CSV;
import eu.europa.ec.eurostat.java4eurostat.io.EurostatTSV;

/**
 * @author gaffuju
 *
 */
public class AccidentStats {

	public static void main(String[] args) {
		System.out.println("Start");

		String basePath = "E:\\workspace\\traffic_accident_map\\";

		//TODO update compress format

		//load population figures
		StatsHypercube hcPop = EurostatTSV.load(basePath+"demo_r_pjangrp3.tsv");
		//age TOTAL
		//time 2017
		//sexT
		//unit
		//geo size=5
		hcPop = hcPop.selectDimValueEqualTo("age", "TOTAL", "time", "2017", "sex", "T");
		hcPop = hcPop.shrinkDimensions();
		hcPop = hcPop.select(new Criteria() {
			@Override
			public boolean keep(Stat stat) {
				return stat.dims.get("geo").length() == 5;
			}
		});
		//hcPop.printInfo(true);

		

		//See: https://ec.europa.eu/commission/presscorner/detail/en/MEMO_19_1990


		//TODO better load procedure
		StatsHypercube hc = CSV.load(basePath+"NUTS_3.csv", "Victims");

		System.out.println(hc.getDimValues("geo"));
		System.out.println(hc.getDimValues("geo").size());
		System.out.println(hcPop.getDimValues("geo"));
		System.out.println(hcPop.getDimValues("geo").size());
		
		//System.out.println( Compacity.getCompacityIndicator(hc, false, false) );
		//1.9322945767601065E-4

		//System.out.println( Compacity.getCompacityIndicator(hc.selectDimValueEqualTo("tut", "Pedestrian"), false, false) );
		//7.5126216276403455E-6
		//System.out.println( Compacity.getCompacityIndicator(hc.selectDimValueEqualTo("tut", "Passenger car"), false, false) );

		//StatsHypercube hc__ = hc.selectDimValueEqualTo("tut", "Passenger car");
		//System.out.println(Compacity.getMaxSize(hc__));
		//System.out.println(hc__.stats.size());
		//TODO bug there - negative value

		//TODO check again
		//System.out.println( Validation.checkUnicity(hc) );

		hc.delete("C - Year");
		hc.delete("geo Description");
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
		//TODO

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

		//hc = hc.selectDimValueEqualTo("U-2 Traffic Unit type","Ridden animal");
		//hc.delete("U-2 Traffic Unit type");
		//hc.printInfo(true);

		/*/check how many with nuts=#NA
		StatsHypercube hcNA = hc.selectDimValueEqualTo("A-4 Nuts Level 3", "#NA");
		System.out.println(hc.stats.size());
		System.out.println(hcNA.stats.size());
		hc.printInfo(false);
		hcNA.printInfo(false);*/


		//dimensions to consider:
		//gender, age, 


		//filter
		hc = hc.selectValueGreaterThan(0);
		hc = hc.select(new Criteria() {
			@Override
			public boolean keep(Stat stat) {
				return !stat.dims.get("geo").equals("#NA");
			}});

		//compute totals
		hc.stats.addAll( Operations.computeSumDim(hc, "gender", "total") );
		hc.stats.addAll( Operations.computeSumDim(hc, "tut", "total") );
		hc.stats.addAll( Operations.computeSumDim(hc, "road_type", "total") );
		hc.stats.addAll( Operations.computeSumDim(hc, "age", "total") );
		//hc.stats.addAll( Operations.computeSumDim(hc, "geo", "total") );
		//hc.printInfo();

		//StatsHypercube hcT = hc.selectDimValueEqualTo("gender", "total", "tut", "total", "road_type", "total", "age", "total", "geo", "total");
		//System.out.println(hcT.stats.size());
		//System.out.println(hcT.stats.iterator().next());
		//TODO 1012328.0 - a lot. check that !!!

		//System.out.println(hc.getDimValues("age"));
		HashSet<String> genders = hc.getDimValues("gender");
		HashSet<String> tuts = hc.getDimValues("tut");
		HashSet<String> roadTypes = hc.getDimValues("road_type");
		HashSet<String> ages = hc.getDimValues("age");

		for(String gender : genders)
			for(String tut : tuts)
				for(String roadType : roadTypes)
					for(String age : ages) {
						System.out.println(gender + " *** "+tut+" *** "+roadType+" *** "+age);
						StatsHypercube hc_ = hc.selectDimValueEqualTo("gender", gender, "tut", tut, "road_type", roadType, "age", age);
						hc_.delete("gender");
						hc_.delete("tut");
						hc_.delete("road_type");
						hc_.delete("age");

						//if no data, skip
						if( hc_.stats.size() == 0 ) continue;

						//remove strange characters
						gender = gender.replace(" ", "_");
						tut = tut.replace("/", "").replace(" ", "_");
						roadType = roadType.replace(" ", "_");
						age = age.replace("-", "").replace("+", "").replace("<", "").replace(" ", "_");

						//save as CSV
						CSV.save(hc_, "val", "E:/web/traffic_map/data/"+gender+"/"+tut+"/"+roadType+"/"+age+".csv");
					}


		//TODO analyse compacity on dimension to select the most pertnent one to show

		System.out.println("End");
	}

}
