package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class Geocoding {

	public static void main(String[] args) throws Exception {

		String filePath = "E:\\dissemination\\shared-data\\MS_data\\Service - Health\\AT/KA-Verzeichnis 2019-10-15.csv";
		ArrayList<Map<String,String>> hospitals = CSVUtil.load(filePath);
		System.out.println(hospitals.size());

		//int count = 0;
		for(Map<String,String> hospital : hospitals) {
			//count++;
			String address = hospital.get("Adresse");
			System.out.println(address);
			Coordinate c = Geocoding.geocode(address);
			System.out.println(c);
			//if(count > 10) break;
			hospital.put("lat", "" + c.y);
			hospital.put("lon", "" + c.x);
		}

		//remove commas
		for(Map<String,String> hospital : hospitals) {
			for(String key : hospital.keySet()) {
				if(key.equals("lon")) continue;
				if(key.equals("lat")) continue;
				hospital.put(key,"");
			}
				//hospital.put(key, hospital.get(key).replace(",", ""));
		}

		
		System.out.println("save");
		CSVUtil.save(hospitals, "E:\\dissemination\\shared-data\\MS_data\\Service - Health\\AT/AT_geolocated.csv");

		//
		//System.out.println(c);

	}





	/*
	 * street=<housenumber> <streetname>
city=<city>
county=<county>
state=<state>
country=<country>
postalcode=<postalcode>
	 */

	//Grazer Straße 13, 7540 Güssing
	//https://europa.eu/webtools/rest/gisco/nominatim/search.php?postalcode=7540&country=austria&polygon=0&viewbox=&format=json&limit=1

	//countrycode=DE
	//addressdetails ?
	//limit=1
	//output: importance?


	/**
	 * Make geocoding from query text.
	 * 
	 * @param query The query text (should not be already formatted as URL)
	 * @return The position, as proposed by the geocoder.
	 */
	public static Coordinate geocode(String query) {
		try {
			query = URLEncoder.encode(query, "UTF-8");
			String url = "https://europa.eu/webtools/rest/gisco/nominatim/search.php?q="+query+"&polygon=0&viewbox=&format=json&limit=1";
			BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
			String line = in.readLine();
			//System.out.println(line);
			String[] parts = line.split(",");
			Coordinate c = new Coordinate();
			for(String part : parts) {
				if(part.contains("\"lat\":")) {
					part = part.replace("\"lat\":\"", "");
					part = part.replace("\"", "");
					c.y = Double.parseDouble(part);
				}
				if(part.contains("\"lon\":")) {
					part = part.replace("\"lon\":\"", "");
					part = part.replace("\"", "");
					c.x = Double.parseDouble(part);
				}
			}
			in.close();
			return c;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
