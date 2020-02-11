package eu.europa.ec.eurostat.jgiscotools.gisco_processes.grid.geomprod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.algo.Decomposer;
import eu.europa.ec.eurostat.jgiscotools.algo.Partition.GeomType;
import eu.europa.ec.eurostat.jgiscotools.algo.base.Union;
import eu.europa.ec.eurostat.jgiscotools.deprecated.CountriesUtil;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.GeoPackageUtil;
import eu.europa.ec.eurostat.jgiscotools.io.SHPUtil;
import eu.europa.ec.eurostat.jgiscotools.util.JTSGeomUtil;

public class DataPreparation {
	static Logger logger = LogManager.getLogger(DataPreparation.class.getName());

	//use: -Xms2G -Xmx6G
	public static void main(String[] args) throws Exception {
		logger.info("Start");

		String path = "E:/workspace/gridstat/data/CNTR_100k/";
		/*
		logger.info("Produce country geometry as the union of different versions");
		produceCountriesUnionVersions(path);

		logger.info("Produce Europe 100k as union of countries");
		SHPUtil.union(path+"CNTR_RG_100K_union_LAEA.shp", path+"Europe_100K_union_LAEA.shp", 0);
		//NB: try iterative union or buffer(0) directly

		//buffering
		int bufferDistance = 1500;
		logger.info("Produce buffers (" + bufferDistance + ") of countries");
		buffer(path+"CNTR_RG_100K_union_LAEA.shp", path+"CNTR_RG_100K_union_buff_" + bufferDistance + "_LAEA.shp", bufferDistance, 4, BufferParameters.CAP_ROUND);
		logger.info("Produce Europe (" + bufferDistance + ") buffer");
		buffer(path+"Europe_100K_union_LAEA.shp", path+"Europe_100K_union_buff_" + bufferDistance + "_LAEA.shp", bufferDistance, 4, BufferParameters.CAP_ROUND);
		 */
		//TODO remove country holes ?

		logger.info("Decompose boundaries");
		Collection<Feature> bn = GeoPackageUtil.getFeatures(path+"CNTR_BN_100K_2016_LAEA.gpkg");
		logger.info(bn.size());
		bn = Decomposer.decomposeFeature(bn, 5000, 200, GeomType.ONLY_LINES, 0);
		for(Feature f : bn) f.setGeometry( JTSGeomUtil.toMulti(f.getGeometry()) );
		logger.info(bn.size());
		GeoPackageUtil.save(bn, path+"CNTR_BN_100K_2016_LAEA_decomposed.gpkg", CRS.decode("EPSG:3035"), true);

		logger.info("End");
	}



	//compute the buffers of features of a SHP file
	public static void buffer(String inFile, String outFile, double bufferDistance, int quadrantSegments, int endCapStyle){

		//load data
		ArrayList<Feature> fs = SHPUtil.getFeatures(inFile);

		for(Feature f : fs) {
			logger.info(f.getAttribute("CNTR_ID"));
			Geometry geom = (Geometry) f.getGeometry();
			Collection<Geometry> geoms = getGeometries(geom);
			logger.info(geoms.size() + " components");
			logger.info("Compute buffers");
			Collection<Geometry> buffs = new ArrayList<>();
			for(Geometry g : geoms)
				buffs.add(g.buffer(bufferDistance, quadrantSegments, endCapStyle));
			logger.info("Compute union of buffers");
			Geometry buff = new CascadedPolygonUnion(buffs ).union();
			if(buff instanceof Polygon) buff = buff.getFactory().createMultiPolygon(new Polygon[] {(Polygon)buff});
			f.setGeometry(buff);
		}

		logger.info("Save");
		SHPUtil.save(fs, outFile, SHPUtil.getCRS(inFile));
	}

	//return list of geometries that are not GeometryCollection
	public static Collection<Geometry> getGeometries(Geometry geom){
		Collection<Geometry> out = new HashSet<Geometry>();
		int nb = geom.getNumGeometries();
		if(nb == 0)
			return out;
		if(nb == 1)
			out.add(geom.getGeometryN(0));
		else
			for(int i=0; i<nb; i++)
				out.addAll( getGeometries(geom.getGeometryN(i)) );
		return out;
	}



	//produce country geometry as the union of different versions
	private static void produceCountriesUnionVersions(String path) throws Exception {
		CoordinateReferenceSystem crs = CRS.decode("EPSG:3035");

		Collection<Feature> cnts = new ArrayList<>();
		for(String cntC : CountriesUtil.EuropeanCountryCodes) {
			logger.info(cntC);

			Collection<Geometry> polys = new ArrayList<Geometry>();
			for(String version : new String[] {"2004","2010","2013","2016"}) {
				logger.info(version);

				Geometry cntGeomV;
				try {
					cntGeomV = CountriesUtil.getEuropeanCountry(cntC, path+"CNTR_RG_100K_"+version+"_LAEA.shp").getGeometry();
				} catch (Exception e) {
					logger.warn("Not found");
					continue;
				}
				cntGeomV = cntGeomV.buffer(0);
				polys.add(cntGeomV);
				//for(Geometry poly : getGeometries(cntGeomV))
				//	polys.add( poly.buffer(0) );
			}

			logger.info("Compute union");
			Geometry cntGeom = Union.polygonsUnionAll(polys);

			Feature cnt = new Feature();
			cnt.setAttribute("CNTR_ID", cntC);
			if(cntGeom instanceof Polygon) cntGeom = cntGeom.getFactory().createMultiPolygon(new Polygon[] {(Polygon)cntGeom});
			cnt.setGeometry(cntGeom);
			cnts.add(cnt);
		}
		logger.info("Save");
		SHPUtil.save(cnts, path+"CNTR_RG_100K_union_LAEA.shp", crs);
	}

}
