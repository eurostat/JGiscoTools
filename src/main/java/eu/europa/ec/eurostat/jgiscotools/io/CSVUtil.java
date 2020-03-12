/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.io;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;

/**
 * @author julien Gaffuri
 *
 */
public class CSVUtil {


	public static void main(String[] args) {
		ArrayList<Map<String, String>> a = load("src/test/resources/csv/test.csv");
		System.out.println(a);
		save(a, "target/out.csv");
	}


	/**
	 * @param filePath
	 * @return
	 */
	public static ArrayList<Map<String,String>> load(String filePath) {
		return load(filePath, CSVFormat.DEFAULT.withFirstRecordAsHeader());
	}

	/**
	 * @param filePath
	 * @param cf
	 * @return
	 */
	public static ArrayList<Map<String,String>> load(String filePath, CSVFormat cf) {
		ArrayList<Map<String,String>> data = new ArrayList<>();
		try {
			//parse file
			Reader in = new FileReader(filePath);
			Iterable<CSVRecord> raws = cf.parse(in);

			//read data
			for (CSVRecord raw : raws) data.add(raw.toMap());

			in.close();
		} catch (Exception e) { e.printStackTrace(); }
		return data;
	}



	//save a csv file
	public static void save(Collection<Map<String, String>> data, String outFile) {
		ArrayList<String> header = new ArrayList<>( data.iterator().next().keySet() );
		CSVFormat cf = CSVFormat.DEFAULT.withHeader(header.toArray(new String[header.size()]));
		save(data, outFile, cf);
	}

	public static void save(Collection<Map<String, String>> data, String outFile, List<String> header) {
		CSVFormat cf = CSVFormat.DEFAULT.withHeader(header.toArray(new String[header.size()]));
		save(data, outFile, cf);
	}

	public static void save(Collection<Map<String, String>> data, String outFile, CSVFormat cf) {
		try {
			FileWriter out = new FileWriter(outFile);
			String[] header = cf.getHeader(); int nb = header.length;
			CSVPrinter printer = new CSVPrinter(out, cf);
			for(Map<String, String> raw : data) {
				String[] values = new String[nb];
				for(int i=0; i<nb; i++) values[i]=raw.get(header[i]);
				printer.printRecord(values);
			}
			printer.close();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static HashSet<String> getUniqueValues(Collection<HashMap<String, String>> data, String key, boolean print) {
		HashSet<String> values = new HashSet<String>();
		for(HashMap<String, String> obj : data)
			values.add(obj.get(key));
		if(print){
			System.out.println(key + " " + values.size()+" values");
			System.out.println(values);
		}
		return values;
	}

	public static ArrayList<HashMap<String, String>> getSubset(ArrayList<HashMap<String, String>> data, String key, String value) {
		ArrayList<HashMap<String, String>> dataOut = new ArrayList<HashMap<String, String>>();
		for(HashMap<String, String> obj : data)
			if(value.equals(obj.get(key))) dataOut.add(obj);
		return dataOut;
	}

	public static void save(List<String> data, String outFile) {
		ArrayList<Map<String, String>> data_ = new ArrayList<>();
		for(int i=0; i<data.size(); i++) {
			HashMap<String, String> m = new HashMap<>();
			m.put("id", i+"");
			m.put("val", data.get(i));
			data_.add(m);
		}
		ArrayList<String> keys = new ArrayList<String>(); keys.add("id"); keys.add("val");
		save(data_, outFile, keys);
	}




	/**
	 * Transform CSV data into a feature collection, with point geometry.
	 * 
	 * @param csvData
	 * @param xCol
	 * @param yCol
	 * @return
	 */
	public static Collection<Feature> CSVToFeatures(Collection<Map<String, String>> csvData, String xCol, String yCol) {
		Collection<Feature> out = new ArrayList<Feature>();
		for (Map<String, String> h : csvData) {
			Feature f = new Feature();
			Coordinate c = new Coordinate(0,0);
			for(Entry<String,String> e : h.entrySet()) {
				if(xCol.equals(e.getKey())) c.x = Double.parseDouble(e.getValue());
				if(yCol.equals(e.getKey())) c.y = Double.parseDouble(e.getValue());
				f.setAttribute(e.getKey(), e.getValue());
			}
			out.add(f);
		}
		return out;
	}
	
}
