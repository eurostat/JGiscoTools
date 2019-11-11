/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gridstat.tiling;

import java.util.ArrayList;
import java.util.HashMap;

import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.java4eurostat.base.Stat;
import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.io.CSV;
import eu.europa.ec.eurostat.jgiscotools.grid.GridCell;
import eu.europa.ec.eurostat.jgiscotools.grid.GridUtil;

/**
 * 
 * Utility to create tiles of grided statistics.
 * Tile dimension is 256*256.
 * 
 * @author Julien Gaffuri
 *
 */
public class GridStatTiler {

	/**
	 * The statistical figures.
	 */
	private StatsHypercube sh;

	/**
	 * The input grid resolution
	 */
	private double resolution;

	/**
	 * The label of the attribute which contains the grid id
	 */
	private String gridIdAtt = "GRD_ID";

	/**
	 * The position of origin of the grid CRS to take into account to defining the tiling frame.
	 * All tiles are 256*256.
	 */
	private Coordinate cRSOriginPoint = new Coordinate(0,0);

	/**
	 * The computed tiles.
	 * All tiles are 256*256.
	 */
	private HashMap<String,GridStatTile> tiles;
	private class GridStatTile {
		public ArrayList<Stat> stats = new ArrayList<Stat>();
	}


	public GridStatTiler(StatsHypercube sh, double resolution) {
		this.sh = sh;
		this.resolution = resolution;
	}

	public GridStatTiler(String csvFilePath, String statAttr, double resolution) {
		this( CSV.load(csvFilePath, statAttr), resolution );
	}

	public void computeTiles(int minZoomLevel, int maxZoomLevel) {
		//create tile dictionnary
		tiles = new HashMap<String,GridStatTile>();

		//go through stats and assign it to tile
		for(Stat s : sh.stats) {
			//get grid tile
			String gridId = s.dims.get(gridIdAtt);

			for(int zoomLevel = minZoomLevel; zoomLevel<=maxZoomLevel; zoomLevel++) {
				//get id of the tile it should belong to
				String tileId = getTileId(zoomLevel, gridId);

				//create tile if it does not exists and add stat to it
				GridStatTile tile = tiles.get(tileId);
				if(tile == null) {
					tile = new GridStatTile();
					tiles.put(tileId, tile);
				}
				tile.stats.add(s);
			}

		}
	}


	private static String getTileId(int zoomLevel, String gridId) {
		//TODO
		return null;
	}

	public void save(String folderPath) {
		//TODO
	}

}
