/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.graph;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.graph.structure.Graph;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

/**
 * @author julien Gaffuri
 *
 */
public class GraphIOUtil {

	public static void exportAsSHP(Graph g, String outPath, String format, CoordinateReferenceSystem crs){
		GraphIOUtil.exportFaces(g, outPath+"faces."+format, crs);
		GraphIOUtil.exportEdges(g, outPath+"edges."+format, crs);
		GraphIOUtil.exportNodes(g, outPath+"nodes."+format, crs);
	}

	public static void exportFaces(Graph g, String outFile, CoordinateReferenceSystem crs){
		GeoData.save(GraphToFeature.asFeature(g.getFaces()), outFile, crs);
	}

	public static void exportEdges(Graph g, String outFile, CoordinateReferenceSystem crs){
		GeoData.save(GraphToFeature.asFeature(g.getEdges()), outFile, crs);
	}

	public static void exportNodes(Graph g, String outFile, CoordinateReferenceSystem crs){
		GeoData.save(GraphToFeature.asFeature(g.getNodes()), outFile, crs);
	}

	/*
	DefaultFeatureCollection fs;
	ShapeFile shp;

	//save nodes as shp file
	shp = new ShapeFile("Point", 3035, "", outPath , "nodes.shp", true,true,true);
	fs = new DefaultFeatureCollection(null, shp.getSchema());
	for(Node n : graph.getNodes())
		fs.add(shp.buildFeature(n.getGeometry()));
	shp.add(fs);


	shp = new ShapeFile("LineString", 3035, "", outPath, "edges.shp", true,true,true);
	fs = new DefaultFeatureCollection(null, shp.getSchema());
	for(Edge e : graph.getEdges())
		fs.add(shp.buildFeature(e.getGeometry()));
	shp.add(fs);

	//save faces as shp file
	shp = new ShapeFile("Polygon", 3035, "", outPath, "faces.shp", true,true,true);
	fs = new DefaultFeatureCollection(null, shp.getSchema());
	for(Face d : graph.getFaces())
		fs.add(shp.buildFeature(d.getGeometry()));
	shp.add(fs);*/

}
