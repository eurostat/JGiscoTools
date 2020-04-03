package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.cntr;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;

import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.health.HCUtil;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class HU {

	public static void main(String[] args) {
		System.out.println("Start");

		try {
			//load data
			CSVFormat cf = CSVFormat.DEFAULT.withDelimiter('\t').withFirstRecordAsHeader();
			Collection<Map<String, String>> data = CSVUtil.load(HCUtil.path + "HU/NEAK_Fin_2020.01_02.csv", cf);
			System.out.println(data.size());
			
			//TODO

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("End");
	}

}
