/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services;

import java.util.ArrayList;

import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.GeoPackageUtil;

/**
 * 
 * Procedure to integrate healthcare service data into a unified dataset
 * It includes schema change and removal of duplicates
 * 
 * @author clemoki
 *
 */
public class HealthCareDataIntegration {



	public static void main(String[] args) throws Exception {
		System.out.println("Start");

		String path = "E:/workspace/gridstat/hospitals/";

		//target data
		ArrayList<Feature> out = new ArrayList<>();
		CoordinateReferenceSystem crs = CRS.decode("EPSG:3035");

		
		/*/load ERM dataset
		ArrayList<Feature> erm = SHPUtil.getFeatures("E:/dissemination/shared-data/ERM/shp-gdb/ERM_2019.1_shp_LAEA/Data/GovservP.shp", CQL.toFilter("F_CODE = 'AX502' OR F_CODE = 'AX503'"));
		System.out.println(erm.size());
		//46876 features
		//print all attribut labels/keys
		//System.out.println(erm.iterator().next().getAttributes().keySet());
		//[NAMA2, NAMA1, NLN1, NLN2, ICC, FCsubtype, GST, F_CODE, geom, inspireId, CAP, NAMN1, beginLifes, NAMN2, SN]
		//TODO check what SN attribute is


		for(Feature f : erm) {
			Feature f_ = new Feature();

			//transform f into f_ according to target schema
			f_.setDefaultGeometry( f.getDefaultGeometry() );
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
			f_.setAttribute("emergency", "AX503".equals(f.getAttribute("F_CODE"))?1:0 );
			f_.setAttribute("type", f.getAttribute("F_CODE"));

			out.add(f_);
		}
*/


		//load tomtom dataset
		//TODO
		ArrayList<Feature> tom = GeoPackageUtil.getFeatures("E:/workspace/gridstat/hospitals/mmpoi_pi_healthcare_7321.gpkg");
		//System.out.println(tom.size()); 28645
		//print all attribut labels/keys
		//System.out.println(tom.iterator().next().getAttributes().keySet());
		//[BRANDNAME, POSTCODE, RELPOS, CLTRPELID, STNAME, geom, TELNUM, EMAIL, IMPORT, PACKAGE, LOCNAME, CONT_SRC, FEATTYP, LANCD, ID, EXTPOIID, HSNUM, HTTP, NAME, COMPNAME, GAL, CONT_MOD, POSACCUR, SUBCAT, FAXNUM, ADDRPID, TEL_TYPE]
		

/*
		for(Feature f : erm) {
			Feature f_ = new Feature();

			//transform f into f_ according to target schema
			f_.setDefaultGeometry( f.getDefaultGeometry() );
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
			f_.setAttribute("emergency", "AX503".equals(f.getAttribute("F_CODE"))?1:0 );
			f_.setAttribute("type", f.getAttribute("F_CODE"));

			out.add(f_);
		}
*/
		
		
		System.out.println("save output");
		GeoPackageUtil.save(out, path+"healthcare_services.gpkg", crs, true);

		System.out.println("End");
	}

}
