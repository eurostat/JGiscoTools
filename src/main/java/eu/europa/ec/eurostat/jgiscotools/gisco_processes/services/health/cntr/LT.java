package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.cntr;

import java.util.ArrayList;
import java.util.Map;

import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.HCUtil;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class LT {

	public static void formatLT() {
		try {
			ArrayList<Map<String, String>> hs = CSVUtil.load(HCUtil.path + "LT/LT_.csv");
			System.out.println(hs.size());

			for(Map<String, String> h : hs) {


				h.remove("Code of legal entity");

				//street	house_number	postcode
				String add = h.get("address");
				//System.out.println(add);
				String[] parts = add.split(";");
				if(parts.length == 2) {
					String[] parts_ = parts[0].split(" ");
					String hn = parts_[parts_.length-1].trim();
					h.put("house_number", hn.toLowerCase());
					//System.out.println(h.get("house_number"));
					String street = parts[0].replace(hn, "").trim();
					h.put("street", street);
					//System.out.println(h.get("street"));

					String rest = parts[1].trim();
					int i = rest.indexOf("LT-");
					if(i>=0) {
						//get postcode
						String postcode = "LT-" + rest.substring(i+3, i+8);
						//System.out.println(postcode);
						h.put("postcode", postcode);
						rest = rest.replace(postcode, "").replace("  ", "").trim();
					}
					//System.out.println(rest + " - " + h.get("city"));
					h.put("city", rest);
				} else if(parts.length == 3) {
					//System.out.println(add);
					String[] parts_ = parts[0].split(" ");
					String hn = parts_[parts_.length-1].trim();
					hn = hn.toLowerCase().replace("km.", "");
					h.put("house_number", hn);
					//System.out.println(h.get("house_number"));

					String street = parts[0].replace(parts_[parts_.length-1], "").trim();
					h.put("street", street);
					//System.out.println(h.get("street"));

					String rest = parts[1].trim() + ";" + parts[2].trim();
					int i = rest.indexOf("LT-");
					if(i>=0) {
						//get postcode
						String postcode = "LT-" + rest.substring(i+3, i+8);
						//System.out.println(postcode);
						h.put("postcode", postcode);
						rest = rest.replace(postcode, "").replace("  ", "").trim();
					}
					//System.out.println(" --- " + street + " --- " +h.get("city"));
					//System.out.println(rest);
				} else {
					System.out.println(add);
				}

				//h.remove("address");

				//sub_priv_pub - public_private
				String spp = h.get("sub_priv_pub");
				h.put("public_private", "8".equals(spp)?"private":"public");
				h.remove("sub_priv_pub");

				h.put("cc", "LT");
				h.put("country", "");
				h.put("emergency", "");
				h.put("list_specs", "");
				h.put("cap_prac", "");
				h.put("cap_rooms", "");
				h.put("tel", "");
				h.put("email", "");
				h.put("url", "");
				h.put("pub_date", "");
				h.put("ref_date", "31/12/2018");
			}

			System.out.println("Save " + hs.size());
			CSVUtil.save(hs, HCUtil.path + "LT/LT_formatted.csv");
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

}
