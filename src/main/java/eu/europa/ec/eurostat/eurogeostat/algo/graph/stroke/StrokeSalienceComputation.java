/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.algo.graph.stroke;

import java.util.Collection;

import org.apache.log4j.Logger;

/**
 * @author julien Gaffuri
 *
 */
public class StrokeSalienceComputation {
	public final static Logger LOGGER = Logger.getLogger(StrokeSalienceComputation.class.getName());

	//between 0 (not salient) and 1 (very salient)
	public double getSalience(Stroke s) {
		double sal = s.getDefaultGeometry().getLength();
		//TODO should be based on 1.length and 2.attribute value "representative"
		return sal;
	};


	public void setSalience(Collection<Stroke> sts) {
		for(Stroke s : sts)
			s.setAttribute("sal", getSalience(s));
	}

}
