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
import eu.europa.ec.eurostat.java4eurostat.io.JSONStat;
import eu.europa.ec.eurostat.jgiscotools.grid.GridCell;

/**
 * 
 * Utility to create tiles of grided statistics.
 * 
 * Notes on zoom levels:
 * - Tiling should be restricted to zoom levels from 0 to 4.
 * - Zoom level 0: level where tiles are composed of 256*256 grid cells
 * - Zoom level 4: level where tiles are composed of 16*16 grid cells
 * - Tile dimension is equal to 2^(8-z) in cell nb, where z is the zoom level
 * - Better visualisation scale for zoom level 0 is the one where 1 pixel screen corresponds to one grid cell
 * 
 * @author Julien Gaffuri
 *
 */
public class GridStatTiler {

	/**
	 * @param zoomLevel
	 * @return The tile size at this zoomLevel, in number of pixels.
	 */
	private static int getTileSizeCellNb(int zoomLevel) {
		return (int) Math.pow(2, 8-zoomLevel);
	}


	/**
	 * The statistical figures to tile
	 */
	private StatsHypercube sh;

	/**
	 * The name of the attribute which contains the grid id
	 */
	private String gridIdAtt = "GRD_ID";

	/**
	 * The position of origin of the grid to take into account to defining the tiling frame.
	 * It should be the bottom left corner of the tiling frame.
	 * Tiling numbering goes from left to right, and from bottom to top.
	 * For LAEA, take (0,0).
	 */
	private Coordinate originPoint = new Coordinate(0,0);

	/**
	 * The computed tiles.
	 */
	private Collection<GridStatTile> tiles;
	public Collection<GridStatTile> getTiles() { return tiles; }

	private class GridStatTile {
		public int x,y,z;
		public ArrayList<Stat> stats = new ArrayList<Stat>();
		GridStatTile(int x, int y, int z) { this.x=x; this.y=y; this.z=z; }
	}


	public GridStatTiler(String csvFilePath, String statAttr) {
		this( CSV.load(csvFilePath, statAttr) );
	}

	public GridStatTiler(StatsHypercube sh) {
		this.sh = sh;
	}


	/**
	 * Build the tiles for zoom levels 0 to 4.
	 */
	public void createTiles() { createTiles(0,4); }

	/**
	 * Build the tiles for several zoom levels.
	 * 
	 * @param minZoomLevel
	 * @param maxZoomLevel
	 */
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

			for(int z=minZoomLevel; z<=maxZoomLevel; z++) {
				//get id of the tile the cell should belong to

				//compute tile information
				int tileCellSize = getTileSizeCellNb(z);
				int tileSize = tileCellSize * resolution;

				//find tile position
				int xt = (int)( (x-originPoint.x)/tileSize );
				int yt = (int)( (y-originPoint.y)/tileSize );
				String tileId = z+"_"+xt+"_"+yt;

				//get tile. If it does not exists, create it.
				GridStatTile tile = tiles_.get(tileId);
				if(tile == null) {
					tile = new GridStatTile(xt, yt, z);
					tiles_.put(tileId, tile);
				}
				
				//add cell to tile
				tile.stats.add(s);
			}
		}
		tiles = tiles_.values();
	}


	/**
	 * Save the tile pyramid as CSV.
	 * 
	 * @param folderPath
	 */
	public void saveCSV(String folderPath) {
		//go through tiles
		for(GridStatTile t : tiles) {
			//compute tile information
			int tileSizeCellNb = getTileSizeCellNb(t.z);

			//build sh for the tile
			StatsHypercube sht = new StatsHypercube(sh.getDimLabels());
			sht.dimLabels.add("x");
			sht.dimLabels.add("y");
			sht.dimLabels.remove(gridIdAtt);

			//prepare tile stats for export
			for(Stat s : t.stats) {

				//get cell position
				GridCell cell = new GridCell( s.dims.get(gridIdAtt) );
				double x = cell.getLowerLeftCornerPositionX();
				double y = cell.getLowerLeftCornerPositionY();

				//compute cell position in tile space
				int tileSize = tileSizeCellNb * cell.getResolution();
				x = tileSizeCellNb * ((1.0*cell.getLowerLeftCornerPositionX())/(1.0*tileSize) - t.x);
				y = tileSizeCellNb * ((1.0*cell.getLowerLeftCornerPositionY())/(1.0*tileSize) - t.y);

				/*/check x,y values. Should be within [0,tileSizeCellNb-1]
				if(x==0) System.out.println("x=0 found");
				if(y==0) System.out.println("y=0 found");
				if(x<0) System.err.println("Too low value: "+x);
				if(y<0) System.err.println("Too low value: "+y);
				if(x==tileSizeCellNb-1) System.out.println("x=tileSizeCellNb-1 found");
				if(y==tileSizeCellNb-1) System.out.println("y=tileSizeCellNb-1 found");
				if(x>tileSizeCellNb-1) System.err.println("Too high value: "+x);
				if(y>tileSizeCellNb-1) System.err.println("Too high value: "+y);*/

				//store value
				Stat s_ = new Stat(s.value, "x", ""+(int)x, "y", ""+(int)y);
				sht.stats.add(s_);
			}

			//save as csv file
			//TODO be sure order is x,y,val
			//TODO handle case of more columns, when using multidimensional stats
			CSV.save(sht, "val", folderPath + "/" +t.z+ "/" +t.x+ "/" +t.y+ ".csv");
		}
	}

}
