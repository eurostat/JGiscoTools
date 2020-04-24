/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.algo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import eu.europa.ec.eurostat.jgiscotools.algo.base.Union;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.feature.JTSGeomUtil;

/**
 * @author julien Gaffuri
 *
 */
public class Partition {

	//TODO handle operations which do not change the input features but produce outputs - exemple: partition areas creation. Recomposition method should then concern these outputs (when necessary).

	public final static Logger LOGGER = LogManager.getLogger(Partition.class.getName());

	public static Collection<Feature> runRecursively(Collection<Feature> features, PartitionedOperation op, boolean parallel, int maxCoordinatesNumber, int objMaxCoordinateNumber, boolean ignoreRecomposition, GeomType gt, double midRandom) {
		Partition p = new Partition("0", features, op, gt, midRandom);
		p.runRecursively(parallel, maxCoordinatesNumber, objMaxCoordinateNumber, ignoreRecomposition);
		return p.getFeatures();
	}

	private String code;
	public String getCode() { return code; }

	public Collection<Feature> features = null;
	public Collection<Feature> getFeatures() { return features; }

	public interface PartitionedOperation { void run(Partition p); }
	private PartitionedOperation operation;

	//the partition input geometry type
	public enum GeomType { ONLY_AREAS, ONLY_LINES, ONLY_POINTS, MIXED }
	private GeomType geomType = GeomType.MIXED;

	//some randomness factor on the middle separation used when splitting a partition into sub partitions
	private double midRandom = 0;

	private Envelope env;
	public Envelope getEnvelope() { return env; }
	public Polygon getExtend(GeometryFactory gf) { return JTS.toGeometry(this.env, gf); }

	private Partition(String code, Collection<Feature> features, PartitionedOperation op, GeomType gt, double midRandom){
		this(code, op, gt, midRandom, FeatureUtil.getEnvelope(features, 1.001));
		this.features = features;
	}
	private Partition(String code, PartitionedOperation op, GeomType gt, double midRandom, double xMin, double xMax, double yMin, double yMax){ this(code, op, gt, midRandom, new Envelope(xMin,xMax,yMin,yMax)); }
	private Partition(String code, PartitionedOperation op, GeomType gt, double midRandom, Envelope env) {
		this.code = code;
		this.operation = op;
		this.geomType = gt;
		this.midRandom = midRandom;
		this.env = env;
	}

	//determine if the partition is too large: if it has too many vertices, or if it contains a polygonal part with too many vertices
	private int coordinatesNumber = 0, maxEltCN = 0;
	private boolean isTooLarge(int maxCoordinatesNumber, int objMaxCoordinateNumber) {
		coordinatesNumber = 0;
		maxEltCN = 0;
		for(Feature f : features) {
			for(Geometry poly : JTSGeomUtil.getGeometries(f.getGeometry())) {
				int fcn = poly.getNumPoints();
				coordinatesNumber += fcn;
				maxEltCN = Math.max(maxEltCN, fcn);
			}
		}
		return coordinatesNumber > maxCoordinatesNumber || maxEltCN > objMaxCoordinateNumber;
	}


	//run process on the partition, decomposing it recursively if it is too large.
	private void runRecursively(boolean parallel, int maxCoordinatesNumber, int objMaxCoordinateNumber, boolean ignoreRecomposition) {
		if(! isTooLarge(maxCoordinatesNumber, objMaxCoordinateNumber)) {
			if(LOGGER.isTraceEnabled()) LOGGER.trace(this.code+"   not too large: Run process...");
			operation.run(this);
		} else {
			if(LOGGER.isTraceEnabled()) LOGGER.trace(this.code+"   too large: Decompose it...");
			Collection<Partition> subPartitions = decompose();

			//run process on sub-partitions
			//TODO allow parallel computation
			for(Partition sp : subPartitions)
				sp.runRecursively(parallel, maxCoordinatesNumber, objMaxCoordinateNumber, ignoreRecomposition);

			if(!ignoreRecomposition) {
				if(LOGGER.isTraceEnabled()) LOGGER.trace(this.code+"   Recomposing");
				recompose(subPartitions);
			}
		}
	}

	//decompose the partition into four partitions
	private Collection<Partition> decompose() {
		//create four sub-partitions

		double xMid = env.getMinX() + (0.5 + midRandom*(Math.random()-0.5)) * (env.getMaxX() - env.getMinX());
		double yMid = env.getMinY() + (0.5 + midRandom*(Math.random()-0.5)) * (env.getMaxY() - env.getMinY());

		/*Coordinate c = FeatureUtil.getMedianPosition(features);
		double xMid = c.x;
		double yMid = c.y;*/

		Partition
		p1 = new Partition(this.code+"1", operation, geomType, midRandom, env.getMinX(), xMid, yMid, env.getMaxY()),
		p2 = new Partition(this.code+"2", operation, geomType, midRandom, xMid, env.getMaxX(), yMid, env.getMaxY()),
		p3 = new Partition(this.code+"3", operation, geomType, midRandom, env.getMinX(), xMid, env.getMinY(), yMid),
		p4 = new Partition(this.code+"4", operation, geomType, midRandom, xMid, env.getMaxX(), env.getMinY(), yMid)
		;

		//fill it
		p1.cutAndSetFeatures(features);
		p2.cutAndSetFeatures(features);
		p3.cutAndSetFeatures(features);
		p4.cutAndSetFeatures(features);

		Collection<Partition> subPartitions = new ArrayList<Partition>();
		if(p1.features.size()>0) subPartitions.add(p1);
		if(p2.features.size()>0) subPartitions.add(p2);
		if(p3.features.size()>0) subPartitions.add(p3);
		if(p4.features.size()>0) subPartitions.add(p4);

		//clean top partition to avoid heavy duplication of features
		features.clear(); features = null;

		return subPartitions;
	}

	private void cutAndSetFeatures(Collection<Feature> inFeatures) {

		features = new HashSet<Feature>();
		Polygon extend = null;

		for(Feature f : inFeatures) {
			Geometry g = f.getGeometry();
			Envelope env_ = g.getEnvelopeInternal();
			if(!this.env.intersects(env_)) continue;

			//feature fully in the envelope
			if(env.contains(env_)) {
				features.add(f);
				continue;
			}

			//check if feature intersects envelope
			if(extend == null) extend = getExtend(g.getFactory());
			Geometry inter = g.intersection(extend);
			if(inter.isEmpty()) continue;
			if(geomType.equals(GeomType.ONLY_AREAS) && inter.getArea() == 0) continue;
			if(geomType.equals(GeomType.ONLY_LINES) && inter.getLength() == 0) continue;

			//create intersection feature
			Feature f_ = new Feature();
			inter = JTSGeomUtil.toMulti(inter);
			if(geomType.equals(GeomType.ONLY_AREAS)) inter = JTSGeomUtil.getPolygonal(inter);
			if(geomType.equals(GeomType.ONLY_LINES)) inter = JTSGeomUtil.getLinear(inter);
			if(geomType.equals(GeomType.ONLY_POINTS)) inter = JTSGeomUtil.getPuntual(inter);
			f_.setGeometry(inter);
			f_.getAttributes().putAll(f.getAttributes());
			f_.setID( f.getID() );
			features.add(f_);
		}

		//set reduced envelope
		if(features.size()>0) this.env = FeatureUtil.getEnvelope(features);

		if(LOGGER.isTraceEnabled()) LOGGER.trace(this.code+"   Features: "+features.size()+" kept from "+inFeatures.size()+". "+(int)(100*features.size()/inFeatures.size()) + "%");
	}


	//recompose partition
	private void recompose(Collection<Partition> subPartitions) {

		//gather pieces together
		HashMap<String,Collection<Geometry>> index = new HashMap<String,Collection<Geometry>>();
		for(Partition p : subPartitions)
			for(Feature f : p.features) {
				Collection<Geometry> col = index.get(f.getID());
				if(col == null) {
					col = new ArrayList<Geometry>();
					index.put(f.getID(), col);
				}
				col.add(f.getGeometry());
			}

		//get features with pieces together
		features = new HashSet<Feature>();
		HashSet<String> fIds = new HashSet<String>();
		for(Partition p : subPartitions)
			for(Feature f : p.features) {
				if(fIds.contains(f.getID())) continue;
				fIds.add(f.getID());
				features.add(f);
				Collection<Geometry> pieces = index.get(f.getID());
				if(pieces.size()==1)
					f.setGeometry(pieces.iterator().next());
				else {
					Geometry union = null;
					if(geomType.equals(GeomType.ONLY_AREAS)) union = Union.polygonsUnionAll(pieces);
					else if(geomType.equals(GeomType.ONLY_LINES)) ; //TODO
					else if(geomType.equals(GeomType.ONLY_POINTS)) ; //TODO
					else ; //TODO

					f.setGeometry(union);
				}
			}
		index.clear();
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();

		//print basic information on partition size
		sb
		.append("Partition ").append(code).append(" -")
		.append(" CoordNb=").append(coordinatesNumber)
		.append(" MaxFCN=").append(maxEltCN)		
		.append(" FeatNb=").append(features.size())
		;

		//if number of features is low, show their ids
		if(features.size() <=5) {
			sb.append(" id="); int i=0;
			for(Feature f : features) {
				i++;
				sb.append(f.getID()).append(",");
				if(i>=4) break;
			}
		}

		return sb.toString();
	}



	//build a dataset of partition areas, with some information on each partition area
	public static Collection<Feature> getPartitionDataset(Collection<Feature> features, boolean parallel, int maxCoordinatesNumber, int objMaxCoordinateNumber, GeomType gt, double midRandom) {
		final Collection<Feature> fs = new ArrayList<Feature>();

		Partition.runRecursively(features, p -> {
			LOGGER.info(p.toString());
			double area = p.env.getArea();
			Feature f = new Feature();
			f.setGeometry(p.getExtend(null));
			f.setAttribute("code", p.code);
			f.setAttribute("f_nb", p.features.size());
			f.setAttribute("c_nb", p.coordinatesNumber);
			f.setAttribute("c_dens", p.coordinatesNumber/area);
			f.setAttribute("maxfcn", p.maxEltCN);
			f.setAttribute("area", area);
			fs.add(f);
		}, parallel, maxCoordinatesNumber, objMaxCoordinateNumber, true, gt, midRandom);

		return fs;
	}

	/*
	public static void main(String[] args) {
		//LOGGER.setLevel(Level.ALL);
		System.out.println("Load");
		//ArrayList<Feature> features = SHPUtil.loadSHP("/home/juju/Bureau/nuts_gene_data/commplus/COMM_PLUS_WM.shp", 3857).fs;
		ArrayList<Feature> features = SHPUtil.loadSHP("/home/juju/Bureau/nuts_gene_data/out/100k_1M/commplus/COMM_PLUS_WM_1M_6.shp", 3857).fs;
		System.out.println("Compute");
		Collection<Feature> fs = getPartitionDataset(features, 1000000, 1000);
		System.out.println("Save");
		SHPUtil.saveSHP(fs, "/home/juju/Bureau/partition/", "partition.shp");
	}
	 */
}
