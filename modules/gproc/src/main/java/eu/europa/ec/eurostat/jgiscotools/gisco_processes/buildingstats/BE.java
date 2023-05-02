package eu.europa.ec.eurostat.jgiscotools.gisco_processes.buildingstats;

import org.locationtech.jts.geom.Geometry;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.geostat.GridAggregator.MapOperation;

public class BE {

	static MapOperation<BuildingStat> mapOp = new MapOperation<>() {
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
