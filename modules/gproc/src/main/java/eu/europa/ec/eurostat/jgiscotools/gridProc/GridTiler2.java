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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

	public enum Format {
		CSV,
		PARQUET
	}
	
	interface colummCalculator {
		String getValue(int x, int y);
	}


	public GridTiler2(Coordinate originPoint, Envelope env, int resolution, int tileResolutionPix, String crs, Format format, CompressionCodecName comp, String folderPath) {

		//tile frame caracteristics
		double tileGeoSize = resolution * tileResolutionPix;
		int tileMinX = (int) Math.floor( (env.getMinX() - originPoint.x) / tileGeoSize );
		int tileMaxX = (int) Math.ceil( (env.getMaxX() - originPoint.x) / tileGeoSize );
		int tileMinY = (int) Math.floor( (env.getMinY() - originPoint.y) / tileGeoSize );
		int tileMaxY = (int) Math.ceil( (env.getMaxY() - originPoint.y) / tileGeoSize );

		//scan tiles
		for(int tx = tileMinX; tx<tileMaxX; tx++)
			for(int ty = tileMinY; ty<tileMaxY; ty++) {
				//handle tile (tx,ty)

				//prepare tile cells
				ArrayList<Map<String, String>> cells = new ArrayList<>();

				for(int xc = 0; xc<tileResolutionPix; xc ++)
					for(int yc = 0; yc<tileResolutionPix; yc ++) {

						//make new cell
						HashMap<String, String> cell = new HashMap<>();

						cell.put("x", xc+"");
						cell.put("y", yc+"");

						//get values
						//TODO

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
		bn.put("xMin", (int) tileMinX);
		bn.put("xMax", (int) tileMaxX);
		bn.put("yMin", (int) tileMinY);
		bn.put("yMax", (int) tileMaxY);
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

}
