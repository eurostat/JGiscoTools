/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gridstat.tiling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.java4eurostat.base.Stat;
import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.io.CSV;

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
	private Collection<GridStatTile> tiles;
	private class GridStatTile {
		public String code;
		public ArrayList<Stat> stats = new ArrayList<Stat>();
		GridStatTile(String code) { this.code=code; }
	}


	public GridStatTiler(StatsHypercube sh, double resolution) {
		this.sh = sh;
		this.resolution = resolution;
	}

	public GridStatTiler(String csvFilePath, String statAttr, double resolution) {
		this( CSV.load(csvFilePath, statAttr), resolution );
	}

	public void createTiles(int minZoomLevel, int maxZoomLevel) {
		//create tile dictionnary
		HashMap<String,GridStatTile> tiles_ = new HashMap<String,GridStatTile>();

		//go through stats and assign it to tile
		for(Stat s : sh.stats) {
			//get grid tile
			String gridId = s.dims.get(gridIdAtt);

			for(int zoomLevel = minZoomLevel; zoomLevel<=maxZoomLevel; zoomLevel++) {
				//get id of the tile it should belong to
				String tileCode = getTileCode(zoomLevel, gridId);

				//create tile if it does not exists and add stat to it
				GridStatTile tile = tiles_.get(tileCode);
				if(tile == null) {
					tile = new GridStatTile(tileCode);
					tiles_.put(tileCode, tile);
				}
				tile.stats.add(s);
			}

		}
		tiles = tiles_.values();
	}


	private String getTileCode(int zoomLevel, String gridId) {
		//TODO
		
		return null;
	}

	public void save(String folderPath) {
		for(GridStatTile tile : tiles) {
			//TODO
		}
	}

}
