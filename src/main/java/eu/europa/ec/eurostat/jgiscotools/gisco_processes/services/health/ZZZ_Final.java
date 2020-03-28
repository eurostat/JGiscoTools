/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.europa.ec.eurostat.jgiscotools.deprecated.NUTSUtils;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoData;
import eu.europa.ec.eurostat.jgiscotools.util.ProjectionUtil;

/**
 * @author julien Gaffuri
 *
 */
public class ZZZ_Final {

	//TODO move stuff to CSVUtils

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Start");

		String path = "/home/juju/Bureau/workspace/healthcare-services/";
		String timeStamp = new SimpleDateFormat("yyyy_MM_dd").format(Calendar.getInstance().getTime());
		System.out.println(timeStamp);

		//the desired columns, ordered
		String[] cols = new String[] {
				"id", "hospital_name", "site_name", "lat", "lon", "street", "house_number", "postcode", "city", "cc", "country", "emergency", "cap_beds", "cap_prac", "cap_rooms", "facility_type", "public_private", "list_specs", "tel", "email", "url", "ref_date", "pub_date"
		};
		List<String> cols_ = Arrays.asList(cols);

		ArrayList<Map<String, String>> all = new ArrayList<Map<String, String>>();

		//load
		for(String cc : new String[] { "AT", "BE", "DE", "DK", "ES", "FI", "FR", "IE", "IT", "LU", "LV", "NL", "PT", "RO", "SE", "UK" }) {
			System.out.println("*** " + cc);

			//load data
			ArrayList<Map<String, String>> data = CSVUtil.load(path+"temp/"+cc+".csv");
			System.out.println(data.size());
			//System.out.println(data.iterator().next().keySet());

			CSVUtil.removeColumn(data, "latGISCO");
			CSVUtil.removeColumn(data, "lonGISCO");
			CSVUtil.removeColumn(data, "x");
			CSVUtil.removeColumn(data, "y");
			CSVUtil.removeColumn(data, "Column name (code)");
			CSVUtil.	removeColumn(data, "county");
			CSVUtil.removeColumn(data, "suburb");
			CSVUtil.	removeColumn(data, "state");
			CSVUtil.removeColumn(data, "Sort_Ratio");
			CSVUtil.removeColumn(data, "Set_Ratio");
			CSVUtil.removeColumn(data, "Ratio");
			CSVUtil.removeColumn(data, "Coordinates");
			CSVUtil.removeColumn(data, "Output Geocoder");
			CSVUtil.removeColumn(data, "Input Geocoder");
			CSVUtil.removeColumn(data, "Part_Ratio");
			CSVUtil.removeColumn(data, "address");
			CSVUtil.removeColumn(data, "extension");
			CSVUtil.removeColumn(data, "Address");
			CSVUtil.removeColumn(data, "organization id");
			CSVUtil.removeColumn(data, "postal district");
			CSVUtil.removeColumn(data, "service industry id");
			CSVUtil.removeColumn(data, "district");
			CSVUtil.removeColumn(data, "organization name");
			CSVUtil.removeColumn(data, "geo_matching");
			CSVUtil.removeColumn(data, "geo_confidence");

			CSVUtil.renameColumn(data, "latBing", "lat");
			CSVUtil.renameColumn(data, "lonBing", "lon");
			CSVUtil.renameColumn(data, "name", "hospital_name");
			CSVUtil.renameColumn(data, "type", "facility_type");
			CSVUtil.renameColumn(data, "year", "ref_date");
			CSVUtil.renameColumn(data, "data_year", "pub_date");

			//System.out.println(data.iterator().next().keySet());

			Set<String> ch = ValidateCSV.checkNoUnexpectedColumn(data, cols_);
			if(ch.size()>0) System.err.println(ch);

			//populateAllColumns(data, cols, "");

			CSVUtil.replaceValue(data, "", null);
			CSVUtil.replaceValue(data, "NA", null);
			CSVUtil.replaceValue(data, "UNKNOWN", null);


			//hospital_name,Region,Country,Adresse,lat,lon


			if(cc.equals("ES")) {				
				CSVUtil.replaceValue(data, "SEGURIDAD SOCIAL", "public");
				CSVUtil.replaceValue(data, "SEGURIDAD SOCIAL", "public");
				CSVUtil.replaceValue(data, "PRIVADO NO BENÉFICO", "private");
				CSVUtil.replaceValue(data, "OTRO PRIVADO BENÉFICO", "private");
				CSVUtil.replaceValue(data, "PRIVADO-BENÉFICO (CRUZ ROJA)", "private");
				CSVUtil.replaceValue(data, "PRIVADO-BENÉFICO (IGLESIA)", "private");
				CSVUtil.replaceValue(data, "OTRA DEPENDENCIA PATRIMONIAL", "private");
				CSVUtil.replaceValue(data, "MATEP", "public"); //TODO ES - check that really?
				CSVUtil.replaceValue(data, "ENTIDADES PÚBLICAS", "public");
				CSVUtil.replaceValue(data, "MUNICIPIO", "public");
				CSVUtil.replaceValue(data, "MINISTERIO DE INTERIOR", "public");
				CSVUtil.replaceValue(data, "MINISTERIO DE DEFENSA", "public");
				CSVUtil.replaceValue(data, "DIPUTACIÓN O CABILDO", "public"); //TODO ES - check that really?
				CSVUtil.replaceValue(data, "COMUNIDAD AUTÓNOMA", "public");
			}
			if(cc.equals("BE")) {
				CSVUtil.replaceValue(data, "Public", "public");
				CSVUtil.replaceValue(data, "Openbaar - Public", "public");
				CSVUtil.replaceValue(data, "Openbaar", "public");
				CSVUtil.replaceValue(data, "Privé", "private");
				CSVUtil.replaceValue(data, "Privaat", "private");
				CSVUtil.replaceValue(data, "Privaat - Privé", "private");
			}


			for(Map<String, String> h : data)
				h.put("pub_date", timeStamp);

			//cc, country
			for(Map<String, String> h :data) {
				h.put("cc", cc);
				String cntr = NUTSUtils.getName(cc);
				if(cntr == null) System.err.println("cc: " + cc);
				h.put("country", cntr);
			}
			CSVUtil.replaceValue(data, "Germany (until 1990 former territory of the FRG)", "Germany");


			//System.out.println(data.iterator().next().keySet());

			//export as geojson and GPKG
			CSVUtil.save(data, path+"data/csv/"+cc+".csv", cols_);
			Collection<Feature> fs = CSVUtil.CSVToFeatures(data, "lon", "lat");
			applyTypes(fs);
			GeoData.save(fs, path+"data/geojson/"+cc+".geojson", ProjectionUtil.getWGS_84_CRS());
			GeoData.save(fs, path+"data/gpkg/"+cc+".gpkg", ProjectionUtil.getWGS_84_CRS());

			//store for big EU file
			all.addAll(data);
		}

		//export all
		System.out.println("*** All");
		System.out.println(all.size());
		CSVUtil.save(all, path+"data/csv/all.csv", cols_);
		Collection<Feature> fs = CSVUtil.CSVToFeatures(all, "lon", "lat");
		applyTypes(fs);
		GeoData.save(fs, path+"data/geojson/all.geojson", ProjectionUtil.getWGS_84_CRS());
		GeoData.save(fs, path+"data/gpkg/all.gpkg", ProjectionUtil.getWGS_84_CRS());

		System.out.println("End");
	}

	private static void applyTypes(Collection<Feature> fs) {
		for(Feature f : fs) {
			for(String att : new String[] {"cap_beds", "cap_prac", "cap_rooms"}) {
				Object v = f.getAttribute(att);
				if(v==null) continue;
				if("".equals(v)) f.setAttribute(att, null);
				else f.setAttribute(att, Integer.parseInt(v.toString()));
			}
			for(String att : new String[] {"lat", "lon"}) {
				Object v = f.getAttribute(att);
				if(v==null) continue;
				if("".equals(v)) f.setAttribute(att, null);
				else f.setAttribute(att, Double.parseDouble(v.toString()));
			}
		}
	}

}
