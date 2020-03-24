package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.csv.CSVFormat;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.locationtech.jts.geom.Coordinate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import eu.europa.ec.eurostat.jgiscotools.geocoding.BingGeocoder;
import eu.europa.ec.eurostat.jgiscotools.geocoding.GISCOGeocoder;
import eu.europa.ec.eurostat.jgiscotools.geocoding.GeocodingAddress;
import eu.europa.ec.eurostat.jgiscotools.geocoding.GeocodingResult;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.XMLUtils;
import eu.europa.ec.eurostat.jgiscotools.util.Util;

public class HealthCareDataFormattingGeocoding {

	static String path = "E:\\dissemination\\shared-data\\MS_data\\Service - Health\\";

	public static void main(String[] args) throws Exception {
		System.out.println("Start");

		//

		// formatAT();
		// formatCH();
		// formatLU();
		// LocalParameters.loadProxySettings();
		// formatRO();
		//formatFI();
		//formatIT();
		//formatDE();
		//formatBE();
		//formatFR();
		// ...
		formatDK();


		/*
		 * /AT System.out.println("load"); ArrayList<Map<String,String>> hospitals =
		 * CSVUtil.load(path+"AT/AT_formatted.csv",
		 * CSVFormat.DEFAULT.withFirstRecordAsHeader()); geocodeGISCO(hospitals, false);
		 * LocalParameters.loadProxySettings(); //TODO fix that - GISCO geocoder does
		 * not work with proxy geocodeBing(hospitals, false);
		 * System.out.println("save");
		 * CSVUtil.save(hospitals,path+"AT/AT_geolocated.csv");
		 */

		/*/geocoding
		System.out.println("load");
		ArrayList<Map<String,String>> hospitals = CSVUtil.load("/home/juju/Bureau/FR/FR_formated.csv");
		geocodeBing(hospitals, true);
		System.out.println("save");
		CSVUtil.save(hospitals, "/home/juju/Bureau/FR/FR_geolocated.csv");*/

		//save as geopckg
		//GeoPackageUtil.save(CSVUtil.CSVToFeatures(hospitals, "lonBing", "latBing"), "E:\\\\dissemination\\\\shared-data\\\\MS_data\\\\Service - Health\\\\IT/IT_geolocated.gpkg", ProjectionUtil.getWGS_84_CRS());

		System.out.println("End");
	}

	private static void geocodeGISCO(ArrayList<Map<String,String>> hospitals, boolean usePostcode) {
		//int count = 0;
		int fails = 0;
		for(Map<String,String> hospital : hospitals) {
			//count++;
			String address = "";
			if(hospital.get("house_number")!=null) address += hospital.get("house_number") + " ";
			address += hospital.get("street");
			address += " ";
			if(usePostcode) {
				address += hospital.get("postcode");
				address += " ";
			}
			address += hospital.get("city");
			address += " ";
			address += hospital.get("country");
			System.out.println(address);

			GeocodingResult gr = GISCOGeocoder.geocode(address);
			Coordinate c = gr.position;
			System.out.println(c  + "  --- " + gr.matching + " --- " + gr.confidence);
			if(c.getX()==0 && c.getY()==0) fails++;

			//if(count > 10) break;
			hospital.put("latGISCO", "" + c.y);
			hospital.put("lonGISCO", "" + c.x);
			hospital.put("geo_matchingGISCO", "" + gr.matching);
			hospital.put("geo_confidenceGISCO", "" + gr.confidence);
		}

		System.out.println("Failures: " + fails + "/" + hospitals.size());
	}


	private static void geocodeBing(ArrayList<Map<String,String>> hospitals, boolean usePostcode) {
		//int count = 0;
		int fails = 0;
		for(Map<String,String> hospital : hospitals) {
			//count++;
			GeocodingAddress address = new GeocodingAddress(
					null,
					hospital.get("house_number"),
					hospital.get("street"),
					hospital.get("city"),
					hospital.get("cc"),
					usePostcode? hospital.get("postcode") : null
					);

			GeocodingResult gr = BingGeocoder.geocode(address);
			Coordinate c = gr.position;
			System.out.println(c  + "  --- " + gr.matching + " --- " + gr.confidence);
			if(c.getX()==0 && c.getY()==0) fails++;

			//if(count > 10) break;
			hospital.put("lat", "" + c.y);
			hospital.put("lon", "" + c.x);
			hospital.put("geo_matching", "" + gr.matching);
			hospital.put("geo_confidence", "" + gr.confidence);
		}

		System.out.println("Failures: " + fails + "/" + hospitals.size());
	}

	public static void formatDK() {
		try {
			String basePath = "/home/juju/Bureau/DK/";
			ArrayList<Map<String, String>> raw = CSVUtil.load(basePath + "DK_geolocated.csv");
			System.out.println(raw.size());

			ArrayList<Map<String, String>> out = new ArrayList<>();
			for(Map<String, String> r : raw) {
				Map<String, String> hf = new HashMap<>();

				hf.put("hospital_name", r.get("hospital_name"));
				hf.put("lat", r.get("lat"));
				hf.put("lon", r.get("lon"));
				hf.put("cc", "DK");
				hf.put("country", "Denmark");

				//street,house_number,postcode,city
				String add = r.get("Adresse");
				String[] parts = add.split(",");
				if(parts.length == 1) {
					hf.put("city", add);
				} else {
					String postcodeCity = parts[parts.length - 1].trim();
					String postcode = postcodeCity.substring(0, 4);
					String city = postcodeCity.substring(5, postcodeCity.length());
					hf.put("postcode", postcode);
					hf.put("city", city);

					String st = parts[0].trim();
					hf.put("street", st);
				}

				out.add(hf);
			}

			System.out.println("Save " + out.size());
			CSVUtil.save(out, basePath + "DK.csv");		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void formatFR() {
		try {
			String basePath = "/home/juju/Bureau/FR/";
			//load csv
			//ArrayList<Map<String, String>> raw = CSVUtil.load(basePath + "finess_data.csv");
			//System.out.println(raw.size());
			//HashMap<String, Map<String, String>> rawI = Util.index(raw, "nofinesset");
			ArrayList<Map<String, String>> raw = CSVUtil.load(basePath + "finess_clean.csv");
			System.out.println(raw.size());
			//CSVUtil.getUniqueValues(raw, "typvoie", true);

			/*/prepare
			GeometryFactory gf = new GeometryFactory();
			CoordinateReferenceSystem LAMBERT_93 = CRS.decode("EPSG:2154");
			CoordinateReferenceSystem UTM_N20 = CRS.decode("EPSG:32620");
			CoordinateReferenceSystem UTM_N21 = CRS.decode("EPSG:32621");
			CoordinateReferenceSystem UTM_N22 = CRS.decode("EPSG:32622");
			CoordinateReferenceSystem UTM_S40 = CRS.decode("EPSG:32740");
			CoordinateReferenceSystem UTM_S38 = CRS.decode("EPSG:32738");*/

			ArrayList<Map<String, String>> out = new ArrayList<>();
			for(Map<String, String> r : raw) {
				Map<String, String> hf = new HashMap<>();

				String id = r.get("nofinesset");

				/*Map<String, String> rd = rawI.get(id);
				if(rd == null) {
					System.out.println("FR - no information for site " + id);
					continue;
				}*/

				/*categagretab
				1101 - Centres Hospitaliers Régionaux
				1102 - Centres Hospitaliers
				//1205 - Autres Etablissements Relevant de la Loi Hospitalière
				//2204 - Etablissements ne relevant pas de la Loi Hospitalière
				1106 - Hôpitaux Locaux*/
				int cat = (int) Double.parseDouble(r.get("categagretab"));
				if(cat!=1101 && cat!=1102 && cat!=1106) continue;

				hf.put("cc", "FR");
				hf.put("id", id);

				/*/get CRS
				CoordinateReferenceSystem crs = null;
				switch (r.get("crs")) {
				case "LAMBERT_93": crs = LAMBERT_93; break;
				case "UTM_N20": crs = UTM_N20; break;
				case "UTM_N21": crs = UTM_N21; break;
				case "UTM_N22": crs = UTM_N22; break;
				case "UTM_S40": crs = UTM_S40; break;
				case "UTM_S38": crs = UTM_S38; break;
				default: System.out.println(r.get("crs")); break;
				}*/

				/*/change crs to lon/lat
				double x = Double.parseDouble( r.get("coordxet") );
				double y = Double.parseDouble( r.get("coordyet") );
				Point pt = (Point) ProjectionUtil.project(gf.createPoint(new Coordinate(x,y)), crs, ProjectionUtil.getWGS_84_CRS());
				hf.put("lon", ""+pt.getY());
				hf.put("lat", ""+pt.getX());*/

				hf.put("hospital_name", !r.get("rslongue").equals("")? r.get("rslongue") : r.get("rs"));
				hf.put("house_number", r.get("numvoie") + r.get("compvoie"));

				String tv = r.get("typvoie");
				switch (tv) {
				case "": break;
				case "R": tv="RUE"; break;
				case "AV": tv="AVENUE"; break;
				case "BD": tv="BOULEVARD"; break;
				case "PL": tv="PLACE"; break;
				case "RTE": tv="ROUTE"; break;
				case "IMP": tv="IMPASSE"; break;
				case "CHE": tv="CHEMIN"; break;
				case "QUA": tv="QUAI"; break;
				case "PROM": tv="PROMENADE"; break;
				case "PASS": tv="PASSAGE"; break;
				case "ALL": tv="ALLEE"; break;
				//default: System.out.println(tv);
				}

				String street = "";
				street += ("".equals(tv) || tv==null)? "" : tv + " ";
				street += r.get("voie") + " ";
				street += (r.get("lieuditbp").contains("BP")? "" : r.get("lieuditbp"));
				street = street.trim();
				hf.put("street", street);

				String lia = r.get("ligneacheminement");
				hf.put("postcode", lia.substring(0, 5));

				String city = lia.substring(6, lia.length());
				for(int cedex = 30; cedex>0; cedex--) city = city.replace("CEDEX " + cedex, "");
				city = city.replace("CEDEX", "");
				city = city.trim();
				hf.put("city", city);
				if(city==null || city.equals("")) System.err.println("No city for " + id);

				hf.put("tel", r.get("telephone"));
				hf.put("public_private", r.get("libsph"));
				hf.put("facility_type", r.get("libcategagretab"));

				hf.put("ref_date", "2020-03-04");

				//if("".equals(hf.get("street"))) System.out.println(hf.get("city") + "  ---  " + hf.get("hospital_name"));
				out.add(hf);
			}

			System.out.println("Save "+out.size());
			CSVUtil.save(out, basePath + "FR_formated.csv");		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public static void formatBE() {
		String basePath = "/home/juju/Bureau/BE/";
		//load csv
		ArrayList<Map<String, String>> raw = CSVUtil.load(basePath + "BE_raw.csv");
		System.out.println(raw.size());
		HashMap<String, Map<String, String>> rawI = Util.index(raw, "NUMERO DE SITE");
		ArrayList<Map<String, String>> rawLonLat = CSVUtil.load(basePath + "BE_raw_lonlat.csv");
		System.out.println(rawLonLat.size());

		ArrayList<Map<String, String>> out = new ArrayList<>();
		for(Map<String, String> r : rawLonLat) {
			Map<String, String> hf = new HashMap<>();

			String id = r.get("NUMERO DE SITE");
			hf.put("cc", "BE");
			hf.put("id", id);
			hf.put("hospital_name", r.get("HOPITAL"));
			hf.put("site_name", r.get("SITE"));
			hf.put("lon", r.get("Longitude").replace(",", "."));
			hf.put("lat", r.get("Latitude").replace(",", "."));

			Map<String, String> rd = rawI.get(id);
			if(rd == null) System.out.println("BE - no information for site " + id);
			hf.put("street", rd.get("ADRESSE")); //TODO decompose with house number?
			hf.put("postcode", rd.get("POST"));
			hf.put("city", rd.get("COMMUNE"));
			hf.put("tel", rd.get("TELEFON"));
			hf.put("url", rd.get("WEBSITE"));
			hf.put("facility_type", rd.get("TYPE HOPITAL"));
			hf.put("public_private", rd.get("STATUT"));
			hf.put("cap_beds", rd.get("TOTAL LITS"));
			if("X".equals(rd.get("PREMIERE PRISE EN CHARGE DES URGENCES")) || "X".equals(rd.get("SOINS URGENTS SPECIALISES")))
				hf.put("emergency", "1"); else hf.put("emergency", "0");
			hf.put("ref_date", "2020-02-01");

			out.add(hf);
		}

		System.out.println("Save "+out.size());
		CSVUtil.save(out, basePath + "BE.csv");
	}


	/**
	 * Format RO
	 */
	public static void formatRO() {

		try {
			/*
			 * String url = "http://infrastructura-sanatate.ms.ro/harta/extractSpitaleGPS";
			 * System.out.println(url); BufferedReader in = new BufferedReader(new
			 * InputStreamReader(new URL(url).openStream())); String line = in.readLine();
			 * System.out.println(line); //String[] parts = line.split(",");
			 */

			Scanner scanner = new Scanner(new File(path + "RO/extractSpitaleGPS.html"));
			String line = scanner.nextLine();
			scanner.close();

			JSONArray data = (JSONArray) new JSONParser().parse(line);
			Collection<Map<String, String>> hospitalsFormatted = new ArrayList<Map<String, String>>();
			for (Object h_ : data) {
				JSONObject h = (JSONObject) h_;
				// [numar_total_paturi, adresa_web, denumire, lng, numar_sectii, adresa,
				// spital_id, lat, numar_compartimente]
				// , beds_ address, name, next, number_sections, address, hospital_id, wide,
				// number_ compartments

				// new formatted hospital
				HashMap<String, String> hf = new HashMap<String, String>();
				hf.put("country", "RO");
				hf.put("id", h.get("spital_id").toString());
				hf.put("name", h.get("denumire").toString());
				hf.put("url", h.get("adresa_web").toString());
				hf.put("cap_beds", h.get("numar_total_paturi") == null ? "" : h.get("numar_total_paturi").toString());
				hf.put("data_year", "2020?"); // TODO check
				hf.put("lat", h.get("lat").toString());
				hf.put("lon", h.get("lng").toString());
				// adresa
				// numar_sectii - numar_compartimente -> capacity?

				hospitalsFormatted.add(hf);
			}
			// save
			CSVUtil.save(hospitalsFormatted, path + "RO/RO_geolocated.csv");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Format AT
	 */
	public static void formatAT() {

		String filePath = path + "AT/KA-Verzeichnis 2019-10-15.csv";
		ArrayList<Map<String, String>> hospitals = CSVUtil.load(filePath,
				CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(';'));
		System.out.println(hospitals.size());

		Collection<Map<String, String>> hospitalsFormatted = new ArrayList<Map<String, String>>();
		for (Map<String, String> h : hospitals) {

			// new formatted hospital
			HashMap<String, String> hf = new HashMap<String, String>();

			// copy columns

			// country - AT
			hf.put("country", "AT");
			// id - KA-Nr
			hf.put("id", h.get("KA-Nr"));
			// name - Bezeichnung
			hf.put("name", h.get("Bezeichnung"));
			// list_specs - Intensivbereiche
			hf.put("list_specs", h.get("Intensivbereiche"));
			// url - Homepage
			hf.put("url", h.get("Homepage"));
			// tel - Telefon
			hf.put("tel", h.get("Telefon"));
			// cap_beds - Bettenanzahl
			hf.put("cap_beds", h.get("Bettenanzahl"));
			// year
			hf.put("data_year", "2020");

			// address
			// street house_number postcode city - Adresse
			// St. Veiter-Straße 46, 5621 St. Veit im Pongau
			String ad = h.get("Adresse");
			String[] parts = ad.split(",");
			if (parts.length != 2)
				System.err.println(ad);
			String rightPart = parts[1];
			String fc = rightPart.substring(0, 1);
			if (!fc.equals(" "))
				System.err.println(fc);
			rightPart = rightPart.substring(1, rightPart.length());
			String postcode = rightPart.substring(0, 4);
			hf.put("postcode", postcode);

			fc = rightPart.substring(4, 5);
			if (!fc.equals(" "))
				System.err.println(fc);
			String city = rightPart.substring(5, rightPart.length());
			hf.put("city", city);

			String leftPart = parts[0];
			if (leftPart.equals("")) {
				hf.put("house_number", "");
				hf.put("street", "");
			} else {
				parts = leftPart.split(" ");
				String house_number = parts[parts.length - 1];

				// assign the house_number, or "" if it equals "0"
				hf.put("house_number", house_number.equals("0") ? "" : house_number);

				leftPart = leftPart.replace(house_number, "");
				fc = leftPart.substring(leftPart.length() - 1, leftPart.length());
				if (!fc.equals(" "))
					System.err.println(fc);
				String street = leftPart.substring(0, leftPart.length() - 1);
				hf.put("street", street);
			}

			// add to list
			hospitalsFormatted.add(hf);
		}

		// save
		CSVUtil.save(hospitalsFormatted,
				"E:\\dissemination\\shared-data\\MS_data\\Service - Health\\AT/AT_formatted.csv");
	}

	/**
	 * Fotmat CH
	 */

	public static void formatCH() {

		String filePath = path + "CH/kzp17_daten.csv";
		ArrayList<Map<String, String>> hospitals = CSVUtil.load(filePath,
				CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(';'));
		System.out.println(hospitals.size());

		Collection<Map<String, String>> hospitalsFormatted = new ArrayList<Map<String, String>>();
		for (Map<String, String> h : hospitals) {

			// new formatted hospital
			HashMap<String, String> hf = new HashMap<String, String>();

			// copy columns

			// country - CH
			hf.put("country", "CH");
			// Inst = name
			hf.put("name", h.get("Inst"));
			// list_specs - Intensivbereiche
			hf.put("list_specs", h.get("Akt"));
			// BettenStat = cap_beds (Betten)
			hf.put("cap_beds", h.get("BettenStat"));
			// PersA = cap_prac (Ärzte)
			hf.put("cap_prac", h.get("PersA"));
			// Typ - facility_type (Spitaltyp, gemäss BFS Spitaltypologie)
			hf.put("facility_type", h.get("Typ"));
			// Jahr = year
			hf.put("data_year", h.get("Jahr"));

			// address AT
			// street house_number postcode city - Adresse
			// St. Veiter-Straße 46, 5621 St. Veit im Pongau

			// address CH
			// Ort = postcode + city
			// 7000 Chur
			// Adr = streetname (street name and number)
			// Loestrasse 220

			// Split "Ort" column into postcode and city
			String ort = h.get("Ort");
			// split in two where there's a space
			String[] parts = ort.split(" ");
			// If there are more than 2 parts, print error
			if (parts.length != 2)
				System.err.println(ort);
			// on the right:
			String rightPart = parts[1];
			String fc = rightPart.substring(0, 1);
			if (!fc.equals(" "))
				System.err.println(fc);
			rightPart = rightPart.substring(1, rightPart.length());
			String postcode = rightPart.substring(0, 4);
			hf.put("postcode", postcode);

			fc = rightPart.substring(4, 5);
			if (!fc.equals(" "))
				System.err.println(fc);
			String city = rightPart.substring(5, rightPart.length());
			hf.put("city", city);

			String leftPart = h.get("Adr");
			// String leftPart = parts[0];
			if (leftPart.equals("")) {
				hf.put("house_number", "");
				hf.put("street", "");
			} else {
				parts = leftPart.split(" ");
				String house_number = parts[parts.length - 1];

				// assign the house_number, or "" if it equals "0"
				hf.put("house_number", house_number.equals("0") ? "" : house_number);

				leftPart = leftPart.replace(house_number, "");
				fc = leftPart.substring(leftPart.length() - 1, leftPart.length());
				if (!fc.equals(" "))
					System.err.println(fc);
				String street = leftPart.substring(0, leftPart.length() - 1);
				hf.put("street", street);
			}

			// add to list
			hospitalsFormatted.add(hf);
		}

		// save
		CSVUtil.save(hospitalsFormatted,
				"E:\\dissemination\\shared-data\\MS_data\\Service - Health\\CH/CH_formatted.csv");
	}

	public static void formatFI() {

		String filePath = "/home/juju/Bureau/FI_geolocated.csv";
		ArrayList<Map<String, String>> hospitals_FI = CSVUtil.load(filePath,
				CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(';'));
		System.out.println(hospitals_FI.size());

		Collection<Map<String, String>> hospitalsFormatted_FI = new ArrayList<Map<String, String>>();
		for (Map<String, String> h : hospitals_FI) {

			// new formatted hospital
			HashMap<String, String> h_FI = new HashMap<String, String>();

			// copy columns

			// country code

			h_FI.put("hospital_name", h.get("organization name"));
			h_FI.put("site_name", h.get("name"));
			h_FI.put("cc", "FI");
			h_FI.put("country", "Finland");
			h_FI.put("postcode", h.get("postcode"));
			h_FI.put("municipality", h.get("municipality"));
			h_FI.put("facility_type", h.get("services"));
			h_FI.put("list_specs", h.get("services"));

			// address
			// PL 13

			// add to list
			hospitalsFormatted_FI.add(h_FI);

		}

		// save
		CSVUtil.save(hospitalsFormatted_FI, "/home/juju/Bureau/FI.csv");
	}

	public static void formatIT() {

		String filePath = path + "IT/C_17_dataset_96_0_upFile.csv";
		ArrayList<Map<String, String>> hospitals = CSVUtil.load(filePath,
				CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(';'));
		System.out.println(hospitals.size());

		Collection<Map<String, String>> hospitalsFormatted = new ArrayList<Map<String, String>>();
		for (Map<String, String> h : hospitals) {

			if( !"2018".equals(h.get("Anno")) ) continue;

			// new formatted hospital
			HashMap<String, String> hf = new HashMap<String, String>();
			hf.put("cc", "IT");
			hf.put("country", "Italy");
			hf.put("id", h.get("Codice Azienda").trim() + "-" + h.get("Codice struttura").trim() + "-" + h.get("Subcodice").trim());
			hf.put("hospital_name", h.get("Denominazione Struttura/Stabilimento").trim());
			hf.put("street", h.get("Indirizzo").trim());
			hf.put("city", h.get("Comune").trim());
			hf.put("cap_beds", h.get("Totale posti letto").trim());
			hf.put("facility_type", h.get("Descrizione tipo struttura").trim());
			hf.put("year", h.get("Anno").trim());

			// add to list
			hospitalsFormatted.add(hf);
		}
		System.out.println(hospitalsFormatted.size());

		//compact the hospitals
		Map<String, Map<String, String>> hospitalsCompacted = new HashMap<>();
		for (Map<String, String> h : hospitalsFormatted) {
			//get hospital compacted
			Map<String, String> hC = hospitalsCompacted.get(h.get("id"));
			if(hC==null) {
				hospitalsCompacted.put(h.get("id"), h);
			} else {
				hC.put("cap_beds", ""+(Integer.parseInt(hC.get("cap_beds")) + Integer.parseInt(h.get("cap_beds"))));
			}
		}
		hospitalsFormatted = hospitalsCompacted.values();
		System.out.println(hospitalsFormatted.size());


		// save
		CSVUtil.save(hospitalsFormatted, path + "IT/IT_formatted.csv");
	}

	public static void formatDE() {
		try {
			String filePath = path + "DE/20200308_Verzeichnisabruf_aktuell.xml";
			Document doc = XMLUtils.parse(new FileInputStream(filePath));

			Element root = doc.getDocumentElement();
			//System.out.println( root.getNodeName() );

			NodeList elts = root.getChildNodes();
			//System.out.println(elts.getLength());
			Collection<Map<String, String>> hospitalsFormatted = new ArrayList<Map<String, String>>();
			for(int i=0; i<elts.getLength(); i++) {
				if(!elts.item(i).getNodeName().equals("Standort")) continue;
				Element elt = (Element) elts.item(i);
				String name = elt.getElementsByTagName("Bezeichnung").item(0).getTextContent();
				String id = elt.getElementsByTagName("StandortId").item(0).getTextContent();
				Element geoAdr = (Element) elt.getElementsByTagName("GeoAdresse").item(0);
				String lon = geoAdr.getElementsByTagName("Längengrad").item(0).getTextContent();
				String lat = geoAdr.getElementsByTagName("Breitengrad").item(0).getTextContent();
				String housenb = geoAdr.getElementsByTagName("Hausnummer").item(0).getTextContent();
				String street = geoAdr.getElementsByTagName("Straße").item(0).getTextContent();
				String postcode = geoAdr.getElementsByTagName("PLZ").item(0).getTextContent();
				String city = geoAdr.getElementsByTagName("Ort").item(0).getTextContent();


				HashMap<String, String> hf = new HashMap<String, String>();
				hf.put("cc", "DE");
				hf.put("country", "Germany");
				hf.put("id", id);
				hf.put("hospital_name", name);
				hf.put("house_number", housenb);
				hf.put("street", street);
				hf.put("postcode", postcode);
				hf.put("city", city);
				hf.put("year", "2020");				
				hf.put("lon", lon);
				hf.put("lat", lat);
				hf.put("type", "standort");
				hospitalsFormatted.add(hf);

				//TODO get all einrichtungen
				NodeList einrichtungen = elt.getElementsByTagName("GeoAdresse");
				for(int j=0; j<einrichtungen.getLength(); j++) {
					Element einrichtung = (Element) einrichtungen.item(j);
					//TODO
				}
			}

			// save
			CSVUtil.save(hospitalsFormatted, path + "DE/DE_formatted.csv");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
