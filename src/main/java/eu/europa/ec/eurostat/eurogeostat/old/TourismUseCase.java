/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.old;

import java.awt.Color;
import java.util.Collection;
import java.util.HashMap;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.filter.function.RangedClassifier;
import org.opencarto.io.ShapeFile;

import eu.europa.ec.eurostat.eurogeostat.core.MappingUtils;
import eu.europa.ec.eurostat.eurogeostat.core.StatisticalMap;
import eu.europa.ec.eurostat.eurogeostat.dasymetric.DasymetricMapping;
import eu.europa.ec.eurostat.eurogeostat.nuts.NUTSMap;
import eu.europa.ec.eurostat.eurogeostat.nuts.NUTSShapeFile;
import eu.europa.ec.eurostat.eurogeostat.nuts.NUTSUtils;
import eu.europa.ec.eurostat.java4eurostat.analysis.Validation;
import eu.europa.ec.eurostat.java4eurostat.base.Selection;
import eu.europa.ec.eurostat.java4eurostat.base.Stat;
import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.base.StatsIndex;
import eu.europa.ec.eurostat.java4eurostat.io.CSV;
import eu.europa.ec.eurostat.java4eurostat.io.EurostatTSV;

/**
 * @author julien Gaffuri
 *
 */
public class TourismUseCase {
	public static String BASE_PATH = "H:/geodata/";
	public static String POI_TOURISEM_SHP_BASE = BASE_PATH + "eur2016_12/mnpoi_";

	//TODO test memory mapping

	//TODO contact tomtom guys. ask for data
	//TODO validation with it data http://dati.istat.it/?lang=en
	//TODO validation of accomodation data
	//TODO focus on FR.

	//TODO test with postgis as datasource
	//http://docs.geotools.org/stable/userguide/library/jdbc/postgis.html
	/*
	  Map<String,Object> params = new HashMap<>();
    params.put( "dbtype", "postgis");
    params.put( "host", "localhost");
    params.put( "port", 5432);
    params.put( "schema", "public");
    params.put( "database", "database");
    params.put( "user", "postgres");
    params.put( "passwd", "postgres");

    DataStore dataStore = DataStoreFinder.getDataStore(params);
	 */

	public static void main(String[] args) throws Exception {
		System.out.println("Start.");

		//download/update data for tourism
		//EurobaseIO.update("H:/eurobase/", "tour_occ_nim", "tour_occ_nin2", "tour_occ_nin2d", "tour_occ_nin2c", "urb_ctour");

		//runDasymetric(0); //NUTS3
		//computeDensityPopRatio();
		runDasymetric(1); //COMM

		//makeMaps();

		//computeValidation();
		//makeValidationMaps();

		//E4 data validation
		//filterE4ValidationDataAggregatesNUTS2();
		//finalCheckE4ValidationData();

		//runDasymetricGrid();
		//makeGridMaps();


		//runDasymetricFrance();
		//makeFranceMaps();



		System.out.println("End.");
	}

	/*public static void runDasymetricFrance(){
		//load tourism data to disaggregate
		StatsHypercube hc = EurostatTSV.load("H:/eurobase/tour_occ_nin2.tsv",
				new Selection.And(
						new Selection.DimValueEqualTo("unit","NR"), //Number
						//new Selection.DimValueEqualTo("nace_r2","I551-I553"), //Hotels; holiday and other short-stay accommodation; camping grounds, recreational vehicle parks and trailer parks
						new Selection.DimValueEqualTo("indic_to","B006"), //Nights spent, total
						//keep only nuts 2 regions
						new Selection.Criteria() { public boolean keep(Stat stat) { return stat.dims.get("geo").length() == 4; } },
						//keep only years after 2010
						new Selection.Criteria() { public boolean keep(Stat stat) { return Integer.parseInt(stat.dims.get("time").replace("", "")) >= 2010; } }
						));
		hc.delete("unit"); hc.delete("indic_to");
		StatsIndex hcI = new StatsIndex(hc, "nace_r2", "time", "geo");
		//hc.printInfo();
		//hcI.print();
		hc = null;

		//TODO
		//look at the BPE data
		//G102 hotel, G103 camping
		//extract points by type
		//check unlocated points in BPE ?
		//run dasymetry with BPE
		//* try to match tomtom with BPE
		//* try to map geo with data from FR_registre_hebergements_classes

	}*/

	public static void runDasymetricGrid(){
		//load tourism data to disaggregate
		StatsHypercube hc = EurostatTSV.load("H:/eurobase/tour_occ_nin2.tsv",
				new Selection.And(
						new Selection.DimValueEqualTo("unit","NR"), //Number
						//new Selection.DimValueEqualTo("nace_r2","I551-I553"), //Hotels; holiday and other short-stay accommodation; camping grounds, recreational vehicle parks and trailer parks
						new Selection.DimValueEqualTo("indic_to","B006"), //Nights spent, total
						//keep only nuts 2 regions
						new Selection.Criteria() { public boolean keep(Stat stat) { return stat.dims.get("geo").length() == 4; } },
						//keep only years after 2010
						new Selection.Criteria() { public boolean keep(Stat stat) { return Integer.parseInt(stat.dims.get("time").replace("", "")) >= 2010; } }
						));
		hc.delete("unit"); hc.delete("indic_to");
		StatsIndex hcI = new StatsIndex(hc, "nace_r2", "time", "geo");
		//hc.printInfo();
		//hcI.print();
		hc = null;

		//go through nace codes
		for(String nace : new String[]{"I551-I553","I551","I552","I553"}){
			//if(!"I551-I553".equals(nace)) continue;
			if(!"I551".equals(nace)) continue;

			//compute values for all years
			for(String time : hcI.getKeys(nace)){
				//if(!"2015 ".equals(time)) continue;

				//create dasymetric analysis object
				DasymetricMapping dm = new DasymetricMapping(
						1,
						null,
						new ShapeFile("resources/NUTS/2013/1M/LAEA/lvl2/RG.shp").getFeatureSource(),
						//NUTSShapeFile.getRG(2).getFeatureStore(),
						"NUTS_ID",
						new ShapeFile(POI_TOURISEM_SHP_BASE+nace+".shp").getFeatureSource(),
						"ID2",
						new ShapeFile(BASE_PATH+"grid/10km/grid10km.shp").getFeatureSource(),
						"ID_"
						);

				//get input stat values to disaggregate
				dm.statValuesInitial = hcI.getSubIndex(nace, time);
				if(dm.statValuesInitial == null) System.out.println("No values !");;

				//run dasymetric mapping
				System.out.println("Step 2: allocate statistics at geo features level");
				dm.allocateStatGeo(true); CSV.save(dm.statsGeoAllocationHC, "value", "H:/methnet/geostat/out/", "NUTS_2_to_POI_"+nace+"_"+time+".csv");
				//dm.statsGeoAllocationHC = CSV.load("H:/methnet/geostat/out/NUTS_2_to_POI_"+nace+"_"+time+".csv", "value");

				System.out.println("Step 3: aggregate statistics at target stat unit level");
				dm.aggregateGeoStat();

				CSV.save(dm.finalStatsHC, "value", "H:/methnet/geostat/out/", "grid10km_"+nace+"_"+time+".csv");
			} //time
		} //nace
	}


	private static void makeGridMaps() {
		String outPath = "H:/methnet/geostat/maps/";
		//String outPath = "H:/desktop/";

		RangedClassifier classifier = MappingUtils.getClassifier(4500,7000,10000,15000,20000,30000,50000,100000);

		for(int year=2010; year<=2015; year++){
			//if(year != 2015) continue;

			HashMap<String, Double> stats = CSV.load("H:/methnet/geostat/out/grid10km_I551_"+year+" .csv", "value").toMap();
			SimpleFeatureCollection grid = new ShapeFile("H:/geodata/grid/10km/grid10km.shp").getFeatureCollection();

			new StatisticalMap(grid, "ID_", stats, null, classifier)
			.setBounds(2580000.0, 7350000.0, 1340000.0, 5450000.0)
			//.setBounds(5580000.0, 6350000.0, 2340000.0, 3450000.0)
			.setTitle(""+year)
			.setNoDataColor(Color.WHITE)
			.setGraticule()
			.make()
			.saveAsImage(outPath+"map_grid10km_I551_"+year+".png")
			.saveLegendAsImage(outPath+"legend_grid10km_I551.png", 0, 200, 20, 5)
			.dispose()
			;
		}

	}



	//n=0 ->NUTS3
	//n=1 ->COMMUNES
	public static void runDasymetric(int n){

		//load tourism data to disaggregate
		StatsHypercube hc = EurostatTSV.load("H:/eurobase/tour_occ_nin2.tsv",
				new Selection.And(
						new Selection.DimValueEqualTo("unit","NR"), //Number
						//new Selection.DimValueEqualTo("nace_r2","I551-I553"), //Hotels; holiday and other short-stay accommodation; camping grounds, recreational vehicle parks and trailer parks
						new Selection.DimValueEqualTo("indic_to","B006"), //Nights spent, total
						//keep only nuts 2 regions
						new Selection.Criteria() { public boolean keep(Stat stat) { return stat.dims.get("geo").length() == 4; } },
						//keep only years after 2010
						new Selection.Criteria() { public boolean keep(Stat stat) { return Integer.parseInt(stat.dims.get("time").replace("", "")) >= 2010; } }
						));
		hc.delete("unit"); hc.delete("indic_to");
		StatsIndex hcI = new StatsIndex(hc, "nace_r2", "time", "geo");
		//hc.printInfo();
		//hcI.print();
		hc = null;

		//save as csv
		//String time = "2015 ", outFile = "H:/methnet/geostat/out/stats_lvl2_"+time+".csv";
		//statIndexToCSV(hcI.getSubIndex(time), "NUTS_ID", outFile);

		//build output structure
		StatsHypercube out = new StatsHypercube("geo", "time", "unit", "nace_r2", "indic_to");

		//go through nace codes
		//for(String nace : new String[]{"I551-I553","I551","I552","I553"}){
		for(String nace : new String[]{"I551-I553","I551"}){

			//create dasymetric analysis object
			DasymetricMapping dm = new DasymetricMapping(
					1,
					null,
					new ShapeFile("resources/NUTS/2013/1M/LAEA/lvl2/RG.shp").getFeatureSource(),
					//NUTSShapeFile.getRG(2).getFeatureStore(),
					"NUTS_ID",
					new ShapeFile(POI_TOURISEM_SHP_BASE+nace+".shp").getFeatureSource(),
					"ID",
					n==0? NUTSShapeFile.getRG(3).getFeatureSource() : new ShapeFile("H:/geodata/gisco_stat_units/COMM_01M_2013_SH/COMM_RG_01M_2013_LAEA.shp").getFeatureSource(),
							n==0? "NUTS_ID" : "COMM_ID"
					);


			//dm.computeGeoStatInitial();   CSV.save(dm.geoStatsInitialHC, "value", "H:/methnet/geostat/out/", "POI_to_NUTS_2___"+nace+".csv");
			dm.geoStatsInitialHC = CSV.load("H:/methnet/geostat/out/POI_to_NUTS_2___"+nace+".csv", "value");

			//dm.computeGeoStatFinal();   CSV.save(dm.geoStatsFinalHC, "value", "H:/methnet/geostat/out/", "POI_to_"+(n==0?"NUTS_3":"COMM")+"___"+nace+".csv");
			dm.geoStatsFinalHC = CSV.load("H:/methnet/geostat/out/POI_to_"+(n==0?"NUTS_3":"COMM")+"___"+nace+".csv", "value");



			//compute values for all years
			for(String time : hcI.getKeys(nace)){
				//get stat values
				dm.statValuesInitial = hcI.getSubIndex(nace, time);
				if(dm.statValuesInitial == null) continue;

				//compute values
				dm.computeDisaggregatedStatsSimplified();

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
		CSV.save(out, "value", "H:/methnet/geostat/out/", "tour_occ_nin2_"+(n==0?"nuts3":"comm")+".csv");

	}

	public static void computeDensityPopRatio(){
		//load computed data
		StatsHypercube sh = CSV.load("H:/methnet/geostat/out/tour_occ_nin2_nuts3.csv", "value");

		//compute P_THAB	Per thousand inhabitants
		StatsHypercube  shPop = NUTSUtils.computePopRatioFigures(sh);
		for(Stat s : shPop.stats) s.dims.put("unit", "P_THAB");

		//compute P_KM2	Per km2
		StatsHypercube  shDens = NUTSUtils.computeDensityFigures(sh);
		for(Stat s : shDens.stats) s.dims.put("unit", "P_KM2");

		//merge and save
		sh.stats.addAll(shPop.stats);
		sh.stats.addAll(shDens.stats);
		CSV.save(sh, "value", "H:/methnet/geostat/out/", "tour_occ_nin2_nuts3_popratio_dens.csv");
	}


	private static void computeValidation() {

		//load data to validate
		StatsHypercube hc = CSV.load("H:/methnet/geostat/out/tour_occ_nin2_nuts3_popratio_dens.csv", "value").selectDimValueEqualTo("unit","P_THAB");
		hc.delete("unit");

		//load validation data
		StatsHypercube hcVal = CSV.load("H:/methnet/geostat/validation/validation_data_2013_filtered.csv", "value");
		//compute ratio, by population
		hcVal = NUTSUtils.computePopRatioFigures(hcVal);

		StatsHypercube diff;

		diff = Validation.computeDifference(hcVal, hc, false, false);
		Validation.printBasicStatistics(diff); System.out.println("");
		CSV.save(diff, "value", "H:/methnet/geostat/validation/", "validation_result_diff.csv");

		diff = Validation.computeDifference(hcVal, hc, true, false);
		Validation.printBasicStatistics(diff); System.out.println("");
		CSV.save(diff, "value", "H:/methnet/geostat/validation/", "validation_result_diff_abs.csv");

		diff = Validation.computeDifference(hcVal, hc, false, true);
		Validation.printBasicStatistics(diff); System.out.println("");
		CSV.save(diff, "value", "H:/methnet/geostat/validation/", "validation_result_diff_ratio.csv");

		diff = Validation.computeDifference(hcVal, hc, true, true);
		Validation.printBasicStatistics(diff); System.out.println("");
		CSV.save(diff, "value", "H:/methnet/geostat/validation/", "validation_result_diff_abs_ratio.csv");
	}


	static RangedClassifier classifier = MappingUtils.getClassifier(750,1500,2000,2500,3000,4000,6000,12000);

	public static void makeMaps(){
		HashMap<String, Double> statData;
		String outPath = "H:/methnet/geostat/maps/";
		int time = 2015;

		//computed data: nuts 3 level map
		statData = CSV.load("H:/methnet/geostat/out/tour_occ_nin2_nuts3_popratio_dens.csv", "value").selectDimValueEqualTo("unit","P_THAB","nace_r2","I551-I553","indic_to","B006","time",time+"").shrinkDims().toMap();
		new NUTSMap(3, 60, statData, classifier).setTitle("NUTS 3 - "+time).make()
		.saveLegendAsImage(outPath+"legend.png", 0, 150, 20, 5)
		.saveAsImage(outPath+"map_result_nuts3_"+time+".png").dispose();
		//*/

		//nuts 2 level map
		statData = EurostatTSV.load("H:/eurobase/tour_occ_nin2.tsv").selectDimValueEqualTo("unit","P_THAB","nace_r2","I551-I553","indic_to","B006","time",time+"").shrinkDims().toMap();
		new NUTSMap(2, 60, statData, classifier).setTitle("NUTS 2 - "+time).make()
		.saveAsImage(outPath+"map_nuts2_"+time+".png").dispose();
		//*/

		//validation data
		StatsHypercube hc = CSV.load("H:/methnet/geostat/validation/validation_data_2013_filtered.csv", "value").selectDimValueEqualTo("nace_r2","I551-I553","indic_to","B006").shrinkDims();
		hc = NUTSUtils.computePopRatioFigures(hc);
		for(int time_ = 2005; time_<= 2013; time_++){
			statData = hc.selectDimValueEqualTo("time",time_+"").shrinkDims().toMap();
			new NUTSMap(3, 60, statData, classifier).setTitle("NUTS 3 validation - "+time_).make()
			.saveAsImage(outPath+"map_validation_data_nuts3_"+time_+".png", 1000, true, false).dispose();
		}
		//*/

	}

	public static void makeValidationMaps(){
		String outPath = "H:/methnet/geostat/maps/";

		StatsHypercube hc = CSV.load("H:/methnet/geostat/validation/validation_result_diff_abs.csv", "value").selectDimValueEqualTo("nace_r2","I551-I553","indic_to","B006").shrinkDims();
		for(int time_ = 2010; time_<= 2013; time_++){
			HashMap<String, Double> statData = hc.selectDimValueEqualTo("time",time_+"").shrinkDims().toMap();
			new NUTSMap(3, 60, statData, classifier).setTitle(time_+" - error").make().saveAsImage(outPath+"map_validation_result_diff_abs_"+time_+".png", 1000, true, false).dispose();
		}
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
				s_ = new Stat(s.value,"geo",geo,"time",time+"");
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

			Collection<Stat> col = dataNuts2Ind.getCollection(geo2, time+"");
			if(col == null) continue;

			for(Stat s2 : col){
				s.dims.put("time", time+"");
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
