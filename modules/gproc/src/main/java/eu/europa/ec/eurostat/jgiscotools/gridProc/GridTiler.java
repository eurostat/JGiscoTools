/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gridProc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericData.Record;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import eu.europa.ec.eurostat.java4eurostat.util.StatsUtil;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.ParquetUtil;
import eu.europa.ec.eurostat.jgiscotools.grid.GridCell;
import eu.europa.ec.eurostat.jgiscotools.gridProc.GridTiler.TilingInfo.DimStat;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.FileUtil;

/**
 * Utility to create tiles of gridded statistics.
 * 
 * @author Julien Gaffuri
 */
public class GridTiler {
	private static Logger logger = LogManager.getLogger(GridTiler.class.getName());

	public enum Format {
		CSV,
		PARQUET
	}

	/** The cells to tile */
	private List<Map<String, String>> cells;
	/** The name of the attribute with the grid id */
	private String gridIdAtt = "GRD_ID";

	/**
	 * The position of origin of the grid to take into account to define the
	 * tiling frame. It should be the bottom left corner of the tiling frame. Tiling
	 * numbering goes from left to right, and from bottom to top. For LAEA, take
	 * (0,0).
	 */
	private Coordinate originPoint = new Coordinate(0, 0);

	/**
	 * The tile resolution, in number of grid cells.
	 */
	private int tileResolutionPix = 256;

	/**
	 * The computed tiles.
	 */
	private Collection<GridStatTile> tiles;

	public Collection<GridStatTile> getTiles() {
		return tiles;
	}

	private class GridStatTile {
		public int x, y;
		public ArrayList<Map<String, String>> cells = new ArrayList<Map<String, String>>();

		GridStatTile(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	public GridTiler(String csvFilePath, String gridIdAtt, Coordinate originPoint, int tileResolutionPix) {
		this(CSVUtil.load(csvFilePath), gridIdAtt, originPoint, tileResolutionPix);
	}

	public GridTiler(List<Map<String, String>> cells, String gridIdAtt, Coordinate originPoint,
			int tileResolutionPix) {
		this.cells = cells;
		this.gridIdAtt = gridIdAtt;
		this.originPoint = originPoint;
		this.tileResolutionPix = tileResolutionPix;
	}

	/**
	 * Build the tiles for several tile sizes.
	 */
	public void createTiles() {

		// create tile dictionary tileId -> tile
		HashMap<String, GridStatTile> tiles_ = new HashMap<String, GridStatTile>();

		// go through cell stats and assign it to a tile
		for (Map<String, String> c : this.cells) {

			// get cell information
			String gridId = c.get(gridIdAtt);
			GridCell cell = new GridCell(gridId);
			double x = cell.getLowerLeftCornerPositionX();
			double y = cell.getLowerLeftCornerPositionY();
			int resolution = cell.getResolution();

			// compute tile size, in geo unit
			int tileSizeM = resolution * this.tileResolutionPix;

			// find tile position
			int xt = (int) ((x - originPoint.x) / tileSizeM);
			int yt = (int) ((y - originPoint.y) / tileSizeM);

			// get tile. If it does not exists, create it.
			String tileId = xt + "_" + yt;
			GridStatTile tile = tiles_.get(tileId);
			if (tile == null) {
				tile = new GridStatTile(xt, yt);
				tiles_.put(tileId, tile);
			}

			// add cell to tile
			tile.cells.add(c);
		}

		/*
		 * if(createEmptyTiles) { Envelope bn = getTilesInfo().tilingBounds; for(int
		 * xt=(int)bn.getMinX(); xt<=bn.getMaxX(); xt++) { for(int yt=(int)bn.getMinY();
		 * yt<=bn.getMaxY(); yt++) { String tileId = xt+"_"+yt; GridStatTile tile =
		 * tiles_.get(tileId); if(tile == null) { tile = new GridStatTile(xt, yt);
		 * tiles_.put(tileId, tile); } } } }
		 */

		tiles = tiles_.values();
		tilesInfo = null;
	}

	/**
	 * Save the tile as CSV.
	 * 
	 * @param folderPath
	 * @param format
	 * @param schemaJson
	 */
	public void save(String folderPath, Format format, String schemaJson) {

		List<String> cols = null;
		Schema schema = null;
		if(format == Format.CSV) {
			// prepare list of columns, ordered
			cols = new ArrayList<>(this.getTiles().iterator().next().cells.get(0).keySet());
			cols.add("x");
			cols.add("y");
			cols.remove(this.gridIdAtt);
			Comparator<String> cp = new Comparator<String>() {
				@Override
				public int compare(String s1, String s2) {
					if (s1.equals(s2))
						return 0;
					if (s1.equals("x"))
						return -1;
					if (s2.equals("x"))
						return 1;
					if (s1.equals("y"))
						return -1;
					if (s2.equals("y"))
						return 1;
					return s1.compareTo(s2);
				}
			};
			cols.sort(cp);
		}
		else if(format == Format.CSV) {
			schema = ParquetUtil.parseSchema(schemaJson);
		}

		// save tiles
		for (GridStatTile t : tiles) {

			// the output cells
			ArrayList<Map<String, String>> cells_ = new ArrayList<Map<String, String>>();

			// prepare tile cells for export
			for (Map<String, String> c : t.cells) {

				// new cell
				HashMap<String, String> c_ = new HashMap<String, String>();
				// copy without grid id
				c_.putAll(c);
				c_.remove(this.gridIdAtt);

				// get cell position
				GridCell cell = new GridCell(c.get(gridIdAtt));
				double x = cell.getLowerLeftCornerPositionX() - originPoint.x;
				double y = cell.getLowerLeftCornerPositionY() - originPoint.y;
				double r = cell.getResolution();

				// compute cell position in tile space
				x = x / r - t.x * tileResolutionPix;
				y = y / r - t.y * tileResolutionPix;

				// check x,y values. Should be within [0,tileResolutionPix-1]
				if (x < 0)
					logger.error("Too low value: " + x + " <0");
				if (y < 0)
					logger.error("Too low value: " + y + " <0");
				if (x > this.tileResolutionPix - 1)
					logger.error("Too high value: " + x + " >"+(this.tileResolutionPix - 1));
				if (y > this.tileResolutionPix - 1)
					logger.error("Too high value: " + y + " >"+(this.tileResolutionPix - 1));

				// store x,y values
				c_.put("x", "" + (int) x);
				c_.put("y", "" + (int) y);

				// keep
				cells_.add(c_);
			}

			if(format == Format.CSV) {				
				// sort cells by x and y
				Collections.sort(cells_, new Comparator<Map<String, String>>() {
					@Override
					public int compare(Map<String, String> s1, Map<String, String> s2) {
						if (Integer.parseInt(s1.get("x")) < Integer.parseInt(s2.get("x")))
							return -1;
						if (Integer.parseInt(s1.get("x")) > Integer.parseInt(s2.get("x")))
							return 1;
						if (Integer.parseInt(s1.get("y")) < Integer.parseInt(s2.get("y")))
							return -1;
						if (Integer.parseInt(s1.get("y")) > Integer.parseInt(s2.get("y")))
							return 1;
						return 0;
					}
				});

				// save as csv file
				new File(folderPath + "/" + t.x + "/").mkdirs();
				CSVUtil.save(cells_, folderPath + "/" + t.x + "/" + t.y + ".csv", cols);
			}
			else if(format == Format.PARQUET) {
				List<Record> recs = new ArrayList<>();

				int i=0;
				Set<String> keys = cells_.get(0).keySet();
				for(Map<String, String> c : cells_) {
					GenericData.Record record = new GenericData.Record(schema);
					record.put("id", i++);
					for(String key : keys) {
						//TODO type
						record.put(key, c.get(key));
					}
					recs.add(record);
				}

				
				// save as parquet file
				new File(folderPath + "/" + t.x + "/").mkdirs();
				ParquetUtil.save(folderPath + "/" + t.x + "/" + t.y + ".parquet", schema, recs);
			}

		}
	}

	private TilingInfo tilesInfo = null;

	public TilingInfo getTilesInfo() {
		if (tilesInfo == null)
			computeTilesInfo();
		return tilesInfo;
	}

	public static class TilingInfo {
		Envelope tilingBounds = null;
		public int resolution = -1;
		public String ePSGCode;
		public ArrayList<DimStat> dSt = new ArrayList<>();

		public static class DimStat {
			public String dimValue;
			public double minValue = Double.MAX_VALUE, maxValue = -Double.MAX_VALUE;
			public double[] percentiles;
			public double averageValue;
		}
	}

	private TilingInfo computeTilesInfo() {
		tilesInfo = new TilingInfo();

		for (GridStatTile t : getTiles()) {
			// set x/y envelope
			if (tilesInfo.tilingBounds == null)
				tilesInfo.tilingBounds = new Envelope(new Coordinate(t.x, t.y));
			else
				tilesInfo.tilingBounds.expandToInclude(t.x, t.y);

			// set resolution and CRS
			if (tilesInfo.resolution == -1 && t.cells.size() > 0) {
				GridCell cell = new GridCell(t.cells.get(0).get(gridIdAtt));
				tilesInfo.resolution = cell.getResolution();
				tilesInfo.ePSGCode = cell.getEpsgCode();
			}

		}

		// get value columns
		Set<String> keys = this.cells.get(0).keySet();
		keys.remove(this.gridIdAtt);

		// get all values, indexed by column
		HashMap<String, Collection<Double>> vals = new HashMap<>();
		for (String key : keys) {
			if (key.equals(this.gridIdAtt))
				continue;
			vals.put(key, new ArrayList<>());
		}
		for (Map<String, String> c : this.cells)
			for (String key : keys) {
				try {
					Double d = Double.parseDouble(c.get(key));
					vals.get(key).add(d);
				} catch (NumberFormatException e) {
				}
			}

		// compute stats
		for (String key : keys)
			tilesInfo.dSt.add(getStats(key, vals.get(key)));

		return tilesInfo;
	}

	private DimStat getStats(String dimValue, Collection<Double> vals) {
		DimStat ds = new DimStat();
		ds.dimValue = dimValue;

		// percentiles
		ds.percentiles = StatsUtil.getQuantiles(vals, 99);

		// average
		ds.averageValue = 0;
		for (double v : vals)
			ds.averageValue += v;
		ds.averageValue /= vals.size();

		// max and min
		ds.maxValue = Double.NEGATIVE_INFINITY;
		ds.minValue = Double.POSITIVE_INFINITY;
		for (double v : vals) {
			if (v > ds.maxValue)
				ds.maxValue = v;
			if (v < ds.minValue)
				ds.minValue = v;
		}
		return ds;
	}

	/**
	 * Save the tiling info.json file
	 * 
	 * @param outpath
	 * @param format
	 * @param description
	 */
	public void saveTilingInfoJSON(String outpath, Format format, String description) {
		TilingInfo ti = getTilesInfo();

		// build JSON object
		JSONObject json = new JSONObject();

		json.put("resolutionGeo", ti.resolution);
		json.put("tileSizeCell", this.tileResolutionPix);
		json.put("crs", ti.ePSGCode);
		json.put("format", format.toString());

		// origin point
		JSONObject op = new JSONObject();
		op.put("x", this.originPoint.x);
		op.put("y", this.originPoint.y);
		json.put("originPoint", op);

		// tiling bounding
		JSONObject bn = new JSONObject();
		bn.put("xMin", (int) ti.tilingBounds.getMinX());
		bn.put("xMax", (int) ti.tilingBounds.getMaxX());
		bn.put("yMin", (int) ti.tilingBounds.getMinY());
		bn.put("yMax", (int) ti.tilingBounds.getMaxY());
		json.put("tilingBounds", bn);


		JSONArray dims = new JSONArray();
		for (DimStat ds : ti.dSt) dims.put(ds.dimValue);

		// data on dimensions
		/*JSONObject dims = new JSONObject();
		for (DimStat ds : ti.dSt) {
			JSONObject ds_ = new JSONObject();

			//ds_.put("minValue", ds.minValue);
			//ds_.put("maxValue", ds.maxValue);
			//ds_.put("averageValue", ds.averageValue);
			JSONArray p = new JSONArray();
			for (double v : ds.percentiles)
				p.put(v);
			ds_.put("percentiles", p);
			dims.put(ds.dimValue, ds_);
		}*/
		json.put("dims", dims);

		// save
		try {
			File f = FileUtil.getFile(outpath + "/info.json", true, true);
			BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
			bw.write(json.toString(3));
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
