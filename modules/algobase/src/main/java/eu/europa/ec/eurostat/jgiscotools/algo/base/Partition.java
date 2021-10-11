/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.algo.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.feature.JTSGeomUtil;

/**
 * Partionning of processes, for large datasets of features, following a quadtree partitioning.
 * The principle is to:
 * 1. Analyse the size of the dataset
 * 2. If it is too large (according to some criteria), then it is decomposed into 4 parts, which are then analysed again (recursive call)
 * 3. If it is small enough, the process is launched
 * 4. When the 4 parts are processed, their result is re-united if necessary
 * 
 * @author julien Gaffuri
 *
 */
public class Partition {
	private final static Logger LOGGER = LogManager.getLogger(Partition.class.getName());

	//TODO handle more generic case:
	//partitionning which change inputs - parition wich porduces output
	//partitionning where input is split - or not
	//partitionning where ouput is joined - or not


	/**
	 * Run a process with recursive partitionning.
	 * This is a process which is applied on some input features, and the partitionning depends on the size of these input features.
	 * 
	 * @param features The features to process
	 * @param op The operation to apply based on the features of one partition
	 * @param parallel Set to true to allow parallel processing
	 * @param maxCoordinatesNumber Indicator of the partition size: The number of vertices of the geometries. Above this value, the dataset is considered as too large, and the sub-partionning is launched.
	 * @param objMaxCoordinateNumber Indicator of the partition size: The number of vertices of the larger geometry. Above this value, the dataset is considered as too large, and the sub-partionning is launched.
	 * @param withSplitRecompose Set to true if input features should be split/recomposed when partitionning.
	 * @param gt The geometry type of the features
	 * @param midRandom Randomness factor on the middle separation used when splitting a partition into sub partitions, within [0,1]. Set to 0 for no randomness.
	 * @return
	 */
	public static void runRecursivelyApply(Collection<Feature> features, PartitionedOperation op, boolean parallel, int maxCoordinatesNumber, int objMaxCoordinateNumber, boolean withSplitRecompose, GeomType gt, double midRandom) {
		Partition p = new Partition("0", features, op, gt, midRandom);
		p.runRecursivelyApply(parallel, maxCoordinatesNumber, objMaxCoordinateNumber, withSplitRecompose);
	}



	/**
	 * The code of the partition.
	 * It is a character string among [1,2,3,4] indicating the position of the partition in the quadtree.
	 */
	private String code;
	public String getCode() { return code; }

	/**
	 * The features of the partition, to be processed.
	 */
	public Collection<Feature> features = null;
	public Collection<Feature> getFeatures() { return features; }

	/**
	 * An operation to be performed on a partition.
	 * 
	 * @author julien Gaffuri
	 *
	 */
	public interface PartitionedOperation { void run(Partition p); }

	/**
	 * The operation to perform on the partition features.
	 */
	private PartitionedOperation operation;

	/**
	 * The partition input geometry type
	 * 
	 * @author julien Gaffuri
	 *
	 */
	public enum GeomType { ONLY_AREAS, ONLY_LINES, ONLY_POINTS, MIXED }
	private GeomType geomType = GeomType.MIXED;

	/**
	 * Randomness factor on the middle separation used when splitting a partition into sub partitions, within [0,1]
	 */
	private double midRandom = 0;

	/**
	 * The envelope of the partition
	 */
	private Envelope env;
	public Envelope getEnvelope() { return env; }
	public Polygon getExtent(GeometryFactory gf) { return JTS.toGeometry(this.env, gf); }

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

	private int coordinatesNumber = 0, maxEltCN = 0;
	/**
	 * Determine if the partition is too large: if it has too many vertices, or if it contains a geometry with too many vertices.
	 * 
	 * @param maxCoordinatesNumber
	 * @param objMaxCoordinateNumber
	 * @return
	 */
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


	/**
	 * Run process on the partition, decomposing it recursively if it is too large.
	 * 
	 * @param parallel
	 * @param maxCoordinatesNumber
	 * @param objMaxCoordinateNumber
	 * @param withSplitRecompose
	 */
	private void runRecursivelyApply(boolean parallel, int maxCoordinatesNumber, int objMaxCoordinateNumber, boolean withSplitRecompose) {

		if(! isTooLarge(maxCoordinatesNumber, objMaxCoordinateNumber)) {
			if(LOGGER.isTraceEnabled()) LOGGER.trace(this.code+"   not too large: Run process...");
			operation.run(this);

		} else {
			if(LOGGER.isTraceEnabled()) LOGGER.trace(this.code+"   too large: Subpartition it...");

			//subpartition
			Collection<Partition> subPartitions = subpartition(withSplitRecompose);

			//run process on sub-partitions
			Stream<Partition> st = subPartitions.stream();
			if(parallel) st = st.parallel();
			st.forEach(sp -> {
				sp.runRecursivelyApply(parallel, maxCoordinatesNumber, objMaxCoordinateNumber, withSplitRecompose);
			});
			st.close();

			//recompose
			if(withSplitRecompose) {
				if(LOGGER.isTraceEnabled()) LOGGER.trace(this.code+"   Recomposing");
				recompose(subPartitions);
			}
		}
	}

	/**
	 * Subpartition the partition into four sub-partitions
	 * @param withSplit Set to true if input features have to be split
	 * 
	 * @return
	 */
	private Collection<Partition> subpartition(boolean withSplit) {
		//create four sub-partitions

		double xMid = env.getMinX() + (0.5 + midRandom*(Math.random()-0.5)) * (env.getMaxX() - env.getMinX());
		double yMid = env.getMinY() + (0.5 + midRandom*(Math.random()-0.5)) * (env.getMaxY() - env.getMinY());

		/*Coordinate c = FeatureUtil.getMedianPosition(features);
		double xMid = c.x;
		double yMid = c.y;*/

		//prepare sub partitions
		Partition
		p1 = new Partition(this.code+"1", operation, geomType, midRandom, env.getMinX(), xMid, yMid, env.getMaxY()),
		p2 = new Partition(this.code+"2", operation, geomType, midRandom, xMid, env.getMaxX(), yMid, env.getMaxY()),
		p3 = new Partition(this.code+"3", operation, geomType, midRandom, env.getMinX(), xMid, env.getMinY(), yMid),
		p4 = new Partition(this.code+"4", operation, geomType, midRandom, xMid, env.getMaxX(), env.getMinY(), yMid)
		;


		if(withSplit) {

			//fill sub partitions
			p1.cutAndSetFeatures(features);
			p2.cutAndSetFeatures(features);
			p3.cutAndSetFeatures(features);
			p4.cutAndSetFeatures(features);

			//clean top partition to avoid heavy duplication of features
			features.clear();
		} else {

			//fill sub partitions
			p1.addFeatures(features);
			p2.addFeatures(features);
			p3.addFeatures(features);
			p4.addFeatures(features);

			//check number
			int nb = p1.features.size() + p2.features.size() + p3.features.size() + p4.features.size();
			if(nb != features.size()) LOGGER.error("Error when partitionning without split: " + nb + " != " +features.size());
		}

		//return list of sub partitions
		Collection<Partition> subPartitions = new ArrayList<Partition>();
		if(p1.features.size()>0) subPartitions.add(p1);
		if(p2.features.size()>0) subPartitions.add(p2);
		if(p3.features.size()>0) subPartitions.add(p3);
		if(p4.features.size()>0) subPartitions.add(p4);

		return subPartitions;
	}


	/**
	 * @param inFeatures
	 */
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
			if(extend == null) extend = getExtent(g.getFactory());
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


	/**
	 * @param inFeatures
	 */
	private void addFeatures(Collection<Feature> inFeatures) {

		features = new HashSet<Feature>();
		for(Feature f : inFeatures) {
			Envelope env_ = f.getGeometry().getEnvelopeInternal();

			if(! this.env.intersects(env_)) continue;

			if(this.env.contains(env_)) {
				features.add(f);
				continue;
			}

			if(env_.getMinX() < this.env.getMinX()) {
				features.add(f);
				continue;
			}
			if(env_.getMinY() < this.env.getMinY()) {
				features.add(f);
				continue;
			}
		}

		//set reduced envelope
		if(features.size()>0) this.env = FeatureUtil.getEnvelope(features);

		if(LOGGER.isTraceEnabled()) LOGGER.trace(this.code+"   Features: "+features.size()+" kept from "+inFeatures.size()+". "+(int)(100*features.size()/inFeatures.size()) + "%");
	}



	/**
	 * Recompose sub-partitions.
	 * 
	 * @param subPartitions
	 */
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
		features.clear();
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



	/**
	 * Build a dataset of partition areas, with some information on each partition area
	 * 
	 * @param features
	 * @param parallel
	 * @param maxCoordinatesNumber
	 * @param objMaxCoordinateNumber
	 * @param gt
	 * @param midRandom
	 * @return
	 */
	public static Collection<Feature> getPartitionDataset(Collection<Feature> features, boolean parallel, int maxCoordinatesNumber, int objMaxCoordinateNumber, GeomType gt, double midRandom) {
		final Collection<Feature> fs = new ArrayList<Feature>();

		Partition.runRecursivelyApply(features, p -> {
			LOGGER.info(p.toString());
			double area = p.env.getArea();
			Feature f = new Feature();
			f.setGeometry(p.getExtent(null));
			f.setAttribute("code", p.code);
			f.setAttribute("f_nb", p.features.size());
			f.setAttribute("c_nb", p.coordinatesNumber);
			f.setAttribute("c_dens", p.coordinatesNumber/area);
			f.setAttribute("maxfcn", p.maxEltCN);
			f.setAttribute("area", area);
			fs.add(f);
		}, parallel, maxCoordinatesNumber, objMaxCoordinateNumber, false, gt, midRandom);

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
