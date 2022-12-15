/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import eu.europa.ec.eurostat.java4eurostat.util.Util;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;

/**
 * @author julien Gaffuri
 *
 */
public class CSVUtil {


	/*public static void main(String[] args) {
		ArrayList<Map<String, String>> a = load("src/test/resources/csv/test.csv");
		System.out.println(a);
		save(a, "target/out.csv");
	}*/


	/**
	 * @param filePath
	 * @return
	 */
	public static ArrayList<String> getHeader(String filePath) {
		BufferedReader br = null;
		ArrayList<String> keys = null;
		try {
			br = new BufferedReader(new FileReader(filePath));
			Pattern pattern = Pattern.compile("\\s*(\"[^\"]*\"|[^,]*)\\s*");

			//read header
			String line = br.readLine();
			Matcher m = pattern.matcher(line);
			keys = new ArrayList<String>();
			while(m.find()){
				keys.add(m.group(1));
				m.find();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return keys;
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
				for(int i=0; i<nb; i++) {
					String val = raw.get(header[i]);
					//for numerical values, check if it is a int to avoid writing the ".0" in the end.
					if (Util.isNumeric(val) && (Double.parseDouble(val) % 1) == 0)
						values[i] = "" + (int)Double.parseDouble(val);
					else
						values[i] = val;
				}
				printer.printRecord(values);
			}
			printer.close();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static HashSet<String> getUniqueValues(Collection<Map<String, String>> data, String key, boolean print) {
		HashSet<String> values = new HashSet<String>();
		for(Map<String, String> obj : data)
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
		GeometryFactory gf = new GeometryFactory();
		for (Map<String, String> h : csvData) {
			Feature f = new Feature();
			Coordinate c = new Coordinate(0,0);
			for(Entry<String,String> e : h.entrySet()) {
				if(xCol.equals(e.getKey())) c.x = Double.parseDouble(e.getValue());
				if(yCol.equals(e.getKey())) c.y = Double.parseDouble(e.getValue());
				f.setAttribute(e.getKey(), e.getValue());
			}
			f.setGeometry(gf.createPoint(c));
			out.add(f);
		}
		return out;
	}


	/**
	 * Transform a feature collection into CSV data.
	 * 
	 * @param fs
	 * @return
	 */
	public static Collection<Map<String, String>> featuresToCSV(Collection<Feature> fs) {
		ArrayList<Map<String, String>> out = new ArrayList<>();
		for (Feature f : fs) {
			Map<String, String> o = new HashMap<>();
			for(Entry<String,Object> e : f.getAttributes().entrySet())
				o.put(e.getKey(), e.getValue().toString());
			out.add(o);
		}
		return out;
	}





	public static void setValue(Collection<Map<String, String>> data, String col, String value) {
		for(Map<String, String> r : data)
			r.put(col, value);
	}

	public static void addColumn(Collection<Map<String, String>> data, String col, String defaultValue) {
		for(Map<String, String> h : data) {
			if(h.get(col) == null || "".equals(h.get(col))) {
				h.put(col, defaultValue);
			}
		}
	}

	public static void addColumns(Collection<Map<String, String>> data, String[] cols, String defaultValue) {
		for(String col : cols)
			addColumn(data, col, defaultValue);
	}

	public static void removeColumn(Collection<Map<String, String>> data, String... cols) {
		for(String col : cols)
			for(Map<String, String> h : data) {
				if(h.get(col) != null)
					h.remove(col);
			}
	}

	public static void renameColumn(Collection<Map<String, String>> data, String oldName, String newName) {
		for(Map<String, String> h : data) {
			if(h.get(oldName) != null) {
				h.put(newName, h.get(oldName));
				h.remove(oldName);
			}
		}
	}

	public static void replaceValue(Collection<Map<String, String>> data, String col, String iniVal, String finVal) {
		for(Map<String, String> h : data) {
			String v = h.get(col);
			if(iniVal == null && v == null || iniVal != null && iniVal.equals(v))
				h.put(col, finVal);
		}
	}

	public static void replaceValue(Collection<Map<String, String>> data, String iniVal, String finVal) {
		for(Map<String, String> h : data)
			for(Entry<String,String> e : h.entrySet()) {
				String v = e.getValue();
				if(iniVal == null && v == null || iniVal != null && iniVal.equals(v))
					e.setValue(finVal);
			}
	}

	public static ArrayList<String> getValues(Collection<Map<String, String>> data, String col) {
		ArrayList<String> out = new ArrayList<>();
		for(Map<String, String> h : data)
			out.add(h.get(col));
		return out;
	}



	public static Collection<Map<String, String>> aggregateById(Collection<Map<String, String>> data, String idCol, String...  sumCols) {
		HashMap<String, Map<String, String>> ind = new HashMap<String, Map<String, String>>();
		for(Map<String, String> h : data) {
			String id  = h.get(idCol);
			Map<String, String> h_ = ind.get(id);
			if(h_ == null) {
				ind.put(id, h);
			} else {
				//increment number of beds
				for(String sumCol : sumCols) {					
					String nbs = h.get(sumCol);
					if(nbs == null || nbs.isEmpty()) continue;
					double nb = Double.parseDouble(nbs);
					String nbs_ = h_.get(sumCol);
					if(nbs_ == null || nbs_.isEmpty()) { h_.put(sumCol, ""+nb); continue; }
					double nb_ = Double.parseDouble(nbs_);
					h_.put(sumCol, ""+(nb+nb_));
				}
			}
		}
		return ind.values();
	}


	/**
	 * Join on one side.
	 * If no element match on the second table, nothing is done.
	 * 
	 * @param data1
	 * @param key1
	 * @param data2
	 * @param key2
	 * @param printWarnings
	 */
	public static void join(List<Map<String, String>> data1, String key1, List<Map<String, String>> data2, String key2, boolean printWarnings) {
		//index data2 by key
		HashMap<String,Map<String,String>> ind2 = new HashMap<>();
		for(Map<String, String> elt : data2) ind2.put(elt.get(key2), elt);

		//join
		for(Map<String, String> elt : data1) {
			String k1 = elt.get(key1);
			Map<String, String> elt2 = ind2.get(k1);
			if(elt2 == null) {
				if(printWarnings) System.out.println("No element to join for key: " + k1);
				continue;
			}
			elt.putAll(elt2);
		}
	}





	/**
	 * Join on both sides
	 * 
	 * @param idProp
	 * @param data1
	 * @param data2
	 * @param defaultValue
	 * @param printWarnings
	 * @return
	 */
	public static ArrayList<Map<String, String>> joinBothSides(String idProp, ArrayList<Map<String, String>> data1, ArrayList<Map<String, String>> data2, String defaultValue, boolean printWarnings) {
		//special cases
		if(data1.size() ==0) return data2;
		if(data2.size() ==0) return data1;

		//get all ids
		HashSet<String> ids = new HashSet<>();
		for(Map<String, String> c : data1) {
			if(ids==null || c==null || idProp == null || c.get(idProp) == null) {
				System.out.println(data1);
				System.out.println(data1.size());
				System.out.println(c);
				System.out.println(idProp);
				System.out.println(c.get(idProp));
				System.exit(0);
			}
			ids.add(c.get(idProp));
		}
		for(Map<String, String> c : data2) {
			if(data2==null) System.err.println("data2");
			if(ids==null) System.err.println("ids");
			if(c==null) System.err.println("c");
			if(c.get(idProp)==null) System.err.println("c.get(idProp)");
			ids.add(c.get(idProp));
		}

		//index data1 and data2 by id
		HashMap<String,Map<String,String>> ind1 = new HashMap<>();
		for(Map<String, String> e : data1) ind1.put(e.get(idProp), e);
		HashMap<String,Map<String,String>> ind2 = new HashMap<>();
		for(Map<String, String> e : data2) ind2.put(e.get(idProp), e);

		//get key sets
		Set<String> ks1 = data1.get(0).keySet();
		Set<String> ks2 = data2.get(0).keySet();

		//build output
		ArrayList<Map<String, String>> out = new ArrayList<>();
		for(String id : ids) {
			Map<String, String> e1 = ind1.get(id);
			Map<String, String> e2 = ind2.get(id);

			//make template
			Map<String, String> e = new HashMap<>();
			for(String k : ks1) if(k!=idProp) e.put(k, defaultValue);
			for(String k : ks2) if(k!=idProp) e.put(k, defaultValue);
			e.put(idProp, id);

			//set data 1
			if(e1 != null)
				for(String k : ks1) if(k!=idProp) e.put(k, e1.get(k));
			//set data 2
			if(e2 != null)
				for(String k : ks2) if(k!=idProp) e.put(k, e2.get(k));

			if(printWarnings && (e1==null || e2==null))
				System.out.println("No element to join for id: " + id);

			out.add(e);
		}

		return out;
	}

}
