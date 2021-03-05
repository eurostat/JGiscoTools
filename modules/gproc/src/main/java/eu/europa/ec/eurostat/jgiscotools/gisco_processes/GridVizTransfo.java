package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;

import eu.europa.ec.eurostat.jgiscotools.grid.GridCell;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class GridVizTransfo {

	public static void main(String[] args) {
	
		
		System.out.println("load");
		CSVFormat cf = CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(';');
		ArrayList<Map<String, String>> d = CSVUtil.load("E:\\workspace\\basic_services_accessibility\\accessibility_output\\avg_time_primary_educ.csv", cf );

		System.out.println(d.size());

		CSVUtil.removeColumn(d, "TOT_P_2018");

		for(Map<String, String> d_ : d) {
			String id = d_.get("GRD_ID");
			GridCell gc = new GridCell(id);
			d_.put("x", gc.getLowerLeftCornerPositionX()/1000+"");
			d_.put("y", gc.getLowerLeftCornerPositionY()/1000+"");
			d_.put("avg_time", ((int)Double.parseDouble(d_.get("AVG_TIME").replace(',','.')))+"");
		}

		CSVUtil.removeColumn(d, "GRD_ID", "AVG_TIME");
		
		System.out.println("save");
		CSVUtil.save(d, "E:\\users\\gaffuju\\eclipse_workspace\\gridviz\\assets\\csv\\Europe\\1km\\accs.csv");
	}

}
