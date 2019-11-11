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
import eu.europa.ec.eurostat.jgiscotools.grid.GridCell;

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
	 * The label of the attribute which contains the grid id
	 */
	private String gridIdAtt = "GRD_ID";

	/**
	 * The position of origin of the grid CRS to take into account to defining the tiling frame.
	 * All tiles are 256*256.
	 * TODO For LAEA, take (0,6000000).
	 */
	private Coordinate cRSOriginPoint = new Coordinate(0,0);

	/**
	 * The computed tiles.
	 * All tiles are 256*256.
	 */
	private Collection<GridStatTile> tiles;
	private class GridStatTile {
		public String id;
		public ArrayList<Stat> stats = new ArrayList<Stat>();
		GridStatTile(String id) { this.id=id; }
	}


	public GridStatTiler(StatsHypercube sh) {
		this.sh = sh;
	}

	public GridStatTiler(String csvFilePath, String statAttr) {
		this( CSV.load(csvFilePath, statAttr) );
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
				String tileId = getTileId(zoomLevel, gridId);

				//create tile if it does not exists and add stat to it
				GridStatTile tile = tiles_.get(tileId);
				if(tile == null) {
					tile = new GridStatTile(tileId);
					tiles_.put(tileId, tile);
				}
				tile.stats.add(s);
			}
		}
		tiles = tiles_.values();
	}


	private String getTileId(int zoomLevel, String gridId) {
		GridCell cell = new GridCell(gridId);
		//cell.
		int tileCellSize = (int) (256 * Math.pow(2, -zoomLevel));

		//TODO

		return null;
	}

	public void save(String folderPath) {
		for(GridStatTile tile : tiles) {
			//TODO
		}
	}

}
