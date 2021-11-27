/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.grid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
	 * @return
	 */
	public static ArrayList<Map<String, String>> gridAggregation(ArrayList<Map<String, String>> cells, String gridIdCol, int res) {	

		//index input data by upper grid cell
		HashMap<String, Map<String, String>> index = new HashMap<>();
		for(Map<String, String> cell : cells) {

			//get upper cell
			GridCell up = new GridCell(cell.get(gridIdCol)).getUpperCell(res);
			String id = up.getId();

			//get upper cell
			Map<String, String> cellAgg = index.get(id);

			if(cellAgg == null) {
				//create
				cellAgg = new HashMap<String, String> ();
				cellAgg.putAll(cell);
				index.put(id, cellAgg);
			}
			else {
				//add
				add(cellAgg, cell, gridIdCol);
			}

			//set grid id
			cellAgg.put(gridIdCol, id);

		}

		//make output
		ArrayList<Map<String, String>> out = new ArrayList<Map<String,String>>();
		out.addAll(index.values());

		return out;
	}


	/**
	 * @param cell
	 * @param cellToAdd
	 * @param gridIdCol
	 */
	private static void add(Map<String, String> cell, Map<String, String> cellToAdd, String gridIdCol) {
		for(String k : cell.keySet()) {
			if(k.equals(gridIdCol)) continue;
			double v = Double.parseDouble(cell.get(k));
			double vToAdd = Double.parseDouble(cellToAdd.get(k));
			v += vToAdd;
			//get value as int if it is an integer
			cell.put(k, (v % 1) == 0 ? Integer.toString((int)v) : Double.toString(v) );
		}
	}
	
}
