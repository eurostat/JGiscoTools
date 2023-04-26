package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridaggregation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

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

public class BuildingStatsComputation {
	private static Logger logger = LogManager.getLogger(BuildingStatsComputation.class.getName());

	//use: -Xms2G -Xmx12G
	/** @param args 
	 * @throws Exception **/
	public static void main(String[] args) {
		logger.info("Start");

		//String basePath = "E:/workspace/building_stats/test/";
		String basePath = "/home/juju/Bureau/gisco/";

		int xMin_ = 3800000, xMax_ = 4300000;
		int yMin_ = 2700000, yMax_ = 3300000;
		int step = 200000;

		//the output statistics
		StatsHypercube shOut = null;

		for(int xMin = xMin_; xMin<xMax_; xMin += step) {
			int xMax = xMin + step;
			for(int yMin = yMin_; yMin<yMax_; yMin += step) {
				int yMax = yMin + step;

				logger.info("Partition " + xMin + " " + yMin);

				logger.info("Load cells...");
				ArrayList<Feature> cells = null;
				try {
					String bg = "BBOX(geometry, "+(xMin+1)+", "+(yMin+1)+", "+(xMax-1)+", "+(yMax-1)+") AND ";
					Filter fil = CQL.toFilter(bg + "(NUTS2021_0 LIKE '%LU%' OR NUTS2021_3 LIKE '%FRF33%' OR NUTS2021_2 LIKE '%BE34%')");
					cells = GeoData.getFeatures(basePath + "grids/grid_1km_surf.gpkg", null, fil);
				} catch (CQLException e) { e.printStackTrace(); }
				if(cells==null || cells.size() == 0) continue;
				logger.info(cells.size() + " cells");

				logger.info("Load buildings...");
				Collection<Feature> bu = new ArrayList<Feature>();

				//logger.info("Load buildings FR...");
				//Collection<Feature> buFR = getFeatures(basePath + "geodata/fr/bdtopo/057/BATIMENT.gpkg", xMin, yMin, xMax, yMax, "ID", "FR");
				//"(ETAT='En service' AND (USAGE1='Résidentiel' OR USAGE2='Résidentiel'))"
				//logger.info("   " + buFR.size() + " buildings FR");
				//bu.addAll(buFR); buFR.clear();

				//logger.info("Load buildings BE...");
				//Collection<Feature> buBE = getFeatures(basePath + "geodata/be/PICC_vDIFF_SHAPE_31370_PROV_LUXEMBOURG/CONSTR_BATIEMPRISE.gpkg", xMin, yMin, xMax, yMax, "GEOREF_ID", "BE");
				//logger.info("   " + buBE.size() + " buildings BE");
				//bu.addAll(buBE); buBE.clear();

				logger.info("Load buildings LU...");
				Collection<Feature> buLU = getFeatures(basePath + "geodata/lu/BD_ACT/BDLTC_SHP/BATIMENT.gpkg", xMin, yMin, xMax, yMax, "ID", "LU");
				logger.info("   " + buLU.size() + " buildings LU");
				bu.addAll(buLU); buLU.clear();

				//TODO filter duplicates among countries

				if(bu.size() == 0) continue;

				//compute aggregation
				GridAggregator<BuildingStat> ga = new GridAggregator<>(cells, "GRD_ID", bu, mapOp, reduceOp);
				ga.compute(true);

				if(ga.getStats().stats.size() == 0) continue;

				if(shOut == null) shOut = ga.getStats();
				else shOut.stats.addAll(ga.getStats().stats);

				//help gc
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





	private static Collection<Feature> getFeatures(String path, int xMin, int yMin, int xMax, int yMax, String idAtt, String ccTag) {
		try {
			ArrayList<Feature> fs = GeoData.getFeatures(
					path,
					idAtt,
					CQL.toFilter("BBOX(geom, "+(xMin+1)+", "+(yMin+1)+", "+(xMax-1)+", "+(yMax-1)+")")
					);
			for(Feature f : fs) f.setAttribute("CC", ccTag);
			return fs;
		} catch (CQLException e) { e.printStackTrace(); }
		return null;

	}




	private static MapOperation<BuildingStat> mapOp = new MapOperation<>() {
		@Override
		public BuildingStat map(Feature f, Geometry inter) {
			String cc = f.getAttribute("CC").toString();
			switch (cc) {
			case "FR": return mapOpFR.map(f, inter);
			case "BE": return mapOpBE.map(f, inter);
			case "LU": return mapOpLU.map(f, inter);
			default: return null;
			}
		}
	};


	private static MapOperation<BuildingStat> mapOpFR = new MapOperation<>() {
		@Override
		public BuildingStat map(Feature f, Geometry inter) {
			if(inter == null || inter.isEmpty()) return new BuildingStat();
			double area = inter.getArea();
			if(area == 0 ) return new BuildingStat();

			if(!f.getAttribute("ETAT").equals("En service")) return new BuildingStat();

			//nb floors
			Integer nb = (Integer) f.getAttribute("NB_ETAGES");
			if(nb == null) {
				//compute floors nb from height
				Double h = (Double) f.getAttribute("HAUTEUR");
				if(h==null) nb = 1;
				else nb = Math.max( (int)(h/3.5), 1);
			}

			//type contributions
			String u1 = (String) f.getAttribute("USAGE1");
			String u2 = (String) f.getAttribute("USAGE2");
			double r0 = getBDTopoTypeRatio("Résidentiel", u1, u2);
			double r1 = getBDTopoTypeRatio("Agricole", u1, u2);
			double r2 = getBDTopoTypeRatio("Industriel", u1, u2);
			double r3 = getBDTopoTypeRatio("Commercial et services", u1, u2);

			//
			return new BuildingStat(
					nb*area*r0,
					nb*area*r1,
					nb*area*r2,
					nb*area*r3
					);
		}

		private double getBDTopoTypeRatio(String type, String u1, String u2) {
			if(type.equals(u1) && u2==null) return 1;
			if(type.equals(u1) && u2!=null) return 0.7;
			if(type.equals(u2)) return 0.3;
			return 0;
		}	

	};



	private static MapOperation<BuildingStat> mapOpBE = new MapOperation<>() {
		@Override
		public BuildingStat map(Feature f, Geometry inter) {
			if(inter == null || inter.isEmpty()) return new BuildingStat();
			double area = inter.getArea();
			if(area == 0 ) return new BuildingStat();

			//nb floors
			Integer nb = 1;
			//TODO: this is elevation of the roof top. Need for elevation of the bottom...
			//double elevTop = f.getGeometry().getCoordinate().z;
			//System.out.println(h);
			//if(h==null) nb = 1;
			//else nb = Math.max( (int)(h/3.5), 1);

			return new BuildingStat(nb*area, 0, 0, 0);
		}
	};


	private static MapOperation<BuildingStat> mapOpLU = new MapOperation<>() {
		@Override
		public BuildingStat map(Feature f, Geometry inter) {
			if(inter == null || inter.isEmpty()) return new BuildingStat();
			double area = inter.getArea();
			if(area == 0 ) return new BuildingStat();

			//nb floors
			Integer nb = 1;
			//double elevTop = f.getGeometry().getCoordinate().z;
			//System.out.println(elevTop);

			//type
			String n = f.getAttribute("NATURE").toString();

			return new BuildingStat(nb*area, 0, 0, 0);
		}
	};







	private static ReduceOperation<BuildingStat> reduceOp = new ReduceOperation<>() {
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
			out.add( new Stat(v[0], cellIdAtt, cellId, "bu_stat", "res") );
			out.add( new Stat(v[1], cellIdAtt, cellId, "bu_stat", "agri") );
			out.add( new Stat(v[2], cellIdAtt, cellId, "bu_stat", "indus") );
			out.add( new Stat(v[3], cellIdAtt, cellId, "bu_stat", "comm_serv") );

			//add total
			double total = v[0]+v[1]+v[2]+v[3];
			out.add( new Stat(total, cellIdAtt, cellId, "bu_stat", "total") );
			double totalActivity = v[1]+v[2]+v[3];
			out.add( new Stat(totalActivity, cellIdAtt, cellId, "bu_stat", "total_activity") );

			//add percentages
			out.add( new Stat(total==0? 0 : 100*v[0]/total, cellIdAtt, cellId, "bu_stat", "p_res") );
			out.add( new Stat(total==0? 0 : 100*v[1]/total, cellIdAtt, cellId, "bu_stat", "p_agri") );
			out.add( new Stat(total==0? 0 : 100*v[2]/total, cellIdAtt, cellId, "bu_stat", "p_indus") );
			out.add( new Stat(total==0? 0 : 100*v[3]/total, cellIdAtt, cellId, "bu_stat", "p_comm_serv") );
			out.add( new Stat(total==0? 0 : 100*totalActivity/total, cellIdAtt, cellId, "bu_stat", "p_act") );

			//typologies
			int typResAct = total==0.0? 0 : getBuildingTypologyResAct(v[0]/total, totalActivity/total);
			out.add( new Stat(typResAct, cellIdAtt, cellId, "bu_stat", "typology_res_act") );
			int typAct = totalActivity==0.0? 0 : getBuildingTypologyAct(v[1]/totalActivity, v[2]/totalActivity, v[3]/totalActivity );
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
	};





	/**
	 * Remove the duplicates, that is the features that have same attributes.
	 * TODO use the one from featureutil instead
	 * 
	 * @param fs
	 * @param idAtt
	 * @return 
	 */
	public static ArrayList<Feature> removeDuplicates(Collection<Feature> fs, String idAtt) {

		ArrayList<Feature> out = new ArrayList<Feature>();
		HashSet<String> ids = new HashSet<String>();

		for(Feature f : fs) {
			String id = idAtt==null? f.getID() : f.getAttribute(idAtt).toString();
			if(ids.contains(id)) continue;
			ids.add(id);
			out.add(f);
		}

		return out;
	}



	public static class BuildingStat {
		double res = 0;
		double agri = 0;
		double indus = 0;
		double commServ = 0;

		public BuildingStat() {
			this(0,0,0,0);
		}

		public BuildingStat(double res, double agri, double indus, double commServ) {
			this.res = res;
			this.agri = agri;
			this.indus = indus;
			this.commServ = commServ;
		}
	}

}
