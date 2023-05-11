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
import eu.europa.ec.eurostat.java4eurostat.io.CSV;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.geostat.GridAggregator;
import eu.europa.ec.eurostat.jgiscotools.geostat.GridAggregator.MapOperation;
import eu.europa.ec.eurostat.jgiscotools.geostat.GridAggregator.ReduceOperation;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

public class ZZZBuildingStatsComputationOld {
	private static Logger logger = LogManager.getLogger(ZZZBuildingStatsComputationOld.class.getName());

	//use: -Xms2G -Xmx12G
	/** @param args 
	 * @throws Exception **/
	public static void main(String[] args) {
		logger.info("Start");

		//String basePath = "E:/workspace/building_stats/test/";
		String basePath = "/home/juju/Bureau/gisco/";

		logger.info("Load cells...");
		Filter fil = null;
		try {
			fil = CQL.toFilter("(NUTS2021_1='FRF')");
		} catch (CQLException e) { e.printStackTrace(); }
		ArrayList<Feature> cells = GeoData.getFeatures(basePath + "grids/grid_1km_surf.gpkg", null, fil);
		logger.info(cells.size() + " cells");

		logger.info("Load buildings...");
		fil = null;
		/*try {
			fil = CQL.toFilter("(ETAT='En service' AND (USAGE1='Résidentiel' OR USAGE2='Résidentiel'))");
		} catch (CQLException e) { e.printStackTrace(); }*/
		Collection<Feature> fs = null;
		for(String dep : new String[] { "008" , "010", "051", "052", "054", "055", "057", "088", "067", "068" }) {
			logger.info("   "+dep);
			if(fs == null) fs = GeoData.getFeatures(basePath + "cnt/fr/bdtopo/" + dep + "/BATIMENT.gpkg", null, fil);
			else fs.addAll( GeoData.getFeatures(basePath + "cnt/fr/bdtopo/" + dep + "/BATIMENT.gpkg", null, fil) );
			logger.info(fs.size() + " buildings");
		}

		logger.info("Remove duplicates");
		fs = removeDuplicates(fs, "ID");
		logger.info(fs.size() + " buildings");

		logger.info("Define map operation");
		MapOperation<double[]> mapOp = new MapOperation<>() {
			@Override
			public double[] map(Feature f, Geometry inter) {

				double[] out = new double[4]; //TODO
				for(int i=0; i<4; i++) out[i]=0;

				if(inter == null || inter.isEmpty()) return out;

				//area
				double area = inter.getArea();
				if(area == 0 ) return out;

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
				return new double[] {
						nb*area*r0,
						nb*area*r1,
						nb*area*r2,
						nb*area*r3
				};
			}
		};

		logger.info("Define reduce operation");
		ReduceOperation<double[]> reduceOp = new ReduceOperation<>() {
			@Override
			public Collection<Stat> reduce(String cellIdAtt, String cellId, Collection<double[]> data) {
				Collection<Stat> out = new ArrayList<>();

				//compute sums, for each building type
				double[] v = new double[4];
				for(int i=0; i<4; i++) {
					v[i] = 0;
					for(double[] map : data) v[i] += map[i];
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


		//compute aggregation
		GridAggregator<double[]> ga = new GridAggregator<>(cells, "GRD_ID", fs, mapOp, reduceOp);
		ga.compute(true);

		logger.info("Round values...");
		for(Stat s : ga.getStats().stats)
			s.value = (int) Math.round(s.value);

		logger.info("Save...");
		//TODO order columns
		CSV.saveMultiValues(ga.getStats(), basePath + "building_stats/building_area.csv", "bu_stat");

		logger.info("End");
	}

	private static double getBDTopoTypeRatio(String type, String u1, String u2) {
		if(type.equals(u1) && u2==null) return 1;
		if(type.equals(u1) && u2!=null) return 0.7;
		if(type.equals(u2)) return 0.3;
		return 0;
	}	





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
