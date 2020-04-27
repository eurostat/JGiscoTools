/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.algo.filterselection;

import org.opengis.feature.simple.SimpleFeature;

/**
 * @author julien Gaffuri
 *
 */
public class SCWithinInterval implements SelectionCriteria {
	private String att;
	private double low, up;

	public SCWithinInterval(String attribute, double lowerBound, double upperBound){
		att=attribute;
		low=lowerBound;
		up=upperBound;
	}

	public boolean keep(Object f) {
		Object val_ = ((SimpleFeature)f).getAttribute(att);
		double val=0;
		if(val_ instanceof Integer) val = (Integer)((SimpleFeature)f).getAttribute(att);
		else if(val_ instanceof Double) val = (Double)((SimpleFeature)f).getAttribute(att);
		else{System.err.println("Unexpected attribute type: "+val_.getClass().getName()+". Should be numerical.");}
		return (val<=up && val>=low);
	}

}
