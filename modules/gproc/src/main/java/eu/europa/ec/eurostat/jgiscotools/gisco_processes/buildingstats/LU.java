package eu.europa.ec.eurostat.jgiscotools.gisco_processes.buildingstats;

import org.locationtech.jts.geom.Geometry;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.geostat.GridAggregator.MapOperation;

public class LU {

	static MapOperation<BuildingStat> mapOp = new MapOperation<>() {
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





}
