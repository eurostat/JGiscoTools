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
	 * The position of origin of the grid to take into account to defining the tiling frame.
	 * Tiling numbering goes from left to right, and from top to bottom.
	 * It should be the top left corner of the tiling frame.
	 * For LAEA, take (0,6000000).
	 */
	private Coordinate originPoint = new Coordinate(0,0);

	/**
	 * The computed tiles.
	 * All tiles are 256*256.
	 */
	private Collection<GridStatTile> tiles;
	private class GridStatTile {
		public int x,y,z;
		public ArrayList<Stat> stats = new ArrayList<Stat>();
		GridStatTile(int x, int y, int z) { this.x=x; this.y=y; this.z=z; }
	}


	public GridStatTiler(StatsHypercube sh) {
		this.sh = sh;
	}

	public GridStatTiler(String csvFilePath, String statAttr) {
		this( CSV.load(csvFilePath, statAttr) );
	}

	public void createTiles(int minZoomLevel, int maxZoomLevel) {
		//create tile dictionnary tileId -> tile
		HashMap<String,GridStatTile> tiles_ = new HashMap<String,GridStatTile>();

		//go through cell stats and assign it to a tile
		for(Stat s : sh.stats) {
			//get cell information
			String gridId = s.dims.get(gridIdAtt);
			GridCell cell = new GridCell(gridId);
			double x = cell.getLowerLeftCornerPositionX();
			double y = cell.getLowerLeftCornerPositionY();
			int resolution = cell.getResolution();

			for(int z = minZoomLevel; z<=maxZoomLevel; z++) {
				//get id of the tile the cell should belong to

				//compute tile information
				int tileCellSize = (int) (256 * Math.pow(2, -z));
				int tileSize = tileCellSize * resolution;

				//find tile position
				int xt = (int)( (x-originPoint.x)/tileSize );
				//TODO revert
				int yt = (int)( (y-originPoint.y)/tileSize );
				String tileId = z+"_"+xt+"_"+yt;

				//create tile if it does not exists and add stat to it
				GridStatTile tile = tiles_.get(tileId);
				if(tile == null) {
					tile = new GridStatTile(xt, yt, z);
					tiles_.put(tileId, tile);
				}
				tile.stats.add(s);
			}
		}
		tiles = tiles_.values();
	}


	public void save(String folderPath) {
		//go through tiles
		for(GridStatTile t : tiles) {
			System.out.println( "/" +t.z+ "/" +t.x+ "/" +t.y+ ".csv" );
			StatsHypercube sth = new StatsHypercube(sh.getDimLabels());
			for(Stat s : sth.stats) {
				String gridId = s.dims.get(gridIdAtt);
				GridCell cell = new GridCell(gridId);
				//TODO change x,y into tile coordinates
				int x = cell.getLowerLeftCornerPositionX();
				int y = cell.getLowerLeftCornerPositionY();
				s.dims.put("x", ""+(int)x);
				s.dims.put("y", ""+(int)y);
				Stat s_ = new Stat(s.value,"x",""+x,"y",""+y);
				sth.stats.add(s_);
			}
			CSV.save(sth, "val", folderPath + "/" +t.z+ "/" +t.x+ "/" +t.y+ ".csv");
		}
	}

}
