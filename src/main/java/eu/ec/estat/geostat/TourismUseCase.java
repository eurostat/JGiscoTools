/**
 * 
 */
package eu.ec.estat.geostat;

import java.util.Collection;
import java.util.HashMap;

import eu.ec.estat.geostat.dasymetric.DasymetricMapping;
import eu.ec.estat.geostat.io.ShapeFile;
import eu.ec.estat.geostat.nuts.NUTSMap;
import eu.ec.estat.java4eurostat.analysis.Validation;
import eu.ec.estat.java4eurostat.base.Selection;
import eu.ec.estat.java4eurostat.base.Stat;
import eu.ec.estat.java4eurostat.base.StatsHypercube;
import eu.ec.estat.java4eurostat.base.StatsIndex;
import eu.ec.estat.java4eurostat.io.CSV;
import eu.ec.estat.java4eurostat.io.EurostatTSV;

/**
 * @author julien Gaffuri
 *
 */
public class TourismUseCase {
	public static String BASE_PATH = "H:/geodata/";
	//TODO deprecated
	public static String NUTS_SHP_LVL2 = BASE_PATH + "gisco_stat_units/NUTS_2013_01M_SH/data/NUTS_RG_01M_2013_LAEA_lvl2.shp";
	//TODO deprecated
	public static String NUTS_SHP_LVL3 = BASE_PATH + "gisco_stat_units/NUTS_2013_01M_SH/data/NUTS_RG_01M_2013_LAEA_lvl3.shp";
	public static String POI_TOURISEM_SHP_BASE = BASE_PATH + "eur2016_12/mnpoi_";


	//TODO correct maps (static classification)
	//TODO show legend
	//TODO better analyse validation data
	//TODO contact tomtom guys. ask for data
	//TODO aggregate at 10km grid level
	//TODO validation of accomodation data
	//TODO focus on FR.

	public static void main(String[] args) throws Exception {
		System.out.println("Start.");

		//download/update data for tourism
		//EurobaseIO.update("H:/eurobase/", "tour_occ_nim", "tour_occ_nin2", "tour_occ_nin2d", "tour_occ_nin2c", "urb_ctour");

		//runDasymetric();
		//computeValidation();
		makeMaps();


		//E4 data validation
		//filterE4ValidationDataAggregatesNUTS2();
		//finalCheckE4ValidationData();

		System.out.println("End.");
	}


	public static void runDasymetric(){

		//load tourism data
		StatsHypercube hc = EurostatTSV.load("H:/eurobase/tour_occ_nin2.tsv",
				new Selection.And(
						new Selection.DimValueEqualTo("unit","NR"), //Number
						//new Selection.DimValueEqualTo("nace_r2","I551-I553"), //Hotels; holiday and other short-stay accommodation; camping grounds, recreational vehicle parks and trailer parks
						new Selection.DimValueEqualTo("indic_to","B006"), //Nights spent, total
						//keep only nuts 2 regions
						new Selection.Criteria() { public boolean keep(Stat stat) { return stat.dims.get("geo").length() == 4; } },
						//keep only years after 2010
						new Selection.Criteria() { public boolean keep(Stat stat) { return Integer.parseInt(stat.dims.get("time").replace(" ", "")) >= 2010; } }
						));
		hc.delete("unit"); hc.delete("indic_to");
		StatsIndex hcI = new StatsIndex(hc, "nace_r2", "time", "geo");
		//hc.printInfo();
		//hcI.print();
		hc = null;

		//save as csv
		//String time = "2015 ", outFile = "H:/methnet/geostat/out/stats_lvl2_"+time+".csv";
		//statIndexToCSV(hcI.getSubIndex(time), "NUTS_ID", outFile);

		//output structure
		StatsHypercube out = new StatsHypercube("geo", "time", "unit", "nace_r2", "indic_to");

		//go through nace codes
		for(String nace : new String[]{"I551-I553","I551","I552","I553"}){

			//create dasymetric analysis object
			DasymetricMapping dm = new DasymetricMapping(
					new ShapeFile(NUTS_SHP_LVL2).getFeatureStore(),
					"NUTS_ID",
					null,
					new ShapeFile(POI_TOURISEM_SHP_BASE+nace+".shp").getFeatureStore(),
					"ID",
					new ShapeFile(NUTS_SHP_LVL3).getFeatureStore(),
					"NUTS_ID"
					);

			//dm.computeGeoStatInitial();   CSV.save(dm.geoStatsInitialHC, "value", "H:/methnet/geostat/out/", "1_geo_to_ini_stats_"+nace+".csv");
			dm.geoStatsInitialHC = CSV.load("H:/methnet/geostat/out/POI_to_NUTS_2___"+nace+".csv", "value");

			//dm.computeGeoStatFinal();   CSV.save(dm.geoStatsFinalHC, "value", "H:/methnet/geostat/out/", "1_geo_to_fin_stats_"+nace+".csv");
			dm.geoStatsFinalHC = CSV.load("H:/methnet/geostat/out/POI_to_NUTS_3___"+nace+".csv", "value");



			//compute values for all years
			for(String time : hcI.getKeys(nace)){
				//get stat values
				dm.statValuesInitial = hcI.getSubIndex(nace, time);
				if(dm.statValuesInitial == null) continue;

				//compute values
				dm.computeFinalStat();

				//
				for(Stat s : dm.finalStatsSimplifiedHC.stats) {
					s.dims.put("time", time);
					s.dims.put("unit", "NR");
					s.dims.put("nace_r2", nace);
					s.dims.put("indic_to", "B006");
				}
				out.stats.addAll(dm.finalStatsSimplifiedHC.stats);
			}
		}
		CSV.save(out, "value", "H:/methnet/geostat/out/", "tour_occ_nin2_nuts3.csv");

	}


	private static void computeValidation() {

		//load data to validate
		StatsHypercube hc = CSV.load("H:/methnet/geostat/out/tour_occ_nin2_nuts3.csv", "value");
		hc.delete("unit");

		//load validation data
		StatsHypercube hcVal = CSV.load("H:/methnet/geostat/validation/validation_data_2013_filtered.csv", "value");

		StatsHypercube diff;

		diff = Validation.computeDifference(hcVal, hc, false, false);
		Validation.printBasicStatistics(diff);
		CSV.save(diff, "value", "H:/methnet/geostat/validation/", "validation_result_diff.csv");

		diff = Validation.computeDifference(hcVal, hc, true, false);
		Validation.printBasicStatistics(diff);
		CSV.save(diff, "value", "H:/methnet/geostat/validation/", "validation_result_diff_abs.csv");

		diff = Validation.computeDifference(hcVal, hc, false, true);
		Validation.printBasicStatistics(diff);
		CSV.save(diff, "value", "H:/methnet/geostat/validation/", "validation_result_diff_ratio.csv");

		diff = Validation.computeDifference(hcVal, hc, true, true);
		Validation.printBasicStatistics(diff);
		CSV.save(diff, "value", "H:/methnet/geostat/validation/", "validation_result_diff_abs_ratio.csv");

	}


	public static void makeMaps(){
		HashMap<String, Double> statData;
		int time = 2015;

		//nuts 2 level map
		statData = EurostatTSV.load("H:/eurobase/tour_occ_nin2.tsv").selectDimValueEqualTo("unit","NR","nace_r2","I551-I553","indic_to","B006","time",time+" ")
				.delete("unit").delete("nace_r2").delete("indic_to").delete("time").toMap();
		new NUTSMap(2, 60, "geo", statData, null).make().saveAsImage("H:/methnet/geostat/maps/map_nuts2_"+time+".png", 1000);
		//*/

		//nuts 3 level map
		statData = CSV.load("H:/methnet/geostat/out/tour_occ_nin2_nuts3.csv", "value").selectDimValueEqualTo("unit","NR","nace_r2","I551-I553","indic_to","B006","time",time+" ")
				.delete("unit").delete("nace_r2").delete("indic_to").delete("time").toMap();
		new NUTSMap(3, 60, "geo", statData, null).make().saveAsImage("H:/methnet/geostat/maps/map_result_nuts3_"+time+".png", 1000);
		//*/

		//validation data
		for(int time_ = 2005; time_<= 2013; time_++){
			statData = CSV.load("H:/methnet/geostat/validation/validation_data_2013_filtered.csv", "value").selectDimValueEqualTo("nace_r2","I551-I553","indic_to","B006","time",time_+" ")
					.delete("nace_r2").delete("indic_to").delete("time").toMap();
			new NUTSMap(3, 60, "geo", statData, null).make().saveAsImage("H:/methnet/geostat/maps/map_validation_data_nuts3_"+time_+".png", 1000);
		}
		//*/

		//CSV.load("H:/methnet/geostat/validation/validation_result_diff_abs.csv", "value").printInfo();
		statData = CSV.load("H:/methnet/geostat/validation/validation_result_diff_abs.csv", "value").selectDimValueEqualTo("nace_r2","I551-I553","indic_to","B006","time","2010 ")
				.delete("nace_r2").delete("indic_to").delete("time").toMap();
		new NUTSMap(3, 60, "geo", statData, null).make().saveAsImage("H:/methnet/geostat/maps/map_validation_result_diff_abs_"+"2010"+".png", 1000);
		//*/

	}








	private static void filterE4ValidationDataAggregatesNUTS2() {

		//load validation data
		StatsHypercube hcValNuts3 = EurostatTSV.load("H:/methnet/geostat/validation/validation_data_2013.tsv");
		//StatsHypercube hcValNuts3 = CSV.load("H:/methnet/geostat/validation/validation_data_2013_filtered.csv", "value");

		//TODO extract generic nuts aggregate computation
		//compute aggregates
		HashMap<String,Stat> data = new HashMap<String,Stat>();
		for(Stat s : hcValNuts3.stats){
			String geo = s.dims.get("geo").substring(0, 4);
			String time = s.dims.get("time");
			String key = geo+"_"+time;
			Stat s_ = data.get(key);
			if(s_ == null){
				s_ = new Stat(s.value,"geo",geo,"time",time+" ");
				data.put(key, s_);
			} else {
				s_.value += s.value;
			}
		}

		//transform into hc structure
		StatsHypercube hcValNuts2 = new StatsHypercube("geo", "time");
		hcValNuts2.stats.addAll(data.values());
		data = null;
		//hcValNuts2.printInfo();
		//CSV.save(hcNuts2, "value", "H:/methnet/geostat/validation/", "validation_data_nuts2_agg.csv");


		//load eurobase data
		StatsHypercube hcEBNuts2 = EurostatTSV.load("H:/eurobase/tour_occ_nin2.tsv",
				new Selection.And(
						new Selection.DimValueEqualTo("unit","NR"),
						//keep only nuts 2 regions
						new Selection.Criteria() { public boolean keep(Stat stat) { return stat.dims.get("geo").length() == 4; } }
						));
		hcEBNuts2.delete("unit");
		//hcEBNuts2.printInfo();

		//nuts structure
		StatsHypercube dataNuts2 = new StatsHypercube(hcEBNuts2.getDimLabels());

		//show all possibilities
		//for(String nace : hc.getDimValues("nace_r2")){
		//for(String indic : hc.getDimValues("indic_to"))
		{
			String indic = "B006", nace = "I551-I553";
			//System.out.println( nace + "   " + indic );

			StatsHypercube hc_;
			hc_ = hcEBNuts2.selectDimValueEqualTo("nace_r2", nace).selectDimValueEqualTo("indic_to", indic);

			StatsHypercube diff = Validation.computeDifference(hcValNuts2, hc_, true, true);
			//Validation.printBasicStatistics(diff);
			//System.out.println( diff.stats.size() + "   " + diff.selectValueEqualTo(0).stats.size() );
			//CSV.save(diff, "value", "H:/methnet/geostat/validation/", "validation_data_nuts2_agg_diff_"+nace+".csv");
			diff = diff.selectValueEqualTo(0);
			for(Stat s : diff.stats) { s.dims.put("nace_r2", nace); s.dims.put("indic_to", indic); }
			dataNuts2.stats.addAll(diff.stats);
		}{
			String nace = "I551", indic = "B006";
			//System.out.println( nace + "   " + indic );

			StatsHypercube hc_;
			hc_ = hcEBNuts2.selectDimValueEqualTo("nace_r2", nace).selectDimValueEqualTo("indic_to", indic);

			StatsHypercube diff = Validation.computeDifference(hcValNuts2, hc_, true, true);
			//Validation.printBasicStatistics(diff);
			//System.out.println( diff.stats.size() + "   " + diff.selectValueEqualTo(0).stats.size() );
			//CSV.save(diff, "value", "H:/methnet/geostat/validation/", "validation_data_nuts2_agg_diff_"+nace+".csv");
			diff = diff.selectValueEqualTo(0);
			for(Stat s : diff.stats) { s.dims.put("nace_r2", nace); s.dims.put("indic_to", indic); }
			dataNuts2.stats.addAll(diff.stats);
		}
		//}

		//dataNuts2.printInfo();
		//CSV.save(dataNuts2, "value", "H:/methnet/geostat/validation/", "validation_data_nuts2.csv");

		//get nuts3 data
		StatsHypercube hcValNuts3Filtered = new StatsHypercube(hcEBNuts2.getDimLabels());
		StatsIndex dataNuts2Ind = new StatsIndex(dataNuts2, "geo", "time");
		for(Stat s : hcValNuts3.stats){
			String geo = s.dims.get("geo");
			String geo2 = geo.substring(0, 4);
			String time = s.dims.get("time");

			Collection<Stat> col = dataNuts2Ind.getCollection(geo2, time+" ");
			if(col == null) continue;

			for(Stat s2 : col){
				s.dims.put("time", time+" ");
				s.dims.put("nace_r2", s2.dims.get("nace_r2"));
				s.dims.put("indic_to", "B006");
				hcValNuts3Filtered.stats.add(s);
			}
		}

		CSV.save(hcValNuts3Filtered, "value", "H:/methnet/geostat/validation/", "validation_data_2013_filtered.csv");

	}

	private static void finalCheckE4ValidationData() {

		//load validation data
		StatsHypercube hcValNuts3 = CSV.load("H:/methnet/geostat/validation/validation_data_2013_filtered.csv", "value");

		//compute aggregates
		HashMap<String,Stat> data = new HashMap<String,Stat>();
		for(Stat s : hcValNuts3.stats){
			String geo = s.dims.get("geo").substring(0, 4);
			String time = s.dims.get("time");
			String key = geo+"_"+time+"_"+s.dims.get("nace_r2")+"_"+s.dims.get("indic_to");
			Stat s_ = data.get(key);
			if(s_ == null){
				s_ = new Stat(s.value,"geo",geo,"time",time,"nace_r2",s.dims.get("nace_r2"),"indic_to",s.dims.get("indic_to"));
				data.put(key, s_);
			} else {
				s_.value += s.value;
			}
		}

		//transform into hc structure
		StatsHypercube hcValNuts2 = new StatsHypercube(hcValNuts3.getDimLabels());
		hcValNuts2.stats.addAll(data.values());
		data = null;
		//hcValNuts2.printInfo();
		//CSV.save(hcNuts2, "value", "H:/methnet/geostat/validation/", "validation_data_nuts2_agg.csv");


		//load eurobase data
		StatsHypercube hcEBNuts2 = EurostatTSV.load("H:/eurobase/tour_occ_nin2.tsv",
				new Selection.And(
						new Selection.DimValueEqualTo("unit","NR"),
						//keep only nuts 2 regions
						new Selection.Criteria() { public boolean keep(Stat stat) { return stat.dims.get("geo").length() == 4; } }
						));
		hcEBNuts2.delete("unit");

		StatsHypercube diff = Validation.computeDifference(hcValNuts2, hcEBNuts2, true, true);
		Validation.printBasicStatistics(diff);
		System.out.println( diff.stats.size() + "   " + diff.selectValueEqualTo(0).stats.size() );
		CSV.save(diff, "value", "H:/methnet/geostat/validation/", "validation_data_nuts2_agg_diff.csv");
	}

}
