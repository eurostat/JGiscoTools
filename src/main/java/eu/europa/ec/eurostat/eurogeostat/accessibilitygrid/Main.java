/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.accessibilitygrid;

import java.util.ArrayList;
import java.util.Collection;

import org.opencarto.datamodel.Feature;
import org.opencarto.io.SHPUtil;
import org.opencarto.util.JTSGeomUtil;
import org.opencarto.util.ProjectionUtil;

/**
 * @author julien Gaffuri
 *
 */
public class Main {

	public static void main(String[] args) {

		//example
		//https://krankenhausatlas.statistikportal.de/

		System.out.println("Start");

		//create xkm grid
		String outPath = "C:/Users/gaffuju/Desktop";


		double res = 5000;
		int epsg = 3035;
		Collection<Feature> fs = new ArrayList<Feature>();
		for(double x=0; x<10000000; x+=res)
			for(double y=0; y<10000000; y+=res) {
				Feature f = new Feature();
				f.setGeom( JTSGeomUtil.createPolygon( x,y, x+res,y, x+res,y+res, x,y+res, x,y ) );
				f.setId( "CRS"+Integer.toString((int)epsg)+"RES"+Integer.toString((int)res)+x+y );
				f.set("cellId", f.getId());
				fs.add(f);
			}
		System.out.println("Save " + fs.size() + " cells");
		SHPUtil.saveSHP(fs, outPath+"/out/grid.shp", ProjectionUtil.getCRS(epsg));

		System.out.println("End");
	}

}
