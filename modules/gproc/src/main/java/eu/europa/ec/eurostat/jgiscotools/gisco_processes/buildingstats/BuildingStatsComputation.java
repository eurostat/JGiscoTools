package eu.europa.ec.eurostat.jgiscotools.gisco_processes.buildingstats;

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

		String basePath = "H:/ws/";
		//String basePath = "/home/juju/Bureau/gisco/";

		int xMin_ = 3700000, xMax_ = 4300000;
		int yMin_ = 2700000, yMax_ = 3300000;
		int step = 50000;

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
					Filter fil = CQL.toFilter(bg + "(NUTS2021_0 LIKE '%LU%' OR NUTS2021_1 LIKE '%FRF%' OR NUTS2021_1 LIKE '%BE3%')");
					cells = GeoData.getFeatures(basePath + "geodata/grids/grid_1km_surf.gpkg", null, fil);
				} catch (CQLException e) { e.printStackTrace(); }
				if(cells==null || cells.size() == 0) continue;
				logger.info(cells.size() + " cells");

				logger.info("Load buildings...");
				Collection<Feature> bu = new ArrayList<Feature>();

				logger.info("Load buildings FR...");
				Collection<Feature> buFR = getFeatures(basePath + "geodata/fr/bdtopo/BDTOPO_3-3_TOUSTHEMES_GPKG_LAMB93_R44_2023-03-15/BATIMENT.gpkg", xMin, yMin, xMax, yMax, "ID", "FR");
				//"(ETAT='En service' AND (USAGE1='Résidentiel' OR USAGE2='Résidentiel'))"
				logger.info("   " + buFR.size() + " buildings FR");
				bu.addAll(buFR); buFR.clear();
				//TODO remove duplicates ?
				logger.info("Load buildings BE...");
				for(String ds : new String[] {"PICC_vDIFF_SHAPE_31370_PROV_BRABANT_WALLON", "PICC_vDIFF_SHAPE_31370_PROV_HAINAUT", "PICC_vDIFF_SHAPE_31370_PROV_LIEGE", "PICC_vDIFF_SHAPE_31370_PROV_LUXEMBOURG", "PICC_vDIFF_SHAPE_31370_PROV_NAMUR"}) {
					Collection<Feature> buBE = getFeatures(basePath + "geodata/be/"+ds+"/CONSTR_BATIEMPRISE.gpkg", xMin, yMin, xMax, yMax, "GEOREF_ID", "BE");
					logger.info("   " + buBE.size() + " buildings BE " + ds);
					bu.addAll(buBE); buBE.clear();
				}

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

			if(!"En service".equals(f.getAttribute("etat_de_l_objet"))) return new BuildingStat();

			//nb floors
			Integer nb = (Integer) f.getAttribute("nombre_d_etages");
			if(nb == null) {
				//compute floors nb from height
				Double h = (Double) f.getAttribute("hauteur");
				if(h==null) nb = 1;
				else nb = Math.max( (int)(h/3.5), 1);
			}

			double contrib = nb*area;

			//type contributions
			String u1 = (String) f.getAttribute("usage_1");
			if(u1 == null || "Indifferencié".equals(u1)) {
				Object n = f.getAttribute("nature");
				if("Industriel, agricole ou commercial".equals(n)) return new BuildingStat(0,contrib/3,contrib/3,contrib/3);
				else if("Silo".equals(n)) return new BuildingStat(0,contrib,0,0);
				else return new BuildingStat(contrib,0,0,0);
			} else {
				String u2 = (String) f.getAttribute("usage_2");
				double r0 = getBDTopoTypeRatio("Résidentiel", u1, u2);
				double r1 = getBDTopoTypeRatio("Agricole", u1, u2);
				double r2 = getBDTopoTypeRatio("Industriel", u1, u2);
				double r3 = getBDTopoTypeRatio("Commercial et services", u1, u2);
				return new BuildingStat(
						contrib*r0,
						contrib*r1,
						contrib*r2,
						contrib*r3
						);
			}

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

			double contrib = nb * area;

			BuildingStat bs = new BuildingStat();

			Object n = f.getAttribute("NATURE_DESC");
			if(n==null) {
				bs.res = contrib;
			} else {
				String nS = n.toString();
				if("Habitation".equals(nS)) bs.res = contrib;
				if("Prison".equals(nS)) bs.res = contrib;
				else if("Agricole".equals(nS)) bs.agri = contrib;
				else if("Industriel".equals(nS)) bs.indus = contrib;
				else if("Station d'épuration".equals(nS)) bs.indus = contrib;
				else if("Château".equals(nS)) ;
				else if("Château d'eau".equals(nS)) ;
				else if("Annexe".equals(nS)) ;
				else {
					System.err.println(nS);
					bs.res = contrib;
				}
			}

			return bs;
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

			double contrib = nb * area;

			BuildingStat bs = new BuildingStat();

			Object n = f.getAttribute("NATURE");
			if(n==null) {
				bs.res = contrib;
			} else {
				String nS = f.getAttribute("NATURE").toString();
				if("0".equals(nS)) bs.res = contrib;
				else if(nS.subSequence(0, 1).equals("1")) bs.indus = contrib;
				else if(nS.subSequence(0, 1).equals("2")) bs.agri = contrib;
				else if(nS.subSequence(0, 1).equals("3")) bs.commServ = contrib;
				else if( "41206".equals(nS) || "41207".equals(nS) || "41208".equals(nS) ) bs.res = contrib;
				else if(nS.subSequence(0, 1).equals("4")) bs.commServ = contrib;
				else if(nS.subSequence(0, 1).equals("5")) bs.commServ = contrib;
				else if("60000".equals(nS)) {}
				else if(nS.subSequence(0, 1).equals("7")) bs.commServ = contrib;
				else if("80000".equals(nS)) bs.agri = contrib;
				else if("90000".equals(nS)) {}
				else if("100000".equals(nS)) {}
				else {
					System.err.println(nS);
					bs.res = contrib;
				}
			}

			return bs;
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

}
