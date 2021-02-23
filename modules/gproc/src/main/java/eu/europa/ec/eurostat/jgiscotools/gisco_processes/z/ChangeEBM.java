package eu.europa.ec.eurostat.jgiscotools.gisco_processes.z;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.geodiff.GeoDiff;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

public class ChangeEBM {
	private final static Logger LOGGER = LogManager.getLogger(ChangeEBM.class.getName());

	public static void main(String[] args) {
		LOGGER.info("Start");
		String path = "E:/dissemination/shared-data/EBM/gpkg/";
		String outpath = "E:/workspace/EBM_2019_2020_comparison/comparison/";

		ArrayList<Feature> fsIni = GeoData.getFeatures(path+"EBM_2019_LAEA/EBM_A.gpkg", "inspireId");
		LOGGER.info("Ini="+fsIni.size());
		ArrayList<Feature> fsFin = GeoData.getFeatures(path+"EBM_2020_LAEA/EBM_A.gpkg", "inspireId");
		LOGGER.info("Fin="+fsFin.size());

		//LOGGER.info("check ids:");
		//LOGGER.info( FeatureUtil.checkIdentfier(fsIni, "inspireId") );
		//LOGGER.info( FeatureUtil.checkIdentfier(fsFin, "inspireId") );

		LOGGER.info("change detection");
		GeoDiff cd = new GeoDiff(fsIni, fsFin);
		cd.setAttributesToIgnore("OBJECTID");

		Collection<Feature> unchanged = cd.getIdentical();
		LOGGER.info("unchanged = "+unchanged.size());
		Collection<Feature> changes = cd.getDifferences();
		LOGGER.info("changes = "+changes.size());
		Collection<Feature> hfgeoms = cd.getHausdorffGeomDifferences();
		LOGGER.info("hfgeoms = "+hfgeoms.size());
		Collection<Feature> geomch = cd.getGeomDifferences();
		LOGGER.info("geomch = "+geomch.size());
		Collection<Feature> sus = GeoDiff.findIdStabilityIssues(changes, 500);
		LOGGER.info("suspect changes = "+sus.size());

		CoordinateReferenceSystem crs = GeoData.getCRS(path+"EBM_2019_LAEA/EBM_A.gpkg");
		GeoData.save(changes, outpath+"changes.gpkg", crs, true);
		GeoData.save(unchanged, outpath+"unchanged.gpkg", crs, true);
		GeoData.save(hfgeoms, outpath+"hfgeoms.gpkg", crs, true);
		GeoData.save(geomch, outpath+"geomch.gpkg", crs, true);
		GeoData.save(sus, outpath+"suspects.gpkg", crs, true);

		LOGGER.info("End");
	}

}
