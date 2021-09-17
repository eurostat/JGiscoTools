/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.nutscommgen;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.TesselationGeneralisation;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.TesselationGeneralisationSpecification;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agenface.AFace;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agenface.CFaceContainPoints;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agenface.CFaceSize;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agenface.CFaceValidity;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentedge.AEdge;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentedge.CEdgeFaceSize;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentedge.CEdgeGranularity;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentedge.CEdgeNoTriangle;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentedge.CEdgeValidity;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentedge.CEdgesFacesContainPoints;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agenttesselation.ATesselation;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentunit.AUnit;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentunit.CUnitContainPoints;
import eu.europa.ec.eurostat.jgiscotools.regionsimplify.agentunit.CUnitNoNarrowGaps;
import eu.europa.ec.eurostat.jgiscotools.util.CRSType;

/**
 * @author julien Gaffuri
 *
 */
public class MainGenCommPlus100k {
	private final static Logger LOGGER = LogManager.getLogger(MainGenCommPlus100k.class.getName());
	//-Xmx13g -Xms2g -XX:-UseGCOverheadLimit
	//-XX:-UseGCOverheadLimit
	//-XX:+UseG1GC -XX:G1HeapRegionSize=n -XX:MaxGCPauseMillis=m  
	//-XX:ParallelGCThreads=n -XX:ConcGCThreads=n

	//projs=("etrs89 4258" "wm 3857" "laea 3035")
	//ogr2ogr -overwrite -f "ESRI Shapefile" "t.shp" "s.shp" -t_srs EPSG:3857 -s_srs EPSG:4258
	//ogr2ogr -overwrite -f "ESRI Shapefile" "GAUL_CLEAN_DICE_DISSOLVE_WM.shp" "GAUL_CLEAN_DICE_DISSOLVE.shp" -t_srs EPSG:3857 -s_srs EPSG:4258
	//ogr2ogr -overwrite -f "ESRI Shapefile" "EEZ_RG_100K_2013_WM.shp" "EEZ_RG_100K_2013.shp" -t_srs EPSG:3857 -s_srs EPSG:4258

	public static void main(String[] args) {
		LOGGER.info("Start");

		//GraphBuilder.LOGGER.setLevel(Level.WARN);

		String basePath = "/home/juju/Bureau/nuts_gene_data/";

		LOGGER.info("Load data");
		String rep="100k_1M/commplus"; String inFile = basePath+"commplus/COMM_PLUS.shp";
		Collection<Feature> units = GeoData.getFeatures(inFile);
		for(Feature f : units) for(String id : new String[] {"NUTS_ID","COMM_ID","idgene","GISCO_ID"}) if(f.getAttribute(id) != null) f.setID( ""+f.getAttribute(id) );

		for(int i=1; i<=100; i++) {

			//define specifications
			TesselationGeneralisationSpecification specs = new TesselationGeneralisationSpecification(1e6, CRSType.CARTO) {
				public void setUnitConstraints(ATesselation t) {
					for(AUnit a : t.aUnits) {
						a.addConstraint(new CUnitNoNarrowGaps(a, res.getSeparationDistanceMeter(), 1e-5, 5, true, true).setPriority(10));
						//a.addConstraint(new CUnitNoNarrowParts(a, res.getSeparationDistanceMeter(), 1e-5, 5, true).setPriority(9));
						a.addConstraint(new CUnitContainPoints(a));
					}
				}
				public void setTopologicalConstraints(ATesselation t) {
					for(AFace a : t.aFaces) {
						a.addConstraint(new CFaceSize(a, 0.1*res.getPerceptionSizeSqMeter(), 3*res.getPerceptionSizeSqMeter(), res.getPerceptionSizeSqMeter(), true, true).setPriority(2));
						a.addConstraint(new CFaceValidity(a));
						a.addConstraint(new CFaceContainPoints(a));
						a.addConstraint(new CFaceEEZInLand(a).setPriority(10));
					}
					for(AEdge a : t.aEdges) {
						a.addConstraint(new CEdgeGranularity(a, 2*res.getResolutionM()));
						a.addConstraint(new CEdgeValidity(a));
						a.addConstraint(new CEdgeNoTriangle(a));
						a.addConstraint(new CEdgeFaceSize(a).setImportance(6));
						a.addConstraint(new CEdgesFacesContainPoints(a));
					}
				}
			};

			LOGGER.info("Launch generalisation " + i);
			units = TesselationGeneralisation.runGeneralisation(units, null, specs, false, 1, 1000000, 1000);

			LOGGER.info("Run GC");
			System.gc();

			LOGGER.info("Save output data");
			GeoData.save(units, basePath+"out/"+ rep+"/COMM_PLUS_WM_1M_"+i+".shp", GeoData.getCRS(inFile));
		}

		LOGGER.info("End");
	}

}
