package eu.europa.ec.eurostat.jgiscotools.gisco_processes.buildingstats;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.locationtech.jts.geom.Geometry;
import org.opengis.filter.Filter;

import eu.europa.ec.eurostat.java4eurostat.base.Stat;
import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.io.CSV;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.geostat.GridAggregator;
import eu.europa.ec.eurostat.jgiscotools.geostat.GridAggregator.MapOperation;
import eu.europa.ec.eurostat.jgiscotools.geostat.GridAggregator.ReduceOperation;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

/**
 * @author gaffuju
 *
 */
public class BuildingStatsComputation implements ReduceOperation<BuildingStat>, MapOperation<BuildingStat> {
	private static Logger logger = LogManager.getLogger(BuildingStatsComputation.class.getName());

	//use: -Xms2G -Xmx12G
	/** @param args 
	 * @throws Exception **/
	public static void main(String[] args) {
		logger.info("Start");

		BuildingStatsComputation bsc = new BuildingStatsComputation();

		String basePath = "H:/ws/";
		//String basePath = "/home/juju/Bureau/gisco/";

		int xMin_ = 3700000, xMax_ = 4200000;
		int yMin_ = 2500000, yMax_ = 3200000;
		int step = 100000;

		//the output statistics
		StatsHypercube shOut = null;

		for(int xMin = xMin_; xMin<xMax_; xMin += step) {
			int xMax = xMin + step;
			for(int yMin = yMin_; yMin<yMax_; yMin += step) {
				int yMax = yMin + step;

				logger.info("Partition " + xMin + " " + yMin);

				logger.info("Load buildings...");
				Collection<Feature> bu = new ArrayList<Feature>();

				logger.info("Load buildings FR...");
				bsc.fr.loadBuildings(bu, basePath, xMin, yMin, xMax, yMax);
				logger.info("Load buildings LU...");
				bsc.lu.loadBuildings(bu, basePath, xMin, yMin, xMax, yMax);
				logger.info("Load buildings BE...");
				bsc.be.loadBuildings(bu, basePath, xMin, yMin, xMax, yMax);


				logger.info("Load cells...");
				ArrayList<Feature> cells = null;
				try {
					String bg = "BBOX(geometry, "+(xMin+1)+", "+(yMin+1)+", "+(xMax-1)+", "+(yMax-1)+") AND ";
					Filter fil = CQL.toFilter(bg + "(NUTS2021_0 LIKE '%LU%' OR NUTS2021_1 LIKE '%FR%' OR NUTS2021_1 LIKE '%BE3%')");
					cells = GeoData.getFeatures(basePath + "geodata/grids/grid_1km_surf.gpkg", null, fil);
				} catch (CQLException e) { e.printStackTrace(); }
				if(cells==null || cells.size() == 0) continue;
				logger.info(cells.size() + " cells");

				//TODO filter duplicates - overlapping buildings

				if(bu.size() == 0) continue;

				//compute aggregation
				GridAggregator<BuildingStat> ga = new GridAggregator<>(cells, "GRD_ID", bu, bsc, bsc);
				ga.compute(true);

				if(ga.getStats().stats.size() == 0) continue;

				if(shOut == null) shOut = ga.getStats();
				else shOut.stats.addAll(ga.getStats().stats);

				//help gc
				for(Feature f : bu) f.destroy();
				bu.clear();
				cells.clear();
			}
		}

		logger.info("Round values...");
		for(Stat s : shOut.stats)
			s.value = (int) Math.round(s.value);

		logger.info("Save...");
		//TODO order columns
		CSV.saveMultiValues(shOut, basePath + "building_stats/building_area.csv", "bu_stat");

		logger.info("End");
	}


	private FR fr = new FR();
	private BE be = new BE();
	private LU lu = new LU();


	@Override
	public BuildingStat map(Feature f, Geometry inter) {
		String cc = f.getAttribute("CC").toString();
		switch (cc) {
		case "FR": return fr.map(f, inter);
		case "BE": return be.map(f, inter);
		case "LU": return lu.map(f, inter);
		default: return null;
		}
	}

	@Override
	public Collection<Stat> reduce(String cellIdAtt, String cellId, Collection<BuildingStat> data) {
		Collection<Stat> out = new ArrayList<>();

		//compute sums, for each building type
		BuildingStat v = new BuildingStat();
		for(BuildingStat map : data) {
			v.res += map.res;
			v.agri += map.agri;
			v.indus += map.indus;
			v.commServ += map.commServ;
		}

		//add stats
		out.add( new Stat(v.res, cellIdAtt, cellId, "bu_stat", "res") );
		out.add( new Stat(v.agri, cellIdAtt, cellId, "bu_stat", "agri") );
		out.add( new Stat(v.indus, cellIdAtt, cellId, "bu_stat", "indus") );
		out.add( new Stat(v.commServ, cellIdAtt, cellId, "bu_stat", "comm_serv") );

		//add total
		double total = v.res+v.agri+v.indus+v.commServ;
		out.add( new Stat(total, cellIdAtt, cellId, "bu_stat", "total") );
		double totalActivity = v.agri+v.indus+v.commServ;
		out.add( new Stat(totalActivity, cellIdAtt, cellId, "bu_stat", "total_activity") );

		//add percentages
		out.add( new Stat(total==0? 0 : 100*v.res/total, cellIdAtt, cellId, "bu_stat", "p_res") );
		out.add( new Stat(total==0? 0 : 100*v.agri/total, cellIdAtt, cellId, "bu_stat", "p_agri") );
		out.add( new Stat(total==0? 0 : 100*v.indus/total, cellIdAtt, cellId, "bu_stat", "p_indus") );
		out.add( new Stat(total==0? 0 : 100*v.commServ/total, cellIdAtt, cellId, "bu_stat", "p_comm_serv") );
		out.add( new Stat(total==0? 0 : 100*totalActivity/total, cellIdAtt, cellId, "bu_stat", "p_act") );

		//typologies
		int typResAct = total==0.0? 0 : getBuildingTypologyResAct(v.res/total, totalActivity/total);
		out.add( new Stat(typResAct, cellIdAtt, cellId, "bu_stat", "typology_res_act") );
		int typAct = totalActivity==0.0? 0 : getBuildingTypologyAct(v.agri/totalActivity, v.indus/totalActivity, v.commServ/totalActivity );
		out.add( new Stat(typAct, cellIdAtt, cellId, "bu_stat", "typology_act") );

		return out;
	}

	//typology res/activity
	private int getBuildingTypologyResAct(double pRes, double pAct) {
		if(pRes==0.0 && pAct==0.0) return 0;
		if(pRes>0.7) return 9;
		if(pAct>0.7) return 8;
		return 98;
	}

	//typology res/activity
	private int getBuildingTypologyAct(double pAgri, double pIndus, double pComServ) {
		if(pAgri==0.0 && pIndus==0.0 && pComServ==0.0) return 0;

		if(pAgri>0.7) return 1;
		if(pIndus>0.7) return 2;
		if(pComServ>0.7) return 3;

		if(pAgri>0.25 && pIndus>0.25 && pComServ>0.25) return 123;

		double min = Math.min(pAgri, Math.min(pIndus, pComServ));
		if(min == pAgri) return 23;
		if(min == pIndus) return 13;
		if(min == pComServ) return 12;

		return -999;
	}







	//TODO move to geodata ?
	//TODO add possibility for other filter ?
	static Collection<Feature> getFeatures(String path, String idAtt, String geomAtt, int xMin, int yMin, int xMax, int yMax, double d) {
		//System.out.println(GeoData.getSchema(path));
		try {
			ArrayList<Feature> fs = GeoData.getFeatures(
					path,
					idAtt,
					//CQL.toFilter("geom.maxx >= "+xMin+" AND geom.minx <= "+xMax+" AND geom.maxy >= "+yMin+" AND geom.miny <= "+yMax)
					CQL.toFilter("BBOX("+geomAtt+", "+(xMin+d)+", "+(yMin+d)+", "+(xMax-d)+", "+(yMax-d)+")")
					);
			return fs;
		} catch (CQLException e) { e.printStackTrace(); }
		return null;
	}

}
