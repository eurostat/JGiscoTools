/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridaggregation;

import java.util.Collection;

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
public class GridAggregator {

	//the grid cells
	Collection<Feature> cells = null;
	//grid id
	String cellIdAtt = "id";

	//the geo features
	Collection<Feature> features = null;

	//output stats
	StatsHypercube sh = null;

	//the constructor
	public GridAggregator() {
		//TODO
	}

	private void compute() {

		//initialise stats
		sh = new StatsHypercube();

		//make spatial index of input features	
		//TODO

		//TODO parallel
		for(Feature cell : cells) {

			//get features intersecting (using spatial index)

			//make stat object
			Stat s = new Stat(0, cellIdAtt);

			//go through features
			//TODO

			//store stat
			sh.stats.add(s);
		}
	}

}
