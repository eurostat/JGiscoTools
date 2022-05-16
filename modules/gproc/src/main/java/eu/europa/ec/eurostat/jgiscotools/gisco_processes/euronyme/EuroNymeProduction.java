/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.euronyme;

import java.util.ArrayList;
import java.util.Collection;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.JTSGeomUtil;
import eu.europa.ec.eurostat.jgiscotools.io.geo.CRSUtil;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;
import eu.europa.ec.eurostat.jgiscotools.util.Util;

/**
 * @author julien Gaffuri
 *
 */
public class EuroNymeProduction {

	private static String namesStruct = "/home/juju/Bureau/namesStruct.gpkg";

	public static void main(String[] args) {
		System.out.println("Start");

		structure();

		//ArrayList<Feature> fs = GeoData.getFeatures(namesStruct);
		/*for(double res = 100; res<100000; res *= 1.5) {
			System.out.println(res);
		}*/

		//GeoData.save(getNameExtend(100), "/home/juju/Bureau/namesStruct_100.gpkg", CRSUtil.getWGS_84_CRS());
		//GeoData.save(getNameExtend(1000), "/home/juju/Bureau/namesStruct_1000.gpkg", CRSUtil.getWGS_84_CRS());
		//GeoData.save(getNameExtend(10000), "/home/juju/Bureau/namesStruct_10000.gpkg", CRSUtil.getWGS_84_CRS());

		System.out.println("End");
	}


	private static void structure() {
		//load input data
		String erm = "/home/juju/Bureau/gisco/geodata/euro-regional-map-gpkg/data/OpenEuroRegionalMap.gpkg";

		//BuiltupP
		//NAMN1/NAMN2 PPL/PP1-PP2 population
		ArrayList<Feature> buP = GeoData.getFeatures(erm, "BuiltupP", "id");
		System.out.println(buP.size() + " features loaded");
		CoordinateReferenceSystem crsERM = GeoData.getCRS(erm);
		//ArrayList<Feature> buA = GeoData.getFeatures(erm, "BuiltupA", "id");
		//System.out.println(buA.size() + " features loaded");

		Collection<Feature> out = new ArrayList<>();
		for(Feature f : buP) {
			Feature f_ = new Feature();

			//name
			//NAMN1 NAMN2
			String name = (String) f.getAttribute("NAMN1");
			if(name == null) {
				System.out.println("No NAMN1 for "+f.getID());
				name = (String) f.getAttribute("NAMN2");
				if(name == null) {
					System.out.println("No NAMN1 for "+f.getID());
					continue;
				}
			}
			f_.setAttribute("name", name);

			//lon / lat
			Point g = (Point) f.getGeometry();
			f_.setAttribute("lon", Double.toString(Util.round(g.getCoordinate().x, 3)));
			f_.setAttribute("lat", Double.toString(Util.round(g.getCoordinate().y, 3)));

			//geometry
			//project
			f_.setGeometry(CRSUtil.toLAEA(f.getGeometry(), crsERM));

			//population
			//PPL PP1 PP2
			Integer pop = (Integer) f.getAttribute("PPL");
			if(pop<0 || pop == null ) {
				Integer pop1 = (Integer) f.getAttribute("PP1");
				Integer pop2 = (Integer) f.getAttribute("PP2");
				if(pop1 >= 0 && pop2 >= 0 ) {
					pop = pop1 + (pop2-pop1)/3;
				} else if(pop1 < 0 && pop2 >= 0 ) {
					//System.out.println("pop2 " + pop2+name + " "+pop1);
					pop = pop2/2;
				} else if(pop1 >= 0 && pop2 < 0 ) {
					//System.out.println("pop1 " + pop1+name + " "+pop2);
					pop = pop1*2;
				} else if(pop1 < 0 && pop2 < 0 ) {
					//System.out.println(pop1+"   "+pop2);
					//TODO
					pop = 0;
				}
			}
			f_.setAttribute("pop", pop.toString());

			f_.setAttribute("font_size", "12");
			f_.setAttribute("font_weight", "");
			f_.setAttribute("rmin", "");
			f_.setAttribute("rmax", "");

			out.add(f_);
		}

		//save output
		GeoData.save(out, namesStruct, CRSUtil.getETRS89_LAEA_CRS());


		/*
		//index buP by "PopulatedPlaceID"
		HashMap<String,Feature> buPI = new HashMap<>();
		for(Feature fp : buP) {
			String id = (String)fp.getAttribute("PopulatedPlaceID");
			if(id == null) {System.err.println("No PopulatedPlaceID found"); continue; }
			Feature f = buPI.get(id);
			if(f!=null) {
				System.err.println("Already a buP with PopulatedPlaceID = "+id);
				System.out.println(f.getAttribute("NAMA1"));
				System.out.println(fp.getAttribute("NAMA1"));
				continue;
			}
			buPI.put(id,fp);
			//TODO check they are inside ?
		}

		//make areas from points
		ArrayList<Feature> areas = new ArrayList<Feature>();
		for(Feature fa : buA) {
			String id = (String)fa.getAttribute("PopulatedPlaceID");
			//if(id==null) { System.err.println("No PopulatedPlaceID for buA"); continue; }
			//System.out.println(id);

		}*/




		//ArrayList<Feature> name = GeoData.getFeatures(erm, "EBM_NAM", "id");
		//System.out.println(name.size() + " features loaded");
		//index EBM
		//id: SHN attribute
		//NAMN
		//PPL - population
		//ARA - area

	}


	private static ArrayList<Feature> getNameExtend(double pixSize) {
		ArrayList<Feature> fs = GeoData.getFeatures(namesStruct);
		for(Feature f : fs) {
			Envelope env = getNameRectangle(f, pixSize);
			f.setGeometry(JTSGeomUtil.getGeometry(env));
		}
		return fs;
	}


	private static Envelope getNameRectangle(Feature f, double pixSize) {
		Coordinate c = f.getGeometry().getCoordinate();
		double x1 = c.x;
		double y1 = c.y;

		//12pt = 16px
		String fs_ = (String) f.getAttribute("font_size");
		//12 as a default
		if(fs_ == null || fs_.length()==0) fs_ = "12";
		double fs = Integer.parseInt(fs_);
		double h = pixSize * fs * 16/12;
		double w = h * ((String)f.getAttribute("name")).length();

		return new Envelope(x1, x1+w, y1, y1+h);
	}


	private static void csvExport() {
		//private static String namesCSV = "/home/juju/Bureau/names2.csv";
		//List<String> header = List.of("name", "lon", "lat", "font_size", "font_weight", "rmin", "rmax", "pop");
		//CSVUtil.save(out, namesCSV, header);
		//ArrayList<Map<String, String>> names = CSVUtil.load(namesCSV);
	}

}
