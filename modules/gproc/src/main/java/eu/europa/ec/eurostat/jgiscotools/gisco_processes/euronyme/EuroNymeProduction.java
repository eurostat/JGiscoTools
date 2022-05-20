/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.euronyme;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.quadtree.Quadtree;
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

	//TODO check gazeeter aswell ?

	public static void main(String[] args) {
		System.out.println("Start");

		//structure();

		//get input lables
		ArrayList<Feature> fs = GeoData.getFeatures(namesStruct);

		//initialise rmax
		for(Feature f : fs)
			f.setAttribute("rmax", Integer.MAX_VALUE);

		//the buffer distance around the label, in pixels
		double pixX = 20, pixY = 20;

		for(int res = 50; res<=50; res *= 1.5) {

			//extract only the labels that are visible for this resolution
			final int res_ = res;
			Predicate<Feature> pr = f -> { Integer rmax = (Integer) f.getAttribute("rmax"); return rmax > res_; };
			List<Feature> fs_ = fs.stream().filter(pr).collect(Collectors.toList());

			//compute label envelopes
			for(Feature f : fs_)
				f.setAttribute("gl", getLabelEnvelope(f, res));

			//make spatial index, with only the ones remaining as visible for res
			Quadtree index = new Quadtree();
			for(Feature f : fs_)
				index.insert((Envelope)f.getAttribute("gl"), f);

			//analyse labels one by one
			for(Feature f : fs_) {
				//System.out.println("----");

				Integer rmax = (Integer) f.getAttribute("rmax");
				if(rmax <= res) continue;

				//get envelope, enlarged
				Envelope env = (Envelope) f.getAttribute("gl");
				Envelope searchEnv = new Envelope(env);
				searchEnv.expandBy(pixX * res, pixY * res);

				//get other labels overlapping/nearby with index
				List<Feature> neigh = index.query(searchEnv);
				//refine list of neighboors
				Predicate<Feature> pr2 = f2 -> { return searchEnv.intersects((Envelope) f2.getAttribute("gl")); };
				neigh = neigh.stream().filter(pr2).collect(Collectors.toList());

				//in case no neighboor...
				if(neigh.size() == 1)
					continue;

				//get best label to keep
				Feature toKeep = getBestLabelToKeep(neigh);

				//set rmax of others, and remove them
				neigh.remove(toKeep);
				for(Feature f_ : neigh) {
					f_.setAttribute("rmax", res);
					index.remove((Envelope) f_.getAttribute("gl"), f_);
				}

			}

		}

		//remove "gl" attribute
		for(Feature f : fs) f.getAttributes().remove("gl");

		//save
		GeoData.save(fs, "/home/juju/Bureau/out.gpkg", CRSUtil.getETRS89_LAEA_CRS());


		//GeoData.save(getNameExtend(10), "/home/juju/Bureau/namesStruct_10.gpkg", CRSUtil.getETRS89_LAEA_CRS());
		//GeoData.save(getNameExtend(50), "/home/juju/Bureau/namesStruct_50.gpkg", CRSUtil.getETRS89_LAEA_CRS());
		//GeoData.save(getNameExtend(100), "/home/juju/Bureau/namesStruct_100.gpkg", CRSUtil.getETRS89_LAEA_CRS());
		//GeoData.save(getNameExtend(1000), "/home/juju/Bureau/namesStruct_1000.gpkg", CRSUtil.getETRS89_LAEA_CRS());

		System.out.println("End");
	}


	private static Feature getBestLabelToKeep(List<Feature> fs) {
		//TODO do better
		Feature fBest = fs.get(0);
		return fBest;
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
			for(Coordinate c : f_.getGeometry().getCoordinates()) { double z = c.x; c.x=c.y;c.y = z; }

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
			Envelope env = getLabelEnvelope(f, pixSize);
			f.setGeometry(JTSGeomUtil.getGeometry(env));
		}
		return fs;
	}


	private static Envelope getLabelEnvelope(Feature f, double pixSize) {
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
		//List<String> header = List.of("name", "lon", "lat", "font_size", "font_weight", "rmax", "pop");
		//CSVUtil.save(out, namesCSV, header);
		//ArrayList<Map<String, String>> names = CSVUtil.load(namesCSV);
	}




	/*/make agent toponymes
	ArrayList<AgentToponyme> agents = new ArrayList<>();
	for(Feature f :fs)
		agents.add(new AgentToponyme(f));
	//make engine
	Engine<AgentToponyme> e = new Engine<>(agents);
	//start
	e.activateQueue();*/

}
