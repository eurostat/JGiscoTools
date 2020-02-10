/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

/**
 * @author julien Gaffuri
 *
 */
public class CSVUtil {


	public static void main(String[] args) {
		ArrayList<Map<String, String>> a = load("src/test/resources/csv/test.csv");
		System.out.println(a);
		save2(a, "target/out.csv");
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




	public static void save2(Collection<Map<String, String>> data, String outFile) {
		save2(data, outFile, CSVFormat.DEFAULT.withFirstRecordAsHeader());
	}

	public static void save2(Collection<Map<String, String>> data, String outFile, CSVFormat cf) {
		try {
			FileWriter out = new FileWriter(outFile);
			CSVPrinter printer = new CSVPrinter(out, cf);
			for(Map<String, String> raw : data) {
				//TODO !!!
				printer.printRecord(raw.entrySet());
			}
			printer.close();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}




	//save a csv file
	public static void save(Collection<Map<String, String>> data, String outFile) { save(data, outFile, null); }
	public static void save(Collection<Map<String, String>> data, String outFile, List<String> keys) {
		try {
			if(data.size()==0){
				System.err.println("Cannot save CSV file: Empty dataset.");
				return;
			}

			//create output file
			File f = FileUtil.getFile(outFile, true, true);
			BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));

			//write header
			if(keys==null) keys = new ArrayList<String>(data.iterator().next().keySet());
			int i=0;
			for(String key : keys ){
				bw.write(key);
				if(i<keys.size()-1) bw.write(",");
				i++;
			}
			bw.write("\n");

			//write data
			for(Map<String, String> obj : data){
				i=0;
				for(String key : keys){
					Object v = obj.get(key);
					if(v == null) System.err.println("Could not find value for key "+key);
					bw.write( v == null? "null" : v.toString() );
					if(i<keys.size()-1) bw.write(",");
					i++;
				}
				bw.write("\n");

				/*Collection<String> values = obj.values(); i=0;
				for(String value:values){
					bw.write(value);
					if(i<values.size()-1) bw.write(",");
					i++;
				}
				bw.write("\n");*/
			}
			bw.close();
		} catch (Exception e) {e.printStackTrace();}
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

}
