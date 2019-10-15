/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.datamodel;

import java.util.HashMap;
import java.util.Map;

import org.locationtech.jts.geom.Geometry;

/**
 * @author julien Gaffuri
 *
 */
public class Feature /*implements SimpleFeature*/ {

	//id
	private static int ID;
	private String id;
	public String getID() { return id; }
	public void setID(String id) { this.id = id; }

	public Feature(){
		id = String.valueOf(ID++);
	}

	//geometry
	private Geometry geometry;
	public Geometry getDefaultGeometry(){ return geometry; }
	public void setDefaultGeometry(Geometry geometry){ this.geometry = geometry; }

	//attributes
	private Map<String, Object> atts;
	public Map<String, Object> getAttributes(){
		if(atts==null) atts = new HashMap<String, Object>();
		return atts;
	}
	public Object getAttribute(String att) { return getAttributes().get(att); }
	public Object setAttribute(String key, Object value) { return getAttributes().put(key, value); }

}
