/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health;

import java.util.ArrayList;

import org.geotools.filter.text.cql2.CQL;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.GeoData;

/**
 * 
 * Procedure to integrate healthcare service data into a unified dataset
 * It includes schema change and removal of duplicates
 * 
 * @author clemoki
 *
 */
public class DataIntegrationERMTomTomOSM {


	DataFormattingGeocoding g;

	public static void main(String[] args) throws Exception {
		System.out.println("Start");

		String path = "E:/workspace/gridstat/hospitals/java";

		//mergeInputs(path);
		//https://en.wikipedia.org/wiki/Levenshtein_distance

		//load hospitals
		ArrayList<Feature> hosps = GeoData.getFeatures(path+"healthcare_services1.gpkg");
		System.out.println(hosps.size());

		//MinimumSpanningTree


		System.out.println("End");
	}


	private static double getSimilarity(Feature hosp1, Feature hosp2) throws Exception {
		double distThreshold = 1000;

		double dist = hosp1.getGeometry().distance( hosp2.getGeometry() );
		if(dist>distThreshold) return Double.MAX_VALUE;

		/*
		int distURL = -1;
		String url1= hosp1.getAttribute("url").toString();
		String url2= hosp2.getAttribute("url").toString();
		if(url1.length()!=0 && url2.length()!=0)
			distURL = LevenshteinMatching.getLevenshteinDistance(url1, url2, true, true, true, true);

		int distName = -1;
		String name1= hosp1.getAttribute("name").toString();
		String name2= hosp2.getAttribute("name").toString();
		if(name1.length()!=0 && name2.length()!=0)
			distName = LevenshteinMatching.getLevenshteinDistance(name1, name2, true, true, true, true);
*/

		return 0;
	}




	private static void mergeInputs(String path) throws Exception {

		//target data
		ArrayList<Feature> out = new ArrayList<>();
		CoordinateReferenceSystem crs = CRS.decode("EPSG:3035");

		System.out.println("Load ERM");
		//load ERM dataset
		ArrayList<Feature> erm = GeoData.getFeatures("E:/dissemination/shared-data/ERM/shp/ERM_2019.1_LAEA/Data/GovservP.shp", null, CQL.toFilter("F_CODE = 'AX502' OR F_CODE = 'AX503'"));
		System.out.println(erm.size());
		//46876 features
		//print all attribut labels/keys
		//System.out.println(erm.iterator().next().getAttributes().keySet());
		//[NAMA2, NAMA1, NLN1, NLN2, ICC, FCsubtype, GST, F_CODE, geom, inspireId, CAP, NAMN1, beginLifes, NAMN2, SN]


		for(Feature f : erm) {
			Feature f_ = new Feature();

			//transform f into f_ according to target schema
			f_.setGeometry( f.getGeometry() );
			f_.setAttribute("sourceID", f.getAttribute("inspireId"));
			f_.setAttribute("source", "ERM");
			f_.setAttribute("name", f.getAttribute("NAMA1"));
			f_.setAttribute("url", "");
			String cap = f.getAttribute("CAP").toString();
			f_.setAttribute("cap", cap.equals("N_P")?"":cap.equals("UNK")?"":cap);
			f_.setAttribute("ICC", f.getAttribute("ICC"));
			f_.setAttribute("address", "");
			f_.setAttribute("beginls", f.getAttribute("beginLifes"));
			f_.setAttribute("specialty", "");
			String er = f.getAttribute("F_CODE").toString();
			f_.setAttribute("emergency", er.equals("AX503")?"T":er.equals("AX502")?"F":"UNK");
			f_.setAttribute("type", f.getAttribute("F_CODE")); 

			out.add(f_);
		}

		System.out.println("Load TomTom");

		//load tomtom dataset
		ArrayList<Feature> tom = GeoData.getFeatures("E:/workspace/gridstat/hospitals/mmpoi_pi_healthcare.gpkg", null, CQL.toFilter("FEATTYP = '7321' OR FEATTYP = '7391'"));
		System.out.println(tom.size());
		//Documentation K:\gridstat\hospitals\
		//System.out.println(tom.size()); 28645
		//print all attribut labels/keys
		//System.out.println(tom.iterator().next().getAttributes().keySet());
		//[ID][FEATTYP][PACKAGE][SUBCAT][IMPORT][NAME][LANCD][TELNUM][TEL_TYPE][FAXNUM][HTTP]{COMPNAME][CLTRPELID][RELPOS][EXTPOIID][ADDRPID][POSACCUR][GAL][CONT_SRC][CNT_MOD]
		//GAL: Geocoding Accuracy Level, RELPOS: Relative Position, CONT_SRC: Content Source, CONT_MOD: Content Modified, PACKAGE: Service Group
		//SUBCAT: 7321001 Unspecified, 7321002 General, 7321003	Special, 7321004 Hospital of Chinese Medicine, 7321005 Hospital for Women and Children
		//FEATTYP: 7321 Hospital/Polyclinic, 7391 Emergency Medical Service



		for(Feature f : tom) {
			Feature f_ = new Feature();
			//transform f into f_ according to target schema
			f_.setGeometry( f.getGeometry() );
			f_.setAttribute("sourceID", f.getAttribute("ID"));
			f_.setAttribute("source", "mmpoi_pi");
			f_.setAttribute("name", f.getAttribute("NAME"));
			f_.setAttribute("url", f.getAttribute("HTTP"));
			f_.setAttribute("cap", "");
			f_.setAttribute("ICC", f.getAttribute("LANCD")); 
			f_.setAttribute("address", f.getAttribute("HSNUM") + " " + f.getAttribute("STNAME") + " " + f.getAttribute("POSTCODE") + " " + f.getAttribute("LOCNAME"));  
			f_.setAttribute("beginls", "");
			f_.setAttribute("specialty", f.getAttribute("SUBCAT")); 
			String emergency = f.getAttribute("FEATTYP").toString();
			f_.setAttribute("emergency", emergency.equals("7391")?"T":emergency.equals("7321")?"UNK":"UNK");
			f_.setAttribute("type", f.getAttribute("FEATTYP")); 

			out.add(f_);
		}



		System.out.println("save output");
		GeoData.save(out, path+"healthcare_services1.gpkg", crs, true);
	}


}
