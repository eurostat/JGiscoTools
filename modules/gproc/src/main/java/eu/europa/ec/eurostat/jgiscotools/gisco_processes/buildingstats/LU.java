package eu.europa.ec.eurostat.jgiscotools.gisco_processes.buildingstats;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.geometry.Envelope;
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

	@Override
	public BuildingStat map(Feature f, Geometry inter) {
		if(inter == null || inter.isEmpty()) return new BuildingStat();
		double area = inter.getArea();
		if(area == 0 ) return new BuildingStat();

		//nb floors
		Integer nb = 1;
		//double elevTop = f.getGeometry().getCoordinate().z;
		//System.out.println(elevTop);
		Point c = f.getGeometry().getCentroid();
		double elevGround = getElevation(c.getX(), c.getY());
		System.out.println(elevGround);

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
	private static double res = 1; //1m
	private GridCoverage2D getDTM() {
		if(dtm==null) {
			String f = "H:/ws/geodata/lu/MNT_LIDAR_2019/MNT_lux2017.tif";
			dtm = GeoTiffUtil.getGeoTIFFCoverage(f);
		}
		return dtm;
	}



	private double getElevation(double xG, double yG) {
		//open geotiff
		GridCoverage2D dtm = getDTM();
		Envelope envG = dtm.getEnvelope();
		double minGX = envG.getMinimum(0);
		double maxGY = envG.getMaximum(1);

		//TODO convert coordinates from 3035 to 
		CoordinateReferenceSystem crs3035 = CRSUtil.getETRS89_LAEA_CRS();
		CoordinateReferenceSystem crs2169 = CRSUtil.getCRS(2169);
		Coordinate c2169 = CRSUtil.project(new Coordinate(xG,yG), crs3035, crs2169);

		//compute raster position
		int i = (int)((c2169.x-minGX)/res);
		int j = (int)(-(c2169.y-maxGY)/res) -1;

		//get value
		int[] v = new int[1];
		getDTM().evaluate(new GridCoordinates2D(i,j), v);

		return v[0];
	}

}
