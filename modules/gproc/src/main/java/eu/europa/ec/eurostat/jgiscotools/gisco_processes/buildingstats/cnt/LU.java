package eu.europa.ec.eurostat.jgiscotools.gisco_processes.buildingstats.cnt;

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
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.buildingstats.BuildingDataLoader;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.buildingstats.BuildingStat;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.buildingstats.BuildingStatsComputation;
import eu.europa.ec.eurostat.jgiscotools.io.geo.CRSUtil;

public class LU implements BuildingDataLoader, MapOperation<BuildingStat> {
	private static Logger logger = LogManager.getLogger(LU.class.getName());

	//TODO check negative values
	//CRS3035RES1000mN2950000E4040000,127,-938563041,56796,88296
	//(1.0770982282552435E12, 126.5510473301154, 56795.566374753056, 88296.19223350796)

	public void loadBuildings(Collection<Feature> bu, String basePath, int xMin, int yMin, int xMax, int yMax) {
		Collection<Feature> buLU = BuildingStatsComputation.getFeatures(basePath + "geodata/lu/BD_ACT/BDLTC_SHP/BATIMENT.gpkg", "ID", "geom", xMin, yMin, xMax, yMax, 1);
		for(Feature f : buLU) f.setAttribute("CC", "LU");
		for(Feature f : buLU) f.setGeometry(f.getGeometry().buffer(0));
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

		//roof elevation
		double elevTop = f.getGeometry().getCoordinate().z;
		if(elevTop > 1E6) return new BuildingStat();
		if(elevTop > 3000) {
			logger.warn("Building with too high roof top: " + elevTop + " - id=" + f.getID());
			return new BuildingStat();
		}

		//ground elevation
		Point c = f.getGeometry().getCentroid();
		double elevGround = getElevation(c.getX(), c.getY());

		if(elevGround == -32767.0) return new BuildingStat();
		if(elevGround < -200) {
			logger.warn("Building with too low ground elevation: " + elevGround + " - pos=" + c.getCoordinate());
			return new BuildingStat();
		}

		//height
		double h = elevTop - elevGround ;
		if(h<1) {
			//if(h<0) {
			//logger.warn("Negative building height - id: " + f.getID() + " - h=" + h);
			//logger.warn("  elevTop:" + elevTop + " - elevGround:" + elevGround);
			//logger.warn("  " + (int)c.getCoordinate().y + " " + (int)c.getCoordinate().x);
			return new BuildingStat();
		}
		if(h > 100) {
			logger.warn("Building with too high height: " + h + " - pos=" + c.getCoordinate());
			return new BuildingStat();
		}

		//number of floors
		int nb = (int) (h/floorHeight);
		if(nb<1) {
			nb = 1;
			//logger.warn("Building with no floor - id: " + f.getID() + " - h=" + h);
			//logger.warn("  elevTop:" + elevTop + " - elevGround:" + elevGround);
			//logger.warn("  " + (int)c.getCoordinate().y + " " + (int)c.getCoordinate().x);
			//return new BuildingStat();
		}

		double contrib = nb * area;

		BuildingStat bs = new BuildingStat();

		Object n = f.getAttribute("NATURE");
		if(n==null) {
			bs.res = contrib;
		} else {
			String nS = f.getAttribute("NATURE").toString();
			String nS1 = nS.subSequence(0, 1) + "";
			if("0".equals(nS)) bs.res = contrib;
			else if(nS1.equals("1")) bs.indus = contrib;
			else if(nS1.equals("2")) bs.agri = contrib;
			else if(nS1.equals("3")) bs.commServ = contrib;
			else if( "41206".equals(nS) || "41207".equals(nS) || "41208".equals(nS) ) bs.res = contrib;
			else if(nS1.equals("4")) bs.commServ = contrib;
			else if(nS1.equals("5")) bs.commServ = contrib;
			else if("60000".equals(nS)) {}
			else if(nS1.equals("7")) bs.commServ = contrib;
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
	private static synchronized GridCoverage2D getDTM() {
		if(dtm==null) {
			logger.info("Load LU DTM...");
			String f = "H:/ws/geodata/lu/MNT_LIDAR_2019/MNT_lux2017.tif";
			dtm = GeoTiffUtil.getGeoTIFFCoverage(f);
		}
		return dtm;
	}


	private static CoordinateReferenceSystem crs3035 = CRSUtil.getETRS89_LAEA_CRS();
	private static CoordinateReferenceSystem crs2169 = CRSUtil.getCRS(2169);

	private static double getElevation(double xG, double yG) {

		//transform coordinates
		Coordinate c = CRSUtil.project(new Coordinate(yG, xG), crs3035, crs2169);

		//return value from DTM
		double[] v2 = new double[1];
		getDTM().evaluate(new Point2D.Double(c.y, c.x), v2);
		return v2[0];
	}

	/*
	public static void main(String[] args) {
		//elevGround:301.0 - bad
		//expected: 268

		//2169
		//System.out.println(getElevation(78303, 66616));

		//3035
		System.out.println(getElevation(4042022, 2943035));

	}*/

}
