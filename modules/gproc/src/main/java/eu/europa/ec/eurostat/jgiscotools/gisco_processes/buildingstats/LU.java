package eu.europa.ec.eurostat.jgiscotools.gisco_processes.buildingstats;

import java.awt.geom.Point2D;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.coverage.grid.GridCoverage2D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.GeoTiffUtil;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.geostat.GridAggregator.MapOperation;
import eu.europa.ec.eurostat.jgiscotools.io.geo.CRSUtil;

public class LU implements BuildingDataLoader, MapOperation<BuildingStat> {
	private static Logger logger = LogManager.getLogger(LU.class.getName());


	public void loadBuildings(Collection<Feature> bu, String basePath, int xMin, int yMin, int xMax, int yMax) {
		Collection<Feature> buLU = BuildingStatsComputation.getFeatures(basePath + "geodata/lu/BD_ACT/BDLTC_SHP/BATIMENT.gpkg", "ID", "geom", xMin, yMin, xMax, yMax, 1);
		for(Feature f : buLU) f.setAttribute("CC", "LU");
		logger.info(buLU.size() + " buildings");
		bu.addAll(buLU);
		buLU.clear();
	}

	double floorHeight = 3.5;

	@Override
	public BuildingStat map(Feature f, Geometry inter) {
		if(inter == null || inter.isEmpty()) return new BuildingStat();
		double area = inter.getArea();
		if(area == 0 ) return new BuildingStat();

		//nb floors
		double elevTop = f.getGeometry().getCoordinate().z;
		Point c = f.getGeometry().getCentroid();
		double elevGround = getElevation(c.getX(), c.getY());
		System.out.println(elevGround);
		double h = elevTop - elevGround ;
		if(h<0) {
			logger.warn("Negative building height - id: " + f.getID() + " - h=" + h);
			return new BuildingStat();
		}
		System.out.println(elevTop + " " + elevGround);
		int nb = (int) (h/floorHeight);
		if(nb<1) {
			logger.warn("Building with no floor - id: " + f.getID() + " - h=" + h);
			return new BuildingStat();
		}
		System.out.println(nb);

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


	private static GridCoverage2D dtm = null;
	private static GridCoverage2D getDTM() {
		if(dtm==null) {
			String f = "H:/ws/geodata/lu/MNT_LIDAR_2019/MNT_lux2017.tif";
			dtm = GeoTiffUtil.getGeoTIFFCoverage(f);
		}
		return dtm;
	}


	private static CoordinateReferenceSystem crs3035 = CRSUtil.getETRS89_LAEA_CRS();
	private static CoordinateReferenceSystem crs2169 = CRSUtil.getCRS(2169);

	private static double getElevation(double xG, double yG) {

		//transform coordinates
		Coordinate c2169_ = CRSUtil.project(new Coordinate(yG,xG), crs3035, crs2169);

		//return value from DTM
		double[] v2 = new double[1];
		getDTM().evaluate(new Point2D.Double(c2169_.x, c2169_.y), v2);
		return v2[0];
	}




	public static void main(String[] args) {

		double x = 4047105;
		double y = 2962350;

		System.out.println(getElevation(x, y));

	}

}
