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

		String basePath = "E:/workspace/building_stats/test/";

		logger.info("Load cells...");
		Filter fil = null;
		try {
			fil = CQL.toFilter("(NUTS_1_ID='FRL')");
		} catch (CQLException e) { e.printStackTrace(); }
		ArrayList<Feature> cells = GeoData.getFeatures("E:\\dissemination\\shared-data\\grid\\grid_1km_surf.gpkg", null, fil);
		logger.info(cells.size() + " cells");

		logger.info("Load buildings...");
		fil = null;
		try {
			fil = CQL.toFilter("(ETAT='En service' AND (USAGE1='RÃ©sidentiel' OR USAGE2='RÃ©sidentiel'))");
		} catch (CQLException e) { e.printStackTrace(); }
		Collection<Feature> fs = null;
		for(String dep : new String[] { "04", "05", "06", "84", "83", "13" }) {
			logger.info("   "+dep);
			if(fs == null) fs = GeoData.getFeatures(basePath + dep + "/buildings.gpkg", null, fil);
			else fs.addAll( GeoData.getFeatures(basePath + dep + "/buildings.gpkg", null, fil) );
			logger.info(fs.size() + " buildings");
		}

		logger.info("Remove duplicates");
		fs = removeDuplicates(fs, "ID");
		logger.info(fs.size() + " buildings");

		logger.info("Define map operation");
		MapOperation<double[]> mapOp = new MapOperation<>() {
			@Override
			public double[] map(Feature f, Geometry inter) {

				double[] out = new double[4];
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
				double r0 = getBDTopoTypeRatio("RÃ©sidentiel", u1, u2);
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
				for(int i=0; i<4; i++) {
					String type = i==0? "res" : i==1? "agri" : i==2? "indus" : "comm_serv";
					Stat s = new Stat(0, cellIdAtt, cellId, "building_type", type);
					for(double[] map : data)
						s.value += map[i];
					out.add(s);
				}
				return out;
			}
		};


		//compute aggregation
		GridAggregator<double[]> ga = new GridAggregator<>(cells, "GRD_ID", fs, mapOp, reduceOp);
		ga.compute(true);

		logger.info("Round values...");
		for(Stat s : ga.getStats().stats)
			s.value = (int) Math.round(s.value);

		logger.info("Save...");
		CSV.saveMultiValues(ga.getStats(), basePath + "/building_area.csv", "building_type");

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
