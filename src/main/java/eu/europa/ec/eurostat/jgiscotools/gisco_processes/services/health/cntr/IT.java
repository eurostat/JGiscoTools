package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.cntr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;

import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.HCUtil;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class IT {

	public static void formatIT() {

		String filePath = HCUtil.path + "IT/C_17_dataset_96_0_upFile.csv";
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
		CSVUtil.save(hospitalsFormatted, HCUtil.path + "IT/IT_formatted.csv");
	}

}
