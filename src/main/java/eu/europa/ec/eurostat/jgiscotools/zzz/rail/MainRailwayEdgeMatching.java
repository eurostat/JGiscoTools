/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco.rail;

import java.util.ArrayList;
import java.util.HashMap;

import eu.europa.ec.eurostat.jgiscotools.algo.edgematching.NetworkEdgeMatching;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.graph.GraphToFeature;
import eu.europa.ec.eurostat.jgiscotools.io.SHPUtil;

/**
 * @author julien Gaffuri
 *
 */
public class MainRailwayEdgeMatching {

	//TODO: improve input file:
	//projection 3035
	//features with null geometries for IT and RO.
	//Multi geoms for IT.
	//overlapping features for PT.
	//projection issue for EL
	//fix DK.
	//get attributes.
	//get more countries.
	//get better resolution (PT, IE, DE, PL).


	public static void main(String[] args) throws Exception {

		//resolution data
		HashMap<String,Double> resolutions = new HashMap<String,Double>();
		resolutions.put("BE", 1.0);
		resolutions.put("LU", 1.5);
		resolutions.put("AT", 1.5);
		resolutions.put("NL", 5.0);
		resolutions.put("NO", 5.0);
		resolutions.put("EE", 5.0);
		resolutions.put("CH", 6.0);
		resolutions.put("SE", 6.0);
		resolutions.put("FR", 7.9);
		resolutions.put("ES", 8.0);
		resolutions.put("FI", 8.0);
		resolutions.put("UK", 9.0);
		resolutions.put("IT", 14.0);
		resolutions.put("PL", 25.0);
		resolutions.put("DE", 50.0);
		resolutions.put("IE", 70.0);
		resolutions.put("RO", 100.0);
		resolutions.put("PT", 500.0);

		//TODO
		resolutions.put("EL", 500.0);
		resolutions.put("DK", 500.0);



		System.out.println("Load input sections");
		String basePath = "/home/juju/Bureau/gisco_rail/";
		//Filter fil = CQL.toFilter( "CNTR <> 'RO' AND CNTR <> 'EL' AND CNTR <> 'DK' AND CNTR <> 'EE'" );
		ArrayList<Feature> secs = SHPUtil.loadSHP(basePath+"MS_data/RailwayLinkClean.shp").fs;
		System.out.println(secs.size());

		//compute edge matching
		NetworkEdgeMatching nem = new NetworkEdgeMatching(secs, resolutions, 1.5, "CNTR", "EM");
		secs = null;
		nem.makeEdgeMatching();

		System.out.println("Save matching edges " + nem.getMatchingEdges().size());
		SHPUtil.saveSHP(GraphToFeature.asFeature(nem.getMatchingEdges()), basePath+"out/EM/matching_edges.shp", SHPUtil.getCRS(basePath+"MS_data/RailwayLink.shp"));

		System.out.println("Save output " + nem.getSections().size());
		SHPUtil.saveSHP(nem.getSections(), basePath+"out/EM/RailwayLinkEM.shp", SHPUtil.getCRS(basePath+"MS_data/RailwayLink.shp"));

		System.out.println("End");
	}

}
