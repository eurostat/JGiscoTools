/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import eu.europa.ec.eurostat.java4eurostat.analysis.Operations;
import eu.europa.ec.eurostat.java4eurostat.analysis.Selection.Criteria;
import eu.europa.ec.eurostat.java4eurostat.base.Stat;
import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.io.CSV;
import eu.europa.ec.eurostat.java4eurostat.io.CSVAsHashMap;

/**
 * @author gaffuju
 *
 */
public class AccidentStats {


	public static void main(String[] args) {
		System.out.println("Start");

		String basePath = "E:\\workspace\\traffic_accident_map\\";
		/*
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
		 */


		//See: https://ec.europa.eu/commission/presscorner/detail/en/MEMO_19_1990

		
		ArrayList<HashMap<String, String>> data = CSVAsHashMap.load(basePath+"NUTS_3.csv");
		
		System.out.println(data.size());
		HashMap<String, String> elt = data.iterator().next();
		System.out.println(elt);
		System.out.println(elt.keySet());
		
		
		if(true) return;

		
		StatsHypercube hc = CSV.load(basePath+"NUTS_3.csv", "Victims");
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
		//TODO include that as a new dimension

		hc.delete("C - Year");
		hc.delete("geo Description");
		hc.delete("C - Country Code (ISO-2)");

		//hc.printInfo(true);

		//analyse compacity

		//System.out.println( Compacity.getCompacityIndicator(hc, false, false) );
		//0.06797196892941573
		//System.out.println( Compacity.getCompacityIndicator(hc, "tut", "Passenger car", false, false) );
		//0.4037205898908027

		/*
		ArrayList<DimensionValueCompacity> ca = Compacity.getDimensionValuesByCompacity(hc, false, false);
		for(DimensionValueCompacity dvc : ca)
			if(!"geo".equals(dvc.dimLabel)) System.out.println(dvc);*/

		//check unicity
		//HashMap<String, Integer> un = Validation.checkUnicity(hc);
		//for(Entry<String, Integer> e : un.entrySet())
		//	System.out.println( e.getKey() + "   " + e.getValue() );

		//filter
		hc = hc.selectValueGreaterThan(0);
		hc = hc.select(new Criteria() {
			@Override
			public boolean keep(Stat stat) {
				return !stat.dims.get("geo").equals("#NA");
			}});


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


		//dimensions to consider:
		//gender, age, 

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

		System.out.println("End");
	}

}
