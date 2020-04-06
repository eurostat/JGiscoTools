package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.cntr;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;

import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.HCUtil;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class PL_attempt {

	public static void main(String[] args) {
		System.out.println("Start");

		try {
			//load data
			CSVFormat f = CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(';');
			Collection<Map<String, String>> data = CSVUtil.load(HCUtil.path + "PL/CSV_Rpm_aktywne/komorki_cells.csv", f);
			System.out.println(data.size());

			data = CSVUtil.aggregateById(data, "ID Jednostki", "Liczba łóżek ogółem" );
			System.out.println(data.size());

			for(String col : new String[] {
					"ID Księgi",
					"ID ZOZ",
					"ID Komórki",
					"Regon",
					"Kod komórki",
					"Specjalność komórki",
					"kodResortVIII",
					"Nazwa komórki",
					"Teryt opis",
					"Teryt",
					"Kod SIMC",
					"Kod ulicy",
					"Lokal",
					"Liczba łóżek intensywnej opieki medycznej",
					"Liczba łóżek intensywnego nadzoru kardiologicznego",
					"Liczba łóżek dla noworodków",
					"Liczba inkubatorów",
					"Liczba łóżek intensywnej terapii",
					"Liczba łóżek intensywnej opieki oparzeń",
					"Liczba łóżek intensywnej opieki toksykologicznej",
					"Liczba łóżek intensywnej terapii noworodka",
					"Liczba łóżek opieki ciągłej dla noworodków po sztucznej wentylacji",
					"Liczba łóżek opieki pośredniej dla noworodków niewymagających wsparcia oddechowego",
					"Liczba łóżek nieinwazyjnej wentylacji mechanicznej",
					"Liczba stanowisk dializacyjnych",
					"Liczba miejsc pobytu dziennego",
					"Data rozpoczęcia działalności komórki",
					"Data zakończenia działalności komórki",
					"Początek okresu zawieszenia",
					"Koniec okresu zawieszenia"					
			})
				CSVUtil.removeColumn(data, col);

			CSVUtil.renameColumn(data, "ID Jednostki", "id");
			CSVUtil.renameColumn(data, "Kod jednostki", "facility_type");

			CSVUtil.renameColumn(data, "Miejscowość", "city");
			CSVUtil.renameColumn(data, "Kod pocztowy", "postcode");
			CSVUtil.renameColumn(data, "Ulica", "street");
			CSVUtil.renameColumn(data, "Budynek", "house_number");

			CSVUtil.renameColumn(data, "Telefon", "tel");
			CSVUtil.renameColumn(data, "Email", "email");
			CSVUtil.renameColumn(data, "Strona WWW", "url");

			CSVUtil.renameColumn(data, "Liczba łóżek ogółem", "cap_beds");

			CSVUtil.addColumn(data, "cc", "PL");
			CSVUtil.addColumn(data, "ref_date", "29/03/2020");

			//replace "NULL" values
			CSVUtil.replaceValue(data, "NULL", "");
			//cap_beds to int
			for(Map<String, String> d : data)
				d.put("cap_beds", ""+((int)Double.parseDouble(d.get("cap_beds"))));
			//TODO get hospital name

			// save 1
			System.out.println(data.size());
			CSVUtil.addColumns(data, HCUtil.cols, "");
			CSVUtil.save(data, HCUtil.path + "PL/PL_formatted.csv");

			//geocode
			//LocalParameters.loadProxySettings();
			//ServicesGeocoding.set(BingGeocoder.get(), data, "lon", "lat", true, true);

			// save 2
			//CSVUtil.save(data, HCUtil.path + "PL/PL.csv");
			//GeoData.save(CSVUtil.CSVToFeatures(data, "lon", "lat"), HCUtil.path + "PL/PL.gpkg", ProjectionUtil.getWGS_84_CRS());
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("End");
	}
}
