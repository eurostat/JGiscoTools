package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.cntr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import eu.europa.ec.eurostat.jgiscotools.geocoding.BingGeocoder;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.LocalParameters;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.ServicesGeocoding;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.HCUtil;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoData;
import eu.europa.ec.eurostat.jgiscotools.util.ProjectionUtil;

public class HU {

	public static void main(String[] args) {
		System.out.println("Start");

		try {
			//load data
			Collection<Map<String, String>> data = CSVUtil.load(HCUtil.path + "HU/HU_raw.csv");
			System.out.println(data.size());

			//Megye,NEAK kód,Intézmény megnevezése,TIP,Ellátási Forma,Szakmakód,Szakma megnevezése,Pregresszivitási szint,Fin. kód,Osztály neve,Aktív ágyszám,Krónikus ágyszám,Krónikus szorzó,Kapcsolódó szervezeti egység,Nappali beteglétszám,Telephely irszám,Telephely,Telephely utca

			CSVUtil.removeColumn(data, "Megye");
			CSVUtil.renameColumn(data, "NEAK kód", "id");
			CSVUtil.renameColumn(data, "Intézmény megnevezése", "hospital_name");
			CSVUtil.removeColumn(data, "TIP");
			CSVUtil.removeColumn(data, "Ellátási Forma");
			CSVUtil.removeColumn(data, "Szakmakód");
			CSVUtil.removeColumn(data, "Szakma megnevezése");
			CSVUtil.removeColumn(data, "Pregresszivitási szint");
			CSVUtil.removeColumn(data, "Fin. kód");
			CSVUtil.removeColumn(data, "Osztály neve");
			CSVUtil.removeColumn(data, "Krónikus szorzó");
			CSVUtil.removeColumn(data, "Kapcsolódó szervezeti egység");
			CSVUtil.removeColumn(data, "Nappali beteglétszám");

			CSVUtil.renameColumn(data, "Telephely irszám", "postcode");
			CSVUtil.renameColumn(data, "Telephely", "city");

			for(Map<String, String> h : data) {
				int nb_beds = 0;
				try { nb_beds += Integer.parseInt(h.get("Aktív ágyszám")); } catch (Exception e) {}
				try { nb_beds += Integer.parseInt(h.get("Krónikus ágyszám")); } catch (Exception e) {}
				h.put("cap_beds", ""+nb_beds);

				String sthnb = h.get("Telephely utca");
				//decompose number - street name
				String[] parts = sthnb.split(" ");
				if(parts.length <= 1) {
				} else if(parts.length >= 2) {
					String hnb = parts[parts.length-1];
					String street = sthnb.replace(hnb, "").trim();
					h.put("house_number", hnb.substring(0, hnb.length()-1));
					h.put("street", street);
				}
			}
			CSVUtil.removeColumn(data, "Aktív ágyszám");
			CSVUtil.removeColumn(data, "Krónikus ágyszám");
			CSVUtil.removeColumn(data, "Telephely utca");

			CSVUtil.addColumn(data, "cc", "HU");
			CSVUtil.addColumn(data, "ref_date", "01/01/2019");

			//aggregate by id
			data = CSVUtil.aggregateById(data, "id", "cap_beds", "cap_prac", "cap_rooms" );

			//remove those without city
			Collection<Map<String, String>> rem = new ArrayList<>();
			for(Map<String, String> h : data)
				if("".equals(h.get("city"))) rem.add(h);
			data.removeAll(rem);


			// save 1
			System.out.println(data.size());
			CSVUtil.addColumns(data, HCUtil.cols, "");
			CSVUtil.save(data, HCUtil.path + "HU/HU_formatted.csv");

			//geocode
			LocalParameters.loadProxySettings();
			ServicesGeocoding.set(BingGeocoder.get(), data, "lon", "lat", true, true);

			// save 2
			CSVUtil.save(data, HCUtil.path + "HU/HU.csv");
			GeoData.save(CSVUtil.CSVToFeatures(data, "lon", "lat"), HCUtil.path + "HU/HU.gpkg", ProjectionUtil.getWGS_84_CRS());
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("End");
	}

}
