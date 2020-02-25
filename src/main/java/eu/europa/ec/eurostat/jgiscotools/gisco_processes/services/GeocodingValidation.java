package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services;

import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoPackageUtil;

public class GeocodingValidation {

	public static void main(String[] args) throws Exception {
		System.out.println("Start");
		
		//load hospitals
		System.out.println("load");
		ArrayList<Map<String,String>> hospitals = CSVUtil.load(HealthCareDataFormattingGeocoding.path+"AT/AT_geolocated.csv", CSVFormat.DEFAULT.withFirstRecordAsHeader());
		CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
		
		//build linear features
		ArrayList<Feature> errors = new ArrayList<>();
		for(Map<String, String> h : hospitals) {
			Coordinate cGISCO = new Coordinate(Double.parseDouble(h.get("lonGISCO")), Double.parseDouble(h.get("latGISCO")));
			Coordinate cBing = new Coordinate(Double.parseDouble(h.get("lonBing")), Double.parseDouble(h.get("latBing")));
			LineString line = new GeometryFactory().createLineString( new Coordinate[] {cBing, cGISCO} );
			Feature error = new Feature();
			error.setGeometry(line);
			double dist = JTS.orthodromicDistance( cGISCO, cBing, crs);
			error.setAttribute("dist", dist);

			errors.add(error);
		}
		
		//save linear feature
		GeoPackageUtil.save(errors, HealthCareDataFormattingGeocoding.path+"AT/geocoding_validation.gpkg", crs, true);
		
		System.out.println("End");
	}

}
