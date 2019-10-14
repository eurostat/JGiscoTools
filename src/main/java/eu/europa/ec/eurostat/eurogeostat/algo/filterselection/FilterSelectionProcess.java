/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.algo.filterselection;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author julien Gaffuri
 *
 */
public class FilterSelectionProcess {

	//criteria
	/*public static void filter(String inFilePath, String inFileName, String geomAtt, String outFilePath, String outFileName, double dp, SelectionCriteria[] scs) {
		//load shp
		SHPData data = SHPUtils.loadSHP(inFilePath+inFileName);
		//filter
		data.fs = filter(data.fs, geomAtt, dp, scs);
		//save shp
		SHPUtils.saveSHP(data.ft, data.fs, outFilePath, outFileName);
	}*/

	public static ArrayList<?> filter(Collection<?> objs, SelectionCriteria[] scs) {
		ArrayList<Object> objs2 = new ArrayList<Object>();

		//apply filter
		for(Object o:objs){
			//check if has to be kept
			boolean keep=true;
			if(scs!=null){
				for(SelectionCriteria sc:scs)
					if(!sc.keep(o)){ keep=false; break; }
			}
			if(!keep) continue;

			objs2.add(o);
		}
		return objs2;
	}
}
