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
		for(Feature cell : cells) {

			//get cell data
			Geometry cellGeom = cell.getGeometry();
			String cellId = cell.getAttribute(cellIdAtt).toString();

			//prepare stat object for the cell
			Stat s = new Stat(0, cellIdAtt);
			s.dims.put(cellIdAtt, cellId);

			//get features intersecting (using spatial index)

			//go through features within the cell (using spatial index)
			List<?> fs_ = getFeaturesInd().query(cellGeom.getEnvelopeInternal());
			for(Object f_ : fs_) {
				Feature f = (Feature)f_;
				Geometry geom = f.getGeometry();

				if(! geom.getEnvelopeInternal().intersects(cellGeom.getEnvelopeInternal()))
					continue;

				//compute intersection
				Geometry inter = cell.getGeometry().intersection(geom);
				if(inter.isEmpty())
					continue;

				//TODO compute feature contribution
			}

			//store stat
			sh.stats.add(s);
		}
	}

}
