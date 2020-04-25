/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.feature;

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

	/** @return The feature identifier. */
	public String getID() {
		return id;
	}

	/**
	 * Set feature identifier.
	 * 
	 * @param id
	 */
	public void setID(String id) {
		this.id = id;
	}

	/**
	 * 
	 */
	public Feature() {
		id = String.valueOf(ID++);
	}

	private static int ID = 1000;

	/** The feature Geometry */
	private Geometry geometry;

	/** @return The feature Geometry */
	public Geometry getGeometry() {
		return geometry;
	}

	/**
	 * Set geometry.
	 * 
	 * @param geometry
	 */
	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

	/** The feature attributes, as a dictionary <key,value> */
	private Map<String, Object> atts;

	/** @return The attributes map */
	public Map<String, Object> getAttributes() {
		if (atts == null)
			atts = new HashMap<String, Object>();
		return atts;
	}

	/**
	 * Get attribute value.
	 * 
	 * @param attributeName
	 * @return
	 */
	public Object getAttribute(String attributeName) {
		return getAttributes().get(attributeName);
	}

	/**
	 * Set attribute value.
	 * 
	 * @param attributeName
	 * @param value
	 * @return
	 */
	public Object setAttribute(String attributeName, Object value) {
		return getAttributes().put(attributeName, value);
	}

}
