/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridaggregation;

import java.util.Collection;
import java.util.List;

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
	Collection<Feature> cells = null;
	//grid id
	String cellIdAtt = "id";

	//the geo features
	Collection<Feature> features = null;

	private STRtree featuresInd = null;
	private STRtree getFeaturesInd() {
		if(featuresInd == null) {
			logger.info("Index features");
			featuresInd = new STRtree();
			for(Feature f : features)
				if(f.getGeometry() != null)
					featuresInd.insert(f.getGeometry().getEnvelopeInternal(), f);
		}
		return featuresInd;
	}


	FeatureContributionCalculator fcc = dfcc;

	public interface FeatureContributionCalculator { double getContribution(Feature f); }

	//just count
	public static FeatureContributionCalculator dfcc = new FeatureContributionCalculator() {
		@Override
		public double getContribution(Feature f) { return 1; }
	};




	//output stats
	StatsHypercube sh = null;

	//the constructor
	public GridAggregator() {
		//TODO
	}

	private void compute() {

		//initialise stats
		sh = new StatsHypercube();

		//TODO parallel
		for(Feature c : cells) {

			//get cell data
			Geometry cGeom = c.getGeometry();
			String cId = cellIdAtt==null? c.getID() : c.getAttribute(cellIdAtt).toString();

			//prepare stat object for the cell
			String cia = cellIdAtt==null? "id" : cellIdAtt;
			Stat s = new Stat(0, cia);
			s.dims.put(cia, cId);

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

				//compute feature contribution
				s.value += fcc.getContribution(f);
			}

			//store stat
			sh.stats.add(s);
		}
	}

}
