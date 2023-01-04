/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gridProc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import eu.europa.ec.eurostat.jgiscotools.ParquetUtil;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.FileUtil;

/**
 * Utility to create tiles of gridded statistics.
 * 
 * @author Julien Gaffuri
 */
public class GridTiler2 {
	//private static Logger logger = LogManager.getLogger(GridTiler2.class.getName());

	public static void main(String[] args) {
		System.out.println("Start");

		Map<String, ColummCalculator> values = new HashMap<>();
		
		tile(
				"my description",
				values,
				new Coordinate(0,0),
				new Envelope(0, 100, 0, 100),
				1,
				10,
				"EPSG:3035",
				Format.CSV,
				null,
				"/home/juju/Bureau/ttt/"
				);

		System.out.println("End");
	}

	public enum Format {
		CSV,
		PARQUET
	}

	interface ColummCalculator {
		String getValue(double xG, double yG);
	}


	public static void tile(String description, Map<String, ColummCalculator> values, Coordinate originPoint, Envelope env, int resolution, int tileResolutionPix, String crs, Format format, CompressionCodecName comp, String folderPath) {

		//tile frame caracteristics
		double tileGeoSize = resolution * tileResolutionPix;
		int tileMinX = (int) Math.floor( (env.getMinX() - originPoint.x) / tileGeoSize );
		int tileMaxX = (int) Math.ceil( (env.getMaxX() - originPoint.x) / tileGeoSize );
		int tileMinY = (int) Math.floor( (env.getMinY() - originPoint.y) / tileGeoSize );
		int tileMaxY = (int) Math.ceil( (env.getMaxY() - originPoint.y) / tileGeoSize );

		//column labels
		Set<String> keys = values.keySet();
		Set<Entry<String, ColummCalculator>> es = values.entrySet();

		//scan tiles
		for(int tx = tileMinX; tx<tileMaxX; tx++)
			for(int ty = tileMinY; ty<tileMaxY; ty++) {
				//handle tile (tx,ty)

				//prepare tile cells
				ArrayList<Map<String, String>> cells = new ArrayList<>();

				for(int xc = 0; xc<tileResolutionPix; xc ++)
					for(int yc = 0; yc<tileResolutionPix; yc ++) {

						//make new cell
						HashMap<String, String> cell = null;

						//get values
						for(Entry<String,ColummCalculator> e : es) {
							double xG = originPoint.x + tx * tileGeoSize + xc*resolution;
							double yG = originPoint.y + ty * tileGeoSize + yc*resolution;
							String v = e.getValue().getValue(xG, yG);
							if(v==null) continue;
							if(cell == null) cell = makeCell(keys);
							cell.put(e.getKey(), v);
						}

						//no value found: skip
						if(cell == null) continue;

						cell.put("x", xc+"");
						cell.put("y", yc+"");

						cells.add(cell);
					}

				//if no cell within tile, skip
				if(cells.size() == 0) continue;

				//save tile

				if(format == Format.CSV) {				
					// sort cells by x and y
					Collections.sort(cells, new Comparator<Map<String, String>>() {
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
					new File(folderPath + "/" + tx + "/").mkdirs();
					CSVUtil.save(cells, folderPath + "/" + tx + "/" + ty + ".csv");
				}
				else if(format == Format.PARQUET) {

					//make dir
					String fp = folderPath + "/" + tx + "/";
					new File(fp).mkdirs();

					// save as csv file
					CSVUtil.save(cells, fp + ty + ".csv");

					//convert csv to parquet
					ParquetUtil.convertCSVToParquet(fp + ty + ".csv", fp, "a", comp.toString());

					//nename parquet file
					new File(fp+"a.parquet").renameTo(new File(fp+ty+".parquet"));

					//delete csv file
					new File(fp + ty + ".csv").delete();
				}
			}


		//save tiles info.json

		// build JSON object
		JSONObject json = new JSONObject();

		json.put("resolutionGeo", resolution);
		json.put("tileSizeCell", tileResolutionPix);
		json.put("crs", crs);
		json.put("format", format.toString());
		json.put("description", description);

		// origin point
		JSONObject op = new JSONObject();
		op.put("x", originPoint.x);
		op.put("y", originPoint.y);
		json.put("originPoint", op);

		// tiling bounding
		JSONObject bn = new JSONObject();
		bn.put("xMin", tileMinX);
		bn.put("xMax", tileMaxX);
		bn.put("yMin", tileMinY);
		bn.put("yMax", tileMaxY);
		json.put("tilingBounds", bn);

		JSONArray dims = new JSONArray();
		for (String key : keys) dims.put(key);
		json.put("dims", dims);

		// save
		try {
			File f = FileUtil.getFile(folderPath + "/info.json", true, true);
			BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
			bw.write(json.toString(3));
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}


	private static HashMap<String, String> makeCell(Set<String> keys) {
		HashMap<String, String> cell = new HashMap<String, String>();
		return cell;
	}

}
