package eu.europa.ec.eurostat.jgiscotools.gisco_processes.buildingstats;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.geostat.GridAggregator.MapOperation;

public class BE implements CountryHandler {
	private static Logger logger = LogManager.getLogger(BE.class.getName());


	public void loadBuildings(Collection<Feature> bu, String basePath, int xMin, int yMin, int xMax, int yMax) {
		Collection<Feature> buBE = new ArrayList<Feature>();
		for(String ds : new String[] {"PICC_vDIFF_SHAPE_31370_PROV_BRABANT_WALLON", "PICC_vDIFF_SHAPE_31370_PROV_HAINAUT", "PICC_vDIFF_SHAPE_31370_PROV_LIEGE", "PICC_vDIFF_SHAPE_31370_PROV_LUXEMBOURG", "PICC_vDIFF_SHAPE_31370_PROV_NAMUR"}) {
			Collection<Feature> buBE_ = BuildingStatsComputation.getFeatures(basePath + "geodata/be/"+ds+"/CONSTR_BATIEMPRISE.gpkg", xMin, yMin, xMax, yMax, 1, "GEOREF_ID");
			logger.info(ds + " " + buBE_.size() + " buildings");
			buBE.addAll(buBE_); buBE_.clear();
		}
		//remove duplicates
		buBE = FeatureUtil.removeDuplicates(buBE, "ID");
		//set country code
		for(Feature f : buBE) f.setAttribute("CC", "BE");
		//
		bu.addAll(buBE); buBE.clear();
	}



	MapOperation<BuildingStat> mapOp = new MapOperation<>() {
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


}
