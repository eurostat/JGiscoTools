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

			HCUtils.removeColumn(data, "latGISCO");
			HCUtils.removeColumn(data, "lonGISCO");
			HCUtils.removeColumn(data, "x");
			HCUtils.removeColumn(data, "y");
			HCUtils.removeColumn(data, "Column name (code)");
			HCUtils.	removeColumn(data, "county");
			HCUtils.removeColumn(data, "suburb");
			HCUtils.	removeColumn(data, "state");
			HCUtils.removeColumn(data, "Sort_Ratio");
			HCUtils.removeColumn(data, "Set_Ratio");
			HCUtils.removeColumn(data, "Ratio");
			HCUtils.removeColumn(data, "Coordinates");
			HCUtils.removeColumn(data, "Output Geocoder");
			HCUtils.removeColumn(data, "Input Geocoder");
			HCUtils.removeColumn(data, "Part_Ratio");
			HCUtils.removeColumn(data, "address");
			HCUtils.removeColumn(data, "extension");
			HCUtils.removeColumn(data, "Address");
			HCUtils.removeColumn(data, "organization id");
			HCUtils.removeColumn(data, "postal district");
			HCUtils.removeColumn(data, "service industry id");
			HCUtils.removeColumn(data, "district");
			HCUtils.removeColumn(data, "organization name");
			HCUtils.removeColumn(data, "geo_matching");
			HCUtils.removeColumn(data, "geo_confidence");

			HCUtils.changeColumnName(data, "latBing", "lat");
			HCUtils.changeColumnName(data, "lonBing", "lon");
			HCUtils.changeColumnName(data, "name", "hospital_name");
			HCUtils.changeColumnName(data, "type", "facility_type");
			HCUtils.changeColumnName(data, "year", "ref_date");
			HCUtils.changeColumnName(data, "data_year", "pub_date");

			//System.out.println(data.iterator().next().keySet());

			Set<String> ch = ValidateCSV.checkNoUnexpectedColumn(data, cols_);
			if(ch.size()>0) System.err.println(ch);

			//populateAllColumns(data, cols, "");

			HCUtils.replaceValue(data, "", null);
			HCUtils.replaceValue(data, "NA", null);
			HCUtils.replaceValue(data, "UNKNOWN", null);


			//hospital_name,Region,Country,Adresse,lat,lon


			if(cc.equals("ES")) {				
				HCUtils.replaceValue(data, "SEGURIDAD SOCIAL", "public");
				HCUtils.replaceValue(data, "SEGURIDAD SOCIAL", "public");
				HCUtils.replaceValue(data, "PRIVADO NO BENÉFICO", "private");
				HCUtils.replaceValue(data, "OTRO PRIVADO BENÉFICO", "private");
				HCUtils.replaceValue(data, "PRIVADO-BENÉFICO (CRUZ ROJA)", "private");
				HCUtils.replaceValue(data, "PRIVADO-BENÉFICO (IGLESIA)", "private");
				HCUtils.replaceValue(data, "OTRA DEPENDENCIA PATRIMONIAL", "private");
				HCUtils.replaceValue(data, "MATEP", "public"); //TODO ES - check that really?
				HCUtils.replaceValue(data, "ENTIDADES PÚBLICAS", "public");
				HCUtils.replaceValue(data, "MUNICIPIO", "public");
				HCUtils.replaceValue(data, "MINISTERIO DE INTERIOR", "public");
				HCUtils.replaceValue(data, "MINISTERIO DE DEFENSA", "public");
				HCUtils.replaceValue(data, "DIPUTACIÓN O CABILDO", "public"); //TODO ES - check that really?
				HCUtils.replaceValue(data, "COMUNIDAD AUTÓNOMA", "public");
			}
			if(cc.equals("BE")) {
				HCUtils.replaceValue(data, "Public", "public");
				HCUtils.replaceValue(data, "Openbaar - Public", "public");
				HCUtils.replaceValue(data, "Openbaar", "public");
				HCUtils.replaceValue(data, "Privé", "private");
				HCUtils.replaceValue(data, "Privaat", "private");
				HCUtils.replaceValue(data, "Privaat - Privé", "private");
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
			HCUtils.replaceValue(data, "Germany (until 1990 former territory of the FRG)", "Germany");


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
