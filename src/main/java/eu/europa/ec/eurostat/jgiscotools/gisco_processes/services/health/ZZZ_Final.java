/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

			removeColumn(data, "latGISCO");
			removeColumn(data, "lonGISCO");
			removeColumn(data, "x");
			removeColumn(data, "y");
			removeColumn(data, "Column name (code)");
			removeColumn(data, "county");
			removeColumn(data, "suburb");
			removeColumn(data, "state");
			removeColumn(data, "Sort_Ratio");
			removeColumn(data, "Set_Ratio");
			removeColumn(data, "Ratio");
			removeColumn(data, "Coordinates");
			removeColumn(data, "Output Geocoder");
			removeColumn(data, "Input Geocoder");
			removeColumn(data, "Part_Ratio");
			removeColumn(data, "address");
			removeColumn(data, "extension");
			removeColumn(data, "Address");
			removeColumn(data, "organization id");
			removeColumn(data, "postal district");
			removeColumn(data, "service industry id");
			removeColumn(data, "district");
			removeColumn(data, "organization name");
			removeColumn(data, "geo_matching");
			removeColumn(data, "geo_confidence");

			changeColumnName(data, "latBing", "lat");
			changeColumnName(data, "lonBing", "lon");
			changeColumnName(data, "name", "hospital_name");
			changeColumnName(data, "type", "facility_type");
			changeColumnName(data, "year", "ref_date");
			changeColumnName(data, "data_year", "pub_date");

			//System.out.println(data.iterator().next().keySet());

			Set<String> ch = checkNoUnexpectedColumn(data, cols_);
			if(ch.size()>0) System.err.println(ch);

			//populateAllColumns(data, cols, "");

			replaceValue(data, "", null);
			replaceValue(data, "NA", null);
			replaceValue(data, "UNKNOWN", null);


			//hospital_name,Region,Country,Adresse,lat,lon


			if(cc.equals("ES")) {				
				replaceValue(data, "SEGURIDAD SOCIAL", "public");
				replaceValue(data, "SEGURIDAD SOCIAL", "public");
				replaceValue(data, "PRIVADO NO BENÉFICO", "private");
				replaceValue(data, "OTRO PRIVADO BENÉFICO", "private");
				replaceValue(data, "PRIVADO-BENÉFICO (CRUZ ROJA)", "private");
				replaceValue(data, "PRIVADO-BENÉFICO (IGLESIA)", "private");
				replaceValue(data, "OTRA DEPENDENCIA PATRIMONIAL", "private");
				replaceValue(data, "MATEP", "public"); //TODO ES - check that really?
				replaceValue(data, "ENTIDADES PÚBLICAS", "public");
				replaceValue(data, "MUNICIPIO", "public");
				replaceValue(data, "MINISTERIO DE INTERIOR", "public");
				replaceValue(data, "MINISTERIO DE DEFENSA", "public");
				replaceValue(data, "DIPUTACIÓN O CABILDO", "public"); //TODO ES - check that really?
				replaceValue(data, "COMUNIDAD AUTÓNOMA", "public");
			}
			if(cc.equals("BE")) {
				replaceValue(data, "Public", "public");
				replaceValue(data, "Openbaar - Public", "public");
				replaceValue(data, "Openbaar", "public");
				replaceValue(data, "Privé", "private");
				replaceValue(data, "Privaat", "private");
				replaceValue(data, "Privaat - Privé", "private");
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
			replaceValue(data, "Germany (until 1990 former territory of the FRG)", "Germany");


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

	private static Set<String> checkNoUnexpectedColumn(ArrayList<Map<String, String>> data, Collection<String> cols) {
		for(Map<String, String> h : data) {
			Set<String> cs = new HashSet<>(h.keySet());
			cs.removeAll(cols);
			if(cs.size() != 0)
				return cs;
		}
		return new HashSet<String>();
	}

	private static void replaceValue(ArrayList<Map<String, String>> data, String ini, String fin) {
		for(Map<String, String> h : data)
			for(Entry<String,String> e : h.entrySet())
				if(e.getValue() != null && ini.equals(e.getValue()))
					e.setValue(fin);
	}

	private static void changeColumnName(ArrayList<Map<String, String>> data, String old, String new_) {
		for(Map<String, String> h : data) {
			if(h.get(old) != null) {
				h.put(new_, h.get(old));
				h.remove(old);
			}
		}
	}

	private static void removeColumn(ArrayList<Map<String, String>> data, String col) {
		for(Map<String, String> h : data) {
			if(h.get(col) != null)
				h.remove(col);
		}
	}

}
