package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.cntr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;

import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.HCUtil;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.Validation;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class HU {

	public static void main(String[] args) {
		System.out.println("Start");

		String cc = "HU";
		
		try {
			//load data
			CSVFormat cf = CSVFormat.DEFAULT.withDelimiter('\t').withFirstRecordAsHeader();
			Collection<Map<String, String>> data = CSVUtil.load(HCUtil.path + "HU/NEAK_Fin_2020.01_02.csv", cf);
			System.out.println(data.size());


			//todo filter
			Collection<Map<String, String>> data_ = new ArrayList<>();
			for(Map<String, String> d : data) {
				if(d.get("Aktív fekvőbeteg-szakellátás").isEmpty()
						&& d.get("Fekvőbeteg-szakellátás").isEmpty()
						&& d.get("Járó és - vagy fekvőbeteg-szakellátás").isEmpty()
						/*&& d.get("Járóbeteg-szakellátás").isEmpty()*/)
					continue;
				data_.add(d);
			}
			data = data_; data_ = null;
			System.out.println(data.size());

			CSVUtil.removeColumn(data, "Megyekód");
			CSVUtil.removeColumn(data, "Megye név"); //TODO keep it for address?

			CSVUtil.removeColumn(data, "Csak CT");
			CSVUtil.removeColumn(data, "Csak Labor");
			CSVUtil.removeColumn(data, "Csak Járó");
			CSVUtil.removeColumn(data, "Csak Aktív");
			CSVUtil.removeColumn(data, "Csak Krónikus");

			//TODO keep that for facility type?
			CSVUtil.removeColumn(data, "Aktív fekvőbeteg-szakellátás");
			CSVUtil.removeColumn(data, "Fekvőbeteg-szakellátás");
			CSVUtil.removeColumn(data, "Járó és - vagy fekvőbeteg-szakellátás");
			CSVUtil.removeColumn(data, "Járóbeteg-szakellátás");

			CSVUtil.renameColumn(data, "Int.kód", "id");
			CSVUtil.renameColumn(data, "Irányítószám", "postcode");
			CSVUtil.renameColumn(data, "Település", "city");
			CSVUtil.renameColumn(data, "Utca", "street");
			//CSVUtil.renameColumn(data, "", "");

			for(Map<String, String> d : data) {
				//tel
				d.put("tel", d.get("Tel.körzet") + "-" + d.get("Telefonszám"));
				
				//System.out.println(d.get("Szolgáltató"));
			}
			CSVUtil.removeColumn(data, "Telefonszám");
			CSVUtil.removeColumn(data, "Tel.körzet");

			CSVUtil.addColumn(data, "cc", cc);
			CSVUtil.addColumn(data, "ref_date", "01/02/2020");

			CSVUtil.addColumn(data, "emergency", "");
			CSVUtil.addColumn(data, "public_private", "");
			CSVUtil.addColumn(data, "geo_qual", "3");
			CSVUtil.addColumn(data, "lat", "0");
			CSVUtil.addColumn(data, "lon", "0");

			Validation.validate(data, "HU");

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("End");
	}

}
