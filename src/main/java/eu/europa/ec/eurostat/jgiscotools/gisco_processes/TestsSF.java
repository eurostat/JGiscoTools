package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import java.util.ArrayList;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import eu.europa.ec.eurostat.jgiscotools.feature.SimpleFeatureUtil;

public class TestsSF {

	public static void main(String[] args) throws Throwable {

		//should we abandon Feature and adopt only SimpleFeature ?

		//create feature type
		SimpleFeatureType ft = SimpleFeatureUtil.getFeatureType("Point", 3035, "ex:String,nb:int");

		//build SF from feature type
		ArrayList<SimpleFeature> sfs = new ArrayList<SimpleFeature>();
		for(int i=1; i<=10; i++) {
			SimpleFeature sf = new SimpleFeatureBuilder(ft).buildFeature("ID_"+i);
			sf.setDefaultGeometry(new GeometryFactory().createPoint(new Coordinate(i,0)));
			sf.setAttribute("ex", "dgdfgfd_"+i);
			sf.setAttribute("nb", i*5);
			//sf.setAttribute("AAAAAAA", "dgddsg"); triggers error
			sfs.add(sf);
		}

		//for(SimpleFeature sf : sfs) System.out.println(sf.getFeatureType() == ft);
		//all features are linked to the same ft

		//build sfcollection
		DefaultFeatureCollection sfc = new DefaultFeatureCollection();
		sfc.addAll(sfs);
		System.out.println(sfc.size());
		System.out.println(sfc.getSchema());
		System.out.println(sfc.getCount());

		//test attribute addition
		SimpleFeature sf = sfc.features().next();
		//sf.setAttribute("guyguyg", "dfhfghfgdhfd"); triggers error

		//TODO
		//is it possible to modify the ft on the fly to add an attribute?

	}

}
