package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.changedetection.ChangeDetection;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoPackageUtil;

public class ChangeEBM {
	private final static Logger LOGGER = LogManager.getLogger(ChangeEBM.class.getName());

	public static void main(String[] args) {
		LOGGER.info("Start");
		String path = "E:/dissemination/shared-data/EBM/gpkg/";
		String outpath = "E:/workspace/EBM_2019_2020_comparison/comparison_/";

		ArrayList<Feature> fsIni = GeoPackageUtil.getFeatures(path+"EBM_2019_LAEA/EBM_A.gpkg");
		LOGGER.info("Ini="+fsIni.size());
		ArrayList<Feature> fsFin = GeoPackageUtil.getFeatures(path+"EBM_2020_LAEA/EBM_A.gpkg");
		LOGGER.info("Fin="+fsFin.size());

		FeatureUtil.setId(fsIni, "inspireId");
		FeatureUtil.setId(fsFin, "inspireId");

		LOGGER.info("check ids:");
		LOGGER.info( FeatureUtil.checkIdentfier(fsIni, "inspireId") );
		LOGGER.info( FeatureUtil.checkIdentfier(fsFin, "inspireId") );

		LOGGER.info("change detection");
		ChangeDetection cd = new ChangeDetection(fsIni, fsFin);
		cd.setAttributesToIgnore("OBJECTID");

		Collection<Feature> unchanged = cd.getUnchanged();
		LOGGER.info("unchanged = "+unchanged.size());
		Collection<Feature> changes = cd.getChanges();
		LOGGER.info("changes = "+changes.size());
		Collection<Feature> hfgeoms = cd.getHdgeomChanges();
		LOGGER.info("hfgeoms = "+hfgeoms.size());
		Collection<Feature> geomch = cd.getGeomChanges();
		LOGGER.info("geomch = "+geomch.size());
		Collection<Feature> sus = ChangeDetection.findIdStabilityIssues(changes);
		LOGGER.info("suspect changes = "+sus.size());

		CoordinateReferenceSystem crs = GeoPackageUtil.getCRS(path+"EBM_2019_LAEA/EBM_A.gpkg");
		GeoPackageUtil.save(changes, outpath+"changes.gpkg", crs, true);
		GeoPackageUtil.save(unchanged, outpath+"unchanged.gpkg", crs, true);
		GeoPackageUtil.save(hfgeoms, outpath+"hfgeoms.gpkg", crs, true);
		GeoPackageUtil.save(geomch, outpath+"geomch.gpkg", crs, true);
		GeoPackageUtil.save(sus, outpath+"suspects.gpkg", crs, true);

		LOGGER.info("End");
	}

}
