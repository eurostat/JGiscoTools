package eu.europa.ec.eurostat.jgiscotools.gisco_processes.buildingstats.cnt;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.geostat.GridAggregator.MapOperation;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.buildingstats.BuildingDataLoader;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.buildingstats.BuildingStat;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.buildingstats.BuildingStatsComputation;

public class FR implements BuildingDataLoader, MapOperation<BuildingStat> {
	private static Logger logger = LogManager.getLogger(FR.class.getName());



	public void loadBuildings(Collection<Feature> bu, String basePath, int xMin, int yMin, int xMax, int yMax) {
		Collection<Feature> buFR = new ArrayList<Feature>();
		for(String ds : new String[] {"R11", "R24", "R27", "R28", "R32", "R44", "R52", "R53", "R75", "R76", "R84", "R93", "R94"}) {
			Collection<Feature> buFR_ = BuildingStatsComputation.getFeatures(basePath + "geodata/fr/bdtopo/BATIMENT"+ds+".gpkg", "ID", "geom", xMin, yMin, xMax, yMax, 1);
			//"(ETAT='En service' AND (USAGE1='Résidentiel' OR USAGE2='Résidentiel'))"
			//logger.info(buFR_.size() + " buildings in " + ds);
			buFR.addAll(buFR_); buFR_.clear();
		}
		//remove duplicates
		buFR = FeatureUtil.removeDuplicates(buFR, null);
		//set country code
		for(Feature f : buFR) f.setAttribute("CC", "FR");
		//
		logger.info(buFR.size() + " buildings");
		bu.addAll(buFR); buFR.clear();
	}


	double floorHeight = 3.5;

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
			else nb = Math.max( (int)(h/floorHeight), 1);
		}

		double contrib = nb*area;

		//type contributions
		String u1 = (String) f.getAttribute("usage_1");
		if(u1 == null || "Indifférencié".equals(u1)) {
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


}
