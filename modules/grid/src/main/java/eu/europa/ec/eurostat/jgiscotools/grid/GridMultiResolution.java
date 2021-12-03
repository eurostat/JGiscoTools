/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.grid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Generate grids with lower resolution.
 * 
 * @author julien Gaffuri
 *
 */
public class GridMultiResolution {

	/**
	 * Aggregate cell data (from CSV file usually) into a target resolution lower.
	 * The target resolution is supposed to be a multiple of the input grid resolution.
	 * Sum of attributes. Attributes are numerical values.
	 * 
	 * @param cells Input grid
	 * @param gridIdCol The column with the cell id
	 * @param res The target resolution
	 * @param factor A factor to avoid floating precision errors, such as "245.0000000034". Set to 10000
	 * @return
	 */
	public static ArrayList<Map<String, String>> gridAggregation(ArrayList<Map<String, String>> cells, String gridIdCol, int res, int factor) {	

		//index input data by upper grid cell
		HashMap<String, List<Map<String, String>>> index = new HashMap<>();
		for(Map<String, String> cell : cells) {

			//get upper cell
			GridCell up = new GridCell(cell.get(gridIdCol)).getUpperCell(res);
			String id = up.getId();

			//get upper cell list
			List<Map<String, String>> list = index.get(id);
			if(list == null) {
				list = new ArrayList<>();
				index.put(id, list);
			}
			list.add(cell);
		}

		//make output
		ArrayList<Map<String, String>> out = new ArrayList<Map<String,String>>();
		for(Entry<String, List<Map<String, String>>> e : index.entrySet()) {
			//make aggregate cell
			Map<String, String> aggCell = new HashMap<String, String>();
			//set id
			aggCell.put(gridIdCol, e.getKey());
			//aggregate values, as sum, keys after keys
			Set<String> keys = e.getValue().get(0).keySet();
			for(String key : keys) {
				if(key.equals(gridIdCol)) continue;
				double sum = 0;
				for(Map<String, String> cell : e.getValue()) {
					String s = cell.get(key);
					double v = Double.parseDouble(s);
					sum += factor * v;
				}
				sum /= factor;
				String sumS = (sum % 1) == 0 ? Integer.toString((int)sum) : Double.toString(sum);
				aggCell.put(key, sumS);
			}
			out.add(aggCell);
		}

		return out;
	}

}
