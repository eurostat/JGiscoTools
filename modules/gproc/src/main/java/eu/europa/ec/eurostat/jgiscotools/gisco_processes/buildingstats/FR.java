package eu.europa.ec.eurostat.jgiscotools.gisco_processes.buildingstats;

import org.locationtech.jts.geom.Geometry;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.geostat.GridAggregator.MapOperation;

public class FR {

	static MapOperation<BuildingStat> mapOp = new MapOperation<>() {
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

	};

}
