package eu.europa.ec.eurostat.jgiscotools.gisco_processes.test;

import java.util.ArrayList;

import org.geotools.data.DataUtilities;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class TestsSF {

	public static void main(String[] args) throws Throwable {

		//is it possible to perform the following operation: load some GeoTools SimpleFeatures, compute a new field, save the result back.

		//first: create feature type
		SimpleFeatureType ft = DataUtilities.createType("", "the_geom:Point:srid=3035,ex:String,nb:int");

		//second: build features with this type
		ArrayList<SimpleFeature> sfs = new ArrayList<SimpleFeature>();
		for(int i=1; i<=10; i++) {
			SimpleFeature sf = new SimpleFeatureBuilder(ft).buildFeature("ID_"+i);
			sf.setDefaultGeometry(new GeometryFactory().createPoint(new Coordinate(i,0)));
			sf.setAttribute("ex", "dgdfgfd_"+i);
			sf.setAttribute("nb", i*5);
			//sf.setAttribute("new", "fdfdgghf"); triggers error because attribute is not defined in the schema
			sfs.add(sf);
		}

		//for(SimpleFeature sf : sfs) System.out.println(sf.getFeatureType() == ft);
		//all features are linked to a unique ft instance



		//test attribute addition

		// is it possible to extend the ft on the fly to add an attribute? No.
		SimpleFeatureType ft2 = DataUtilities.createType("", "the_geom:Point:srid=3035,ex:String,nb:int,new:double");
		System.out.println(ft2);
		//this works only to select existing attributes, not to add new:
		//SimpleFeatureType ft3 = DataUtilities.createSubType(ft, new String[]{"new:double"});
		//System.out.println(ft3);

		//TODO make function to add attribute to featuretype ?
		//SimpleFeatureType ft3 = SimpleFeatureUtil.addAttribute(ft, "new", String.class);
		//System.out.println(ft3);


		for(SimpleFeature sf : sfs) {
			System.out.println(sf);
			//apply new schema to feature - this returns a new feature...
			SimpleFeature sf2 = DataUtilities.reType(ft2, sf);
			System.out.println(sf2);
			sf2.setAttribute("new", 15451);
			System.out.println(sf2);
		}

		//conclusion: it is not possible to modify a simple feature by simply adding a new attribute.
		//there are ways to build a new modified simple feature

	}

}
