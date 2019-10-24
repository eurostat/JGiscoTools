/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.datamodel;

import java.util.HashMap;
import java.util.Map;

import org.locationtech.jts.geom.Geometry;

/**
 * 
 * A POJO structure to represent simple geographical feature. It makes
 * manipulation of these features even simpler than with SimpleFeature.
 * 
 * @author julien Gaffuri
 *
 */
public class Feature {

	/** The feature identifier */
	private String id;

	public String getID() {
		return id;
	}

	public void setID(String id) {
		this.id = id;
	}

	public Feature() {
		id = String.valueOf(ID++);
	}

	private static int ID;

	/** The feature Geometry */
	private Geometry geometry;

	public Geometry getDefaultGeometry() {
		return geometry;
	}

	public void setDefaultGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

	/** The feature attributes, as a dictionary <key,value> */
	private Map<String, Object> atts;

	public Map<String, Object> getAttributes() {
		if (atts == null)
			atts = new HashMap<String, Object>();
		return atts;
	}

	public Object getAttribute(String att) {
		return getAttributes().get(att);
	}

	public Object setAttribute(String key, Object value) {
		return getAttributes().put(key, value);
	}

}
