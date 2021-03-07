/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.geostat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;

import eu.europa.ec.eurostat.java4eurostat.base.Stat;
import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;

/**
 * 
 * Compute statistics at grid level from vector geo data.
 * 
 * @author Julien Gaffuri
 *
 */
public class GridAggregator<T> {
	private static Logger logger = LogManager.getLogger(GridAggregator.class.getName());

	//the grid cells
	private Collection<Feature> cells = null;
	//grid id
	private String cellIdAtt = "GRD_ID";

	//the geo features from which to compute aggregates
	private Collection<Feature> features = null;

	//
	private MapOperation<T> map = null;
	public interface MapOperation<T> { T map(Feature f, Geometry inter); }


	//
	/** output statistics. */
	StatsHypercube stats = null;
	/** @return The ouput statistics. */
	public StatsHypercube getStats() { return stats; }

	/**
	 * Build a grid aggregator, to compute grid level statistics from features.
	 * 
	 * @param cells The grid cells.
	 * @param cellIdAtt The identifier of the grid cell. can be set to null if the getId() value should be used.
	 * @param features The features to compute the statistics from.
	 * @param map The function to compute the contribution of the feature to the final statistic.
	 */
	public GridAggregator(Collection<Feature> cells, String cellIdAtt, Collection<Feature> features, MapOperation<T> map) {
		this.cells = cells;
		this.cellIdAtt = cellIdAtt;
		this.features = features;
		this.map = map;
	}

	//the spatial index of the input features
	private STRtree featuresInd = null;
	private synchronized STRtree getFeaturesInd() {
		if(featuresInd == null) {
			logger.info("Index features");
			featuresInd = FeatureUtil.getSTRtree(features);
		}
		return featuresInd;
	}

	/**
	 * @param parallel
	 */
	public void compute(boolean parallel) {

		//initialise output statistics
		String cia = cellIdAtt==null? "id" : cellIdAtt;
		stats = new StatsHypercube(cia);

		logger.info("Compute grid aggregation...");
		Stream<Feature> st = cells.stream(); if(parallel) st = st.parallel();
		st.forEach(c -> {

			//get cell data
			Geometry cGeom = c.getGeometry();
			String cId = cellIdAtt==null? c.getID() : c.getAttribute(cellIdAtt).toString();


			//map

			//go through features within the cell (using spatial index)
			List<?> fs_ = getFeaturesInd().query(cGeom.getEnvelopeInternal());
			ArrayList<Object> mapData = new ArrayList<Object>();
			for(Object f_ : fs_) {
				Feature f = (Feature)f_;
				Geometry geom = f.getGeometry();

				if(! geom.getEnvelopeInternal().intersects(cGeom.getEnvelopeInternal()))
					continue;

				//compute intersection
				Geometry inter = c.getGeometry().intersection(geom);
				if(inter.isEmpty())
					continue;

				//map
				T data = map.map(f, inter);
				mapData.add(data);
			}

			//reduce

			//prepare stat object for the cell
			//TODO extract that in reducer that returns stat object
			Stat s = new Stat(0, cia, cId);
			for(Object map : mapData) {
				//add feature contribution
				s.value += (Double)map;
			}

			//store stat
			stats.stats.add(s);
		});
	}

}
