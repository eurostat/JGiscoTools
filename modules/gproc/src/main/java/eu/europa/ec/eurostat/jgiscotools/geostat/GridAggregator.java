/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.geostat;

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

/**
 * 
 * Compute statistics at grid level from vector geo data.
 * 
 * @author Julien Gaffuri
 *
 */
public class GridAggregator {
	private static Logger logger = LogManager.getLogger(GridAggregator.class.getName());

	//the grid cells
	private Collection<Feature> cells = null;
	//grid id
	private String cellIdAtt = "GRD_ID";

	//the geo features from which to compute aggregates
	private Collection<Feature> features = null;

	//
	private FeatureContributionCalculator fcc = dfcc;

	public interface FeatureContributionCalculator { double getContribution(Feature f, Geometry inter); }
	private static FeatureContributionCalculator dfcc = new FeatureContributionCalculator() {
		@Override
		public double getContribution(Feature f, Geometry inter) { return 1; }
	};


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
	 * @param fcc The function to compute the contribution of the feature to the final statistic.
	 */
	public GridAggregator(Collection<Feature> cells, String cellIdAtt, Collection<Feature> features, FeatureContributionCalculator fcc) {
		this.cells = cells;
		this.cellIdAtt = cellIdAtt;
		this.features = features;
		this.fcc = fcc;
	}

	//the spatial index of the input features
	private STRtree featuresInd = null;
	private STRtree getFeaturesInd() {
		synchronized(featuresInd) {
			if(featuresInd == null) {
				logger.info("Index features");
				featuresInd = new STRtree();
				for(Feature f : features)
					if(f.getGeometry() != null)
						featuresInd.insert(f.getGeometry().getEnvelopeInternal(), f);
			}
		}
		return featuresInd;
	}

	/**
	 * @param parallel
	 */
	public void compute(boolean parallel) {

		//initialise stats
		String cia = cellIdAtt==null? "id" : cellIdAtt;
		stats = new StatsHypercube(cia);

		logger.info("Compute grid aggregation...");
		Stream<Feature> st = cells.stream(); if(parallel) st = st.parallel();
		st.forEach(c -> {

			//get cell data
			Geometry cGeom = c.getGeometry();
			String cId = cellIdAtt==null? c.getID() : c.getAttribute(cellIdAtt).toString();

			//prepare stat object for the cell
			Stat s = new Stat(0, cia, cId);

			//go through features within the cell (using spatial index)
			List<?> fs_ = getFeaturesInd().query(cGeom.getEnvelopeInternal());
			for(Object f_ : fs_) {
				Feature f = (Feature)f_;
				Geometry geom = f.getGeometry();

				if(! geom.getEnvelopeInternal().intersects(cGeom.getEnvelopeInternal()))
					continue;

				//compute intersection
				Geometry inter = c.getGeometry().intersection(geom);
				if(inter.isEmpty())
					continue;

				//add feature contribution
				s.value += fcc.getContribution(f, inter);
			}

			//store stat
			stats.stats.add(s);
		});
	}

}
