package eu.europa.ec.eurostat.jgiscotools.geostat;

import java.io.IOException;
import java.util.HashMap;

import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import eu.europa.ec.eurostat.java4eurostat.base.Stat;
import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.base.StatsIndex;

/**
 * @author julien Gaffuri
 *
 */
public class StatisticalUnitsIntersectionMatrix {

	// The statistical unit feature stores and the attributes to use as identifiers.
	private FeatureSource<SimpleFeatureType,SimpleFeature> su1, su2;
	private String idField1, idField2;
	private boolean cache1=false, cache2=false;


	/**
	 * The intersection matrix as stat hypercube. Dimensions are:
	 * - idField1 and idField2: The ids of respectivelly the SU1 and SU2
	 * - type: Values are "intersection_area", "ratio1from2" and "ratio2from1"
	 */
	public StatsHypercube interSectionMatrix = null;


	public StatisticalUnitsIntersectionMatrix(FeatureSource<SimpleFeatureType,SimpleFeature> su1, String idField1, FeatureSource<SimpleFeatureType,SimpleFeature> su2, String idField2){
		this.su1 = su1;
		this.su2 = su2;
		this.idField1 = idField1;
		this.idField2 = idField2;
	}
	public StatisticalUnitsIntersectionMatrix(FeatureSource<SimpleFeatureType,SimpleFeature> su1, String idField1, boolean cache1, FeatureSource<SimpleFeatureType,SimpleFeature> su2, String idField2, boolean cache2){
		this(su1, idField1, su2, idField2);
		this.cache1=cache1; this.cache2=cache2;
	}


	/**
	 * Compute the intersection matrix between two statistical unit datasets of a same area of interest.
	 * This intersection matrix computes the share of a statistical unit which intersects another ones of another statistical units dataset.
	 * Value of the intersection area is also computed.
	 * 
	 * NB: this operation commutes.
	 */
	public StatisticalUnitsIntersectionMatrix compute() throws IOException{
		interSectionMatrix = new StatsHypercube(this.idField1, this.idField2, "type");

		//load statistical units 1
		int nb1 = su1.getFeatures().size();
		FeatureIterator<SimpleFeature> itSu1 = null;
		if(cache1) itSu1 = DataUtilities.collection(su1.getFeatures()).features();
		else itSu1 = su1.getFeatures().features();

		DefaultFeatureCollection su2_ = null;
		if(cache2) su2_ = DataUtilities.collection(su2.getFeatures());

		//go through statistical units 1
		int counter = 1;
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
		while (itSu1.hasNext()) {
			SimpleFeature f1 = itSu1.next();
			String id1 = f1.getAttribute(idField1).toString();
			Geometry geom1 = (Geometry) f1.getDefaultGeometryProperty().getValue();
			double a1 = geom1.getArea();

			System.out.println("SU1 (id="+id1+") intersection with SU2. counter=" + " " + (counter++) + "/" + nb1 + " " + (Math.round(10000*counter/nb1))*0.01 + "%");

			//get all su2 intersecting the su1 (with spatial index)
			//Filter f = ff.bbox(ff.property("the_geom"), f1.getBounds());
			Filter f = ff.intersects(ff.property("the_geom"), ff.literal(geom1));
			FeatureIterator<SimpleFeature> itSu2 = null;
			if(cache2) itSu2 = su2_.subCollection(f).features();
			else itSu2 = su2.getFeatures(f).features();
			//System.out.println(" -> "+(data2.getFeatures(f).size())+" "+datasetName2);

			while (itSu2.hasNext()) {
				SimpleFeature f2 = itSu2.next();
				String id2 = f2.getAttribute(idField2).toString();
				Geometry geom2 = (Geometry) f2.getDefaultGeometryProperty().getValue();

				//check intersection
				//if(!geom1.intersects(geom2)) continue;
				double interArea = geom1.intersection(geom2).getArea();
				if(interArea == 0) continue;

				//store intersection data
				interSectionMatrix.stats.add(new Stat(interArea, this.idField1, id1, this.idField2, id2, "type", "intersection_area"));
				interSectionMatrix.stats.add(new Stat(interArea/geom2.getArea(), this.idField1, id1, this.idField2, id2, "type", "ratio1from2"));
				interSectionMatrix.stats.add(new Stat(interArea/a1, this.idField1, id1, this.idField2, id2, "type", "ratio2from1"));
			}
			itSu2.close();
		}
		itSu1.close();

		return this;
	}


	/**
	 * Compute statistical values over SU2 from statistical valuesover SU1.
	 * 
	 * @param iniStatValues
	 * @return
	 */
	public StatsHypercube computeStatValueFromIntersection(HashMap<String,Double> iniStatValues) {
		String idFieldIni = this.idField1, idFieldFin = this.idField2, type = "ratio2from1";

		//the output data
		StatsHypercube out = new StatsHypercube(idFieldFin);

		//index matrix figures
		StatsIndex interInd = new StatsIndex(interSectionMatrix.selectDimValueEqualTo("type",type).shrinkDims(), idFieldFin, idFieldIni);

		for(String idFin : interInd.getKeys()){
			double statValueFin = 0;
			double sWeigths = 0;

			//compute weigthted average of 2 contributions
			for(String idIni : interInd.getKeys(idFin)){
				double iniStatValue = iniStatValues.get(idIni);
				if(Double.isNaN(iniStatValue)) continue;

				double weight = interInd.getSingleValue(idFin, idIni);
				if(Double.isNaN(weight)) continue;

				statValueFin += weight * iniStatValue;
				sWeigths += weight;
			}
			statValueFin = sWeigths==0? 0 : statValueFin/sWeigths;
			//store
			out.stats.add( new Stat(statValueFin, idFieldFin, idFin) );
		}

		return out;
	}

}
