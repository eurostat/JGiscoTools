/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.io.web;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Map.Entry;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import eu.europa.ec.eurostat.jgiscotools.feature.SimpleFeatureUtil;

/**
 * @author julien Gaffuri
 *
 */
public class ArcGISRest {


	public static void download(String urlBase, String file, int layerid, String format) {
		HTTPUtil.downloadFromURL(urlBase + layerid + "/query?where=1%3D1&outFields=*&f=" + format, file);
	}
	public static void download(String urlBase, String file, int layerid) {
		download(urlBase, file, layerid, "json");
	}



	public static void download2(String urlBase, String file, int layerid, String objIdAttribute, String format) {
		new File(file).delete();

		int i=0;
		int nbTry=0;
		int nbTryLimit = 100;
		while(nbTry<nbTryLimit){
			String url = urlBase + "find?searchText="+i+"&searchFields="+objIdAttribute+"&layers="+layerid +"&f="+format;
			//System.out.println(url);
			i++;

			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
				String line = in.readLine();
				//System.out.println(line);

				if(line == null || "".equals(line) || "{\"results\":[]}".equals(line)){
					System.out.println("No data for OBJECTID="+(i-1));
					nbTry++;
					continue;
				}

				//System.out.println(line);
				nbTry = 0;

				//append to file
				try {
					PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
					out.println(line);
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			} catch (Exception e) {
				nbTry++;
			}
		}

	}

	public static void download2(String urlBase, String file, int layerid, String objIdAttribute) {
		download2(urlBase, file, layerid, objIdAttribute, "json");
	}

	public static void download2(String urlBase, String file, int layerid) {
		download2(urlBase, file, layerid, "OBJECTID");
	}



	public static void toSHP(String dataFile, String shpFile) {
		try {
			//read file
			String jsoStr = "";
			BufferedReader reader = new BufferedReader(new FileReader(new File(dataFile)));
			String line=null;
			while ((line = reader.readLine()) != null) jsoStr += line;
			reader.close();

			//parse json
			JSONObject jso = (JSONObject)new JSONParser().parse(jsoStr);
			jsoStr=null;

			//get geometry type
			String geomType = (String)jso.get("geometryType");
			if("esriGeometryPoint".equals(geomType)) geomType = "Point";
			else if("esriGeometryPolygon".equals(geomType)) geomType = "Polygon";
			else if("esriGeometryMultipoint".equals(geomType)) geomType = "MultiPoint";
			else if("esriGeometryMultilinestring".equals(geomType)) geomType = "MultiLineString";
			else if("esriGeometryMultipolygon".equals(geomType)) geomType = "MultiPolygon";
			else{
				System.err.println("Non supported geometry type in ArcGISRest to SHP converter: "+geomType);
				return;
			}
			//System.out.println(geomType);

			//get srid
			long srid = Long.parseLong(((JSONObject)jso.get("spatialReference")).get("latestWkid").toString()); //"4326";
			//System.out.println(srid);

			//build feature type
			String attSt = "";
			boolean first = true;
			JSONArray fields = (JSONArray)jso.get("fields");
			for(int i=0; i<fields.size(); i++){
				JSONObject field = (JSONObject)fields.get(i);

				String name = (String) field.get("name");
				if("Shape".equals(name)) continue;
				if("OBJECTID".equals(name)) continue;

				String type = (String) field.get("type");
				if("esriFieldTypeOID".equals(type)) type="String";
				else if("esriFieldTypeString".equals(type)) type="String";
				else if("esriFieldTypeDouble".equals(type)) type="Double";
				else if("esriFieldTypeInteger".equals(type)) type="Integer";
				else if("esriFieldTypeSmallInteger".equals(type)) type="Integer";
				else{
					System.err.println("Non supported field type in ArcGISRest to SHP converter: "+type);
					return;
				}
				//String alias = (String) field.get("alias");

				if(first) first=false; else attSt+=",";
				attSt += name + ":" + type;
			}
			SimpleFeatureType ft = SimpleFeatureUtil.getFeatureType(geomType, (int)srid, attSt);
			SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(ft);

			//build features collection
			JSONArray features = (JSONArray)jso.get("features");
			DefaultFeatureCollection fs = new DefaultFeatureCollection(null, ft);
			for(int i=0; i<features.size(); i++){
				JSONObject feature = (JSONObject)features.get(i);
				JSONObject att = (JSONObject) feature.get("attributes");
				JSONObject geom = (JSONObject) feature.get("geometry");

				//build feature
				Geometry g = getGeometry(geomType, geom);
				SimpleFeature f = sfb.buildFeature(""+i, new Object[]{g});
				for(Object obj : att.entrySet()){
					String k = (String)((Entry<?,?>)obj).getKey();
					if("Shape".equals(k)) continue;
					if("OBJECTID".equals(k)) continue;
					f.setAttribute(k, ((Entry<?,?>)obj).getValue());
				}
				fs.add(f);
			}

			System.out.println("Save...");
			SHPUtil.saveSHP(fs, shpFile);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void toSHP2(String dataFile, String shpFile) {
		try {
			SimpleFeatureType ft = null;
			SimpleFeatureBuilder sfb = null;

			DefaultFeatureCollection fs = null;
			BufferedReader reader = new BufferedReader(new FileReader(new File(dataFile)));
			String line=null;
			int id=0;
			while ((line = reader.readLine()) != null) {
				JSONObject jso = (JSONObject)new JSONParser().parse(line);
				JSONObject thing = (JSONObject)((JSONArray) jso.get("results")).iterator().next();
				//{"attributes":{"Shape":"Multipoint","NI":"0.26","THICKNESS":"0","FE":"10.8","CU":"0.07","MN":"11","CDRC_SEQUE":"2960 2350","CO":"0.44","DEPTH":"770","LONGITUDE":"178.2","LATITUDE":"-11.317","OBJECTID":"1211"}
				//,"geometry":{"points":[[178.2000000001,-11.3170000004495]],"spatialReference":{"wkid":4326}}}

				JSONObject att = (JSONObject) thing.get("attributes");
				JSONObject geom = (JSONObject) thing.get("geometry");
				String geomType = (String)att.get("Shape");

				if(id==0){
					//get srid
					long srid = Long.parseLong(((JSONObject)geom.get("spatialReference")).get("wkid").toString()); //"4326";

					String attSt = "";
					boolean first = true;
					for(Object obj : att.entrySet()){
						String k = ((Entry<?,?>)obj).getKey().toString();
						if("Shape".equals(k)) continue;
						if("OBJECTID".equals(k)) continue;
						if(first) first=false; else attSt+=",";
						attSt+=k;
					}

					String geomType_ = geomType;
					if("Multipoint".equals(geomType_)) geomType_ = "MultiPoint";
					else if("Multilinestring".equals(geomType_)) geomType_ = "MultiLineString";
					else if("Multipolygon".equals(geomType_)) geomType_ = "MultiPolygon";

					ft = SimpleFeatureUtil.getFeatureType(geomType_, (int)srid, attSt);
					sfb = new SimpleFeatureBuilder(ft);
				}

				//build object
				Geometry g = getGeometry(geomType, geom);
				SimpleFeature f = sfb.buildFeature(""+(id++), new Object[]{g});
				for(Object obj : att.entrySet()){
					String k = (String)((Entry<?,?>)obj).getKey();
					if("Shape".equals(k)) continue;
					if("OBJECTID".equals(k)) continue;
					f.setAttribute(k, ((Entry<?,?>)obj).getValue());
				}

				if(fs == null) fs = new DefaultFeatureCollection(null, ft);
				fs.add(f);
			}
			reader.close();

			System.out.println("Save...");
			SHPUtil.saveSHP(fs, shpFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static Geometry getGeometry(String geomType, JSONObject geom) {
		if("Point".equals(geomType)){
			Coordinate c = new Coordinate(((Number)geom.get("x")).doubleValue(), ((Number)geom.get("y")).doubleValue());
			return new GeometryFactory().createPoint(c );
		} else if("MultiPoint".equals(geomType) || "Multipoint".equals(geomType)){
			//{"points":[[-78.1716999999549,31.0750000000501]],"spatialReference":{"wkid":4326}}
			JSONArray pts = (JSONArray) geom.get("points");
			Coordinate[] cs = new Coordinate[pts.size()];
			for(int i=0;i<pts.size();i++) {
				JSONArray pt = (JSONArray)pts.get(i);
				cs[i] = new Coordinate(((Number)pt.get(0)).doubleValue(), ((Number)pt.get(1)).doubleValue());
			}
			return new GeometryFactory().createMultiPointFromCoords(cs);
		} else if("Polygon".equals(geomType)){
			//System.out.println(geom.keySet());
			//JSONArray rings = (JSONArray) geom.get("rings");
			LinearRing[] holes = null;
			LinearRing shell = null;
			//System.out.println(rings.size());
			return new GeometryFactory().createPolygon(shell, holes);
		} else {
			System.err.println("Non supported geometry type in ArcGISRest to SHP converter: "+geomType);
			//System.err.println("   "+geom);
		}
		return null;
	}


	public static void main(String[] args) {
		System.out.println("Start");
		//download("http://maratlas.discomap.eea.europa.eu/arcgis/rest/services/Maratlas/AccidentDensity/MapServer/", "E:/gaffuju/Desktop/test.json", 0, "json");
		toSHP("E:/gaffuju/Desktop/test.json", "E:/gaffuju/Desktop/test.shp");
		System.out.println("Done");
	}

}
