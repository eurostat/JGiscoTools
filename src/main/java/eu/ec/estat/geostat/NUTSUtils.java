package eu.ec.estat.geostat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

/**
 * 
 * @author julien Gaffuri
 *
 */
public class NUTSUtils {
	//TODO develop/extract generic functions on NUTS regions

	public static void main(String[] args) {
		load2010_2013();
	}

	private static class NUTSChange{
		public String codeIni, codeFin, change, explanation;
		@Override
		public String toString() { return codeIni+" -> "+codeFin+" - "+change+" - "+explanation; }
	}

	private static HashSet<NUTSChange> changes2010_2013 = null;
	private static HashMap<String,NUTSChange> changes2010_2013I1 = null;
	private static void load2010_2013(){
		if(changes2010_2013 != null) return;
		changes2010_2013 = new HashSet<NUTSChange>();
		changes2010_2013I1 = new HashMap<String,NUTSChange>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("resources/nuts_changes/NUTS_changes_2010_2013.csv"));
			//skip first line
			String line = br.readLine();
			//read data
			while ((line = br.readLine()) != null) {
				String[] elts = line.split(",", -1);
				if(elts.length != 4) System.out.println(elts.length);
				NUTSChange nc = new NUTSChange(); nc.codeIni=elts[0]; nc.codeFin=elts[1]; nc.change=elts[2]; nc.explanation=elts[3];
				changes2010_2013.add(nc);
				if(nc.codeIni != null && !"".equals(nc.codeIni)) changes2010_2013I1.put(nc.codeIni, nc);
			}
		} catch (IOException e) { e.printStackTrace();
		} finally {
			try { if (br != null)br.close(); } catch (Exception ex) { ex.printStackTrace(); }
		}
	}

	public String get2010To2013Code(String code2010){
		load2010_2013();
		NUTSChange nc = changes2010_2013I1.get(code2010);
		if(nc == null) return null;
		if("".equals(nc.codeFin)) return null;
		return nc.codeFin;
	}

}


//Boundary shift
//Code change
//Code; name change
//Merged
//Name change
//New region
//Split

