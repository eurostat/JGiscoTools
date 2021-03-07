/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridaccessibility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;
import eu.europa.ec.eurostat.jgiscotools.routing.AccessibilityGrid;
import eu.europa.ec.eurostat.jgiscotools.routing.SpeedCalculator;

/**
 * @author julien Gaffuri
 *
 */
public class BasicServiceAccessibility {
	private static Logger logger = LogManager.getLogger(BasicServiceAccessibility.class.getName());

	//use: -Xms2G -Xmx12G
	public static void main(String[] args) throws Exception {
		logger.info("Start");

		//logger.setLevel(Level.ALL);


		String basePath = "E:/workspace/basic_services_accessibility/";
		String outPath = basePath + "routing_paths/";
		String egPath = "E:/dissemination/shared-data/";
		CoordinateReferenceSystem crs = CRS.decode("EPSG:3035");

		//set the country id (set to null for all countries)
		String cnt = null;

		int resKM = 10;
		logger.info("Load grid cells " + resKM + "km ...");
		String cellIdAtt = "GRD_ID";
		ArrayList<Feature> cells = GeoData.getFeatures(basePath + "grid/grid_"+resKM+"km.gpkg",null, cnt==null?null:CQL.toFilter("CNTR_ID = '"+cnt+"'"));
		logger.info(cells.size() + " cells");


		logger.info("Load network sections...");
		Collection<Feature> networkSections = RoadERM.get(cnt);
		SpeedCalculator sc = RoadERM.getSpeedCalculator();		
		logger.info(networkSections.size() + " sections loaded.");


		final class Case {
			String label, filter;
			double minDurAccMinT;
			public Case(String label, String filter, double minDurAccMinT) {
				this.label = label;
				this.filter = filter;
				this.minDurAccMinT = minDurAccMinT;
			}
		};

		for(Case c : new Case[] {
				new Case("healthcare", "GST = 'GF0703' OR GST = 'GF0306'", 15),
				new Case("educ1", "GST = 'GF090102'", 10),
				new Case("educ2", "GST = 'GF0902'", 20),
				new Case("educ3", "GST = 'GF0904'", 60)
		}) {

			logger.info("Load POIs " + c.label + "...");
			ArrayList<Feature> pois = GeoData.getFeatures(egPath+"ERM/gpkg/ERM_2019.1_LAEA/GovservP.gpkg", null, CQL.toFilter("("+c.filter +")"+ (cnt==null?"":" AND (ICC = '"+cnt+"')") ));
			logger.info(pois.size() + " POIs");


			logger.info("Build accessibility...");
			AccessibilityGrid ag = new AccessibilityGrid(cells, cellIdAtt, resKM*1000, pois, networkSections, "TOT_P_2011", c.minDurAccMinT);
			ag.setEdgeWeighter(sc);

			logger.info("Compute accessibility...");
			ag.compute();

			logger.info("Save data...");
			CSVUtil.save(ag.getCellData(), outPath + "accessibility_"+(cnt==null?"":cnt+"_")+resKM+"km"+"_"+c.label+".csv");
			logger.info("Save routes... Nb=" + ag.getRoutes().size());
			GeoData.save(ag.getRoutes(), outPath + "routes_"+(cnt==null?"":cnt+"_")+resKM+"km"+"_"+c.label+".gpkg", crs, true);

		}

		logger.info("End");
	}




	//TODO move to graph builder
	public Collection<Feature> decomposeSectionsRoutable(Collection<Feature> sections) {

		//build line merger
		LineMerger lm = new LineMerger();
		for(Feature f : sections)
			if(f.getGeometry()!=null && !f.getGeometry().isEmpty())
				lm.add(f.getGeometry());

		//run linemerger
		Collection<?> ls_ = lm.getMergedLineStrings();
		lm = null;

		//index lines
		STRtree index = new STRtree();
		for(Object ls : ls_) index.insert(((Geometry)ls).getEnvelopeInternal(), ls);
		ls_.clear(); ls_ = null;

		//build output features
		Collection<Feature> out = new ArrayList<>();
		for(Feature f : sections) {
			Geometry g = f.getGeometry();
			if(g==null || g.isEmpty()) continue;

			//get lines nearby feature geometry
			List<?> ls = index.query(g.getEnvelopeInternal());
			for(Object line_ : ls) {
				Geometry line = (Geometry)line_;
				if(! g.getEnvelopeInternal().intersects(line.getEnvelopeInternal())) continue;
				if(! g.contains(line)) continue;

				//make new feature
				Feature f2 = new Feature();
				f2.setGeometry(line);
				f2.getAttributes().putAll(f.getAttributes());
				out.add(f2);
			}
		}

		return out;
	}




}
