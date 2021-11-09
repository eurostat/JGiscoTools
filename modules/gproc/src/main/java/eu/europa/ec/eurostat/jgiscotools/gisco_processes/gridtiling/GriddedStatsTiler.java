/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridtiling;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import eu.europa.ec.eurostat.java4eurostat.base.Stat;
import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.io.CSV;
import eu.europa.ec.eurostat.java4eurostat.util.StatsUtil;
import eu.europa.ec.eurostat.jgiscotools.grid.GridCell;
import eu.europa.ec.eurostat.jgiscotools.io.FileUtil;

/**
 * 
 * Utility to create tiles of gridded statistics.
 * 
 * @author Julien Gaffuri
 *
 */
public class GriddedStatsTiler {

	/** The statistical figures to tile */
	private StatsHypercube sh;

	/** In case several values are provided, the dimension label where to find them. */
	private String dimLabel = null;

	private String noValue = "";

	/** The name of the attribute with the grid id */
	private String gridIdAtt = "GRD_ID";

	/**
	 * The position of origin of the grid to take into account to defining the tiling frame.
	 * It should be the bottom left corner of the tiling frame.
	 * Tiling numbering goes from left to right, and from bottom to top.
	 * For LAEA, take (0,0).
	 */
	private Coordinate originPoint = new Coordinate(0,0);


	/**
	 * The tile resolution, in number of grid cells.
	 */
	private int tileResolutionPix = 256;

	/**
	 * The computed tiles.
	 */
	private Collection<GridStatTile> tiles;

	public Collection<GridStatTile> getTiles() { return tiles; }

	private class GridStatTile {
		public int x,y;
		public ArrayList<Stat> stats = new ArrayList<Stat>();
		GridStatTile(int x, int y) { this.x=x; this.y=y; }
		public Stat getMaxValue() {
			Stat s_ = null;
			for(Stat s : stats) if (s_==null || s.value > s_.value) s_=s;
			return s_;
		}
		public Stat getMinValue() {
			Stat s_ = null;
			for(Stat s : stats) if (s_==null || s.value < s_.value) s_=s;
			return s_;
		}
	}

	public GriddedStatsTiler(int tileResolutionPix, String csvFilePath, String statAttr) {
		this( tileResolutionPix, CSV.load(csvFilePath, statAttr), null, "");
	}

	public GriddedStatsTiler(int tileResolutionPix, StatsHypercube sh, String dimLabel, String noValue) {
		this.tileResolutionPix = tileResolutionPix;
		this.sh = sh;
		this.dimLabel = dimLabel;
		this.noValue = noValue;
	}


	/**
	 * Build the tiles for several tile sizes.
	 */
	public void createTiles(boolean createEmptyTiles) {

		//create tile dictionary tileId -> tile
		HashMap<String,GridStatTile> tiles_ = new HashMap<String,GridStatTile>();

		//go through cell stats and assign it to a tile
		for(Stat s : sh.stats) {

			//get cell information
			String gridId = s.dims.get(gridIdAtt);
			GridCell cell = new GridCell(gridId);
			double x = cell.getLowerLeftCornerPositionX();
			double y = cell.getLowerLeftCornerPositionY();
			int resolution = cell.getResolution();

			//compute tile size, in geo unit
			int tileSizeM = resolution * this.tileResolutionPix;

			//find tile position
			int xt = (int)( (x-originPoint.x)/tileSizeM );
			int yt = (int)( (y-originPoint.y)/tileSizeM );

			//get tile. If it does not exists, create it.
			String tileId = xt+"_"+yt;
			GridStatTile tile = tiles_.get(tileId);
			if(tile == null) {
				tile = new GridStatTile(xt, yt);
				tiles_.put(tileId, tile);
			}

			//add cell to tile
			tile.stats.add(s);
		}

		tiles = tiles_.values();

		tilesInfo = null;
		if(createEmptyTiles) {
			Envelope bn = getTilesInfo().tilingBounds;
			for(int xt=(int)bn.getMinX(); xt<=bn.getMaxX(); xt++) {
				for(int yt=(int)bn.getMinY(); yt<=bn.getMaxY(); yt++) {
					String tileId = xt+"_"+yt;
					GridStatTile tile = tiles_.get(tileId);
					if(tile == null) {
						tile = new GridStatTile(xt, yt);
						tiles_.put(tileId, tile);
					}
				}
			}
		}

		tiles = tiles_.values();
	}


	/**
	 * Save the tile as CSV.
	 * 
	 * @param folderPath
	 */
	public void saveCSV(String folderPath) {

		for(GridStatTile t : tiles) {

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
				x = x/r - t.x*tileResolutionPix;
				y = y/r - t.y*tileResolutionPix;

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
				if(this.dimLabel != null) s_.dims.put(this.dimLabel, s.dims.get(this.dimLabel));
				sht.stats.add(s_);
			}

			//save as csv file
			Comparator<String> cp = new Comparator<>() {
				@Override
				public int compare(String s1, String s2) {
					if(s1.equals(s2)) return 0;
					if(s1.equals("x")) return -1;
					if(s2.equals("x")) return 1;
					if(s1.equals("y")) return -1;
					if(s2.equals("y")) return 1;
					System.out.println(s1+" "+s2+" "+s2.compareTo(s1));
					return s2.compareTo(s1);
				}
			};
			CSV.saveMultiValues(sht, folderPath + "/" +t.x+ "/" +t.y+ ".csv", ",", this.noValue, cp, "time");

		}
	}




	private TilingInfo tilesInfo = null;
	public TilingInfo getTilesInfo() {
		if (tilesInfo == null)
			computeTilesInfo();
		return tilesInfo;
	}

	public class TilingInfo {
		Envelope tilingBounds = null;
		public int resolution = -1;
		public String ePSGCode;
		public double minValue = Double.MAX_VALUE, maxValue = -Double.MAX_VALUE;
		public double[] percentiles;
		public double averageValue;
	}

	private TilingInfo computeTilesInfo() {
		tilesInfo = new TilingInfo();
		Collection<Double> vals = new ArrayList<>();

		for(GridStatTile t : getTiles()) {
			//set x/y envelope
			if(tilesInfo.tilingBounds==null) tilesInfo.tilingBounds = new Envelope(new Coordinate(t.x, t.y));
			else tilesInfo.tilingBounds.expandToInclude(t.x, t.y);

			//set resolution and CRS
			if(tilesInfo.resolution == -1 && t.stats.size()>0) {
				GridCell cell = new GridCell( t.stats.get(0).dims.get(gridIdAtt) );
				tilesInfo.resolution = cell.getResolution();
				tilesInfo.ePSGCode = cell.getEpsgCode();
			}

			//set min/max stat values
			if(t.stats.size()>0) {
				tilesInfo.maxValue = Math.max(t.getMaxValue().value, tilesInfo.maxValue);
				tilesInfo.minValue = Math.min(t.getMinValue().value, tilesInfo.minValue);
			}

			//store values
			for(Stat s : t.stats) vals.add(s.value);
		}

		tilesInfo.percentiles = StatsUtil.getQuantiles(vals, 99);

		//get average
		double sum = 0;
		for(double v : vals) sum += v;
		tilesInfo.averageValue = sum/vals.size();

		return tilesInfo;
	}

	/**
	 * Save the tiling info.json file
	 * 
	 * @param outpath
	 * @param description
	 */
	public void saveTilingInfoJSON(String outpath, String description) {
		TilingInfo ti = getTilesInfo();

		//build JSON object
		JSONObject json = new JSONObject();

		json.put("resolutionGeo", ti.resolution);
		json.put("tileSizeCell", this.tileResolutionPix);
		json.put("crs", ti.ePSGCode);
		//json.put("minValue", ti.minValue);
		//json.put("maxValue", ti.maxValue);
		//json.put("averageValue", ti.averageValue);

		JSONObject bn = new JSONObject();
		bn.put("minX", (int)ti.tilingBounds.getMinX());
		bn.put("maxX", (int)ti.tilingBounds.getMaxX());
		bn.put("minY", (int)ti.tilingBounds.getMinY());
		bn.put("maxY", (int)ti.tilingBounds.getMaxY());
		json.put("tilingBounds", bn);

		//JSONArray p = new JSONArray(); for(double v:ti.percentiles) p.put(v);
		//json.put("percentiles", p);

		//TODO add corner position
		//TODO add columns - and stat data for each of them

		//save
		try {
			File f = FileUtil.getFile(outpath+"/info.json", true, true);
			BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
			bw.write(json.toString());
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
