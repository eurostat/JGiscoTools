/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.algo.filterselection;

import org.opengis.feature.simple.SimpleFeature;

/**
 * @author julien Gaffuri
 *
 */
public class SCEqualsValue implements SelectionCriteria {
	private String att;
	private String val;

	public SCEqualsValue(String attribute, String value){
		att=attribute;
		val=value;
	}

	public boolean keep(Object f) {
		return (val.equals(((SimpleFeature)f).getAttribute(att)));
	}

}
