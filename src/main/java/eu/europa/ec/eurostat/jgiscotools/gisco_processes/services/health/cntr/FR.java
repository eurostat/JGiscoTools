package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.cntr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.HCUtil;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.ValidateCSV;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class FR {

	public static void main(String[] args) {
		System.out.println("Start");

		//load data
		ArrayList<Map<String, String>> data = CSVUtil.load(HCUtil.path + "FR/finess_clean.csv");
		System.out.println(data.size());

		ArrayList<Map<String, String>> out = new ArrayList<>();
		for(Map<String, String> r : data) {
			Map<String, String> hf = new HashMap<>();

			//Keep only "Etablissements Hospitaliers", that is: "categagretab = 11**"
			String cat = r.get("categagretab").substring(0,2);
			if(!"11".equals(cat)) continue;


			String id = r.get("nofinesset");
			hf.put("id", id);
			String hname = !r.get("rslongue").equals("")? r.get("rslongue") : r.get("rs");
			hf.put("hospital_name", hname);
			hf.put("site_name", r.get("complrs"));


			hf.put("facility_type", r.get("libcategetab"));

			//public_private
			switch (r.get("codesph")) {
			case "": hf.put("public_private", ""); break;
			case "0": hf.put("public_private", ""); break;
			case "1": hf.put("public_private", "public"); break;
			case "2": hf.put("public_private", "public"); break; //PSPH
			case "3": hf.put("public_private", "public"); break; //PSPH
			case "4": hf.put("public_private", "public"); break; //PSPH
			case "5": hf.put("public_private", "private"); break;
			case "6": hf.put("public_private", "private"); break;
			case "7": hf.put("public_private", "private"); break;
			case "9": hf.put("public_private", ""); break;
			default:
				System.out.println("Unhandled codesph = " + r.get("codesph") + " --- " + r.get("libsph") );
				System.out.println(hname);
				break;
			}

			//TODO find info somewhere
			hf.put("emergency", "");


			//house number
			hf.put("house_number", r.get("numvoie") + r.get("compvoie"));

			//street
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

			//postcode - TODO convert cedex to noncedex?
			String lia = r.get("ligneacheminement");
			hf.put("postcode", lia.substring(0, 5));

			//city
			String city = lia.substring(6, lia.length());
			for(int cedex = 30; cedex>0; cedex--) city = city.replace("CEDEX " + cedex, "");
			city = city.replace("CEDEX", "");
			city = city.trim();
			hf.put("city", city);
			if(city==null || city.equals("")) System.err.println("No city for " + id);

			//country code. Take into account oversea territories.
			String cc = "";
			switch (hf.get("postcode").substring(0,3)) {
			case "971": cc="GP"; break;
			case "972": cc="MQ"; break;
			case "973": cc="GF"; break;
			case "974": cc="RE"; break;
			case "975": cc="PM"; break;
			case "976": cc="YT"; break;
			default: cc="FR"; break;
			}
			hf.put("cc", cc);

			hf.put("tel", r.get("telephone"));

			//if("".equals(hf.get("street"))) System.out.println(hf.get("city") + "  ---  " + hf.get("hospital_name"));
			out.add(hf);
		}

		CSVUtil.addColumn(out, "ref_date", "06/03/2020");
		CSVUtil.addColumn(out, "pub_date", "");
		CSVUtil.addColumn(out, "geo_qual", "-1");
		CSVUtil.addColumn(out, "lon", "0");
		CSVUtil.addColumn(out, "lat", "0");
		ValidateCSV.validate(out, "FR");

		System.out.println("Save " + out.size());
		CSVUtil.save(out, HCUtil.path + "FR/FR_formated.csv");

		System.out.println("End");
	}




	public static void formatFR() {
		try {
			//load csv
			//ArrayList<Map<String, String>> raw = CSVUtil.load(basePath + "finess_data.csv");
			//System.out.println(raw.size());
			//HashMap<String, Map<String, String>> rawI = Util.index(raw, "nofinesset");
			ArrayList<Map<String, String>> raw = CSVUtil.load(HCUtil.path + "FR/finess_clean.csv");
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

				//TODO cedex postcode to noncedex

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

			//System.out.println("Save "+out.size());
			//CSVUtil.save(out, path + "FR/FR_formated.csv");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
