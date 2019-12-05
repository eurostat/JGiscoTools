/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.grid;

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
 * 
 * @author Julien Gaffuri
 *
 */
public class GriddedStatsTiler {

	/**
	 * The statistical figures to tile
	 */
	private StatsHypercube sh;

	/**
	 * The name of the attribute with the grid id
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
		public int x,y,s;
		public ArrayList<Stat> stats = new ArrayList<Stat>();
		GridStatTile(int x, int y, int s) { this.x=x; this.y=y; this.s=s; }
	}


	public GriddedStatsTiler(String csvFilePath, String statAttr) {
		this( CSV.load(csvFilePath, statAttr) );
	}

	public GriddedStatsTiler(StatsHypercube sh) {
		this.sh = sh;
	}


	/**
	 * Build the tiles for tile sizes from 2^5 to 2^8.
	 */
	public void createTiles() { createTiles(5,8); }

	/**
	 * Build the tiles for several tile sizes.
	 * 
	 * @param minPowTwo
	 * @param maxPowTwo
	 */
	public void createTiles(int minPowTwo, int maxPowTwo) {
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

			for(int pow=minPowTwo; pow<=maxPowTwo; pow++) {
				//get id of the tile the cell should belong to

				//compute tile size
				int tileSizePix = (int)Math.pow(2, pow);
				int tileSizeM = resolution * tileSizePix;

				//find tile position
				int xt = (int)( (x-originPoint.x)/tileSizeM );
				int yt = (int)( (y-originPoint.y)/tileSizeM );

				//get tile. If it does not exists, create it.
				String tileId = tileSizePix+"_"+xt+"_"+yt;
				GridStatTile tile = tiles_.get(tileId);
				if(tile == null) {
					tile = new GridStatTile(xt, yt, tileSizePix);
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

		for(GridStatTile t : tiles) {
			//compute tile size
			//int ts = getTileSizeCellNb(t.z);
			int ts = t.s;

			//build sh for the tile
			StatsHypercube sht = new StatsHypercube(sh.getDimLabels());
			sht.dimLabels.add("x");
			sht.dimLabels.add("y");
			sht.dimLabels.remove(gridIdAtt);

			//prepare tile stats for export
			for(Stat s : t.stats) {

				//get cell position
				GridCell cell = new GridCell( s.dims.get(gridIdAtt) );
				double x = cell.getLowerLeftCornerPositionX() - originPoint.x;
				double y = cell.getLowerLeftCornerPositionY() - originPoint.y;
				double r = cell.getResolution();

				//compute cell position in tile space
				x = x/r - ts*t.x;
				y = y/r - ts*t.y;

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
			//TODO add json with service information
			CSV.save(sht, "val", folderPath + "/" +t.s+ "/" +t.x+ "/" +t.y+ ".csv");
		}
	}

}
