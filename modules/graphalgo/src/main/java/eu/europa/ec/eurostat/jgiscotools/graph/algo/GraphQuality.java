package eu.europa.ec.eurostat.jgiscotools.graph.algo;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.locationtech.jts.index.strtree.STRtree;

import eu.europa.ec.eurostat.jgiscotools.algo.base.NodingUtil;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.graph.algo.NodeReduction.NodeReductionCriteria;
import eu.europa.ec.eurostat.jgiscotools.graph.base.GraphBuilder;
import eu.europa.ec.eurostat.jgiscotools.graph.base.GraphToFeature;
import eu.europa.ec.eurostat.jgiscotools.graph.base.structure.Graph;

public class GraphQuality {
	public final static Logger LOGGER = LogManager.getLogger(GraphQuality.class.getName());

	/**
	 * Perform a number of operations on network sections to ensure a clean planar graph can be built from them.
	 * 
	 * @param secs
	 * @return
	 */
	public static Collection<Feature> qualityFixForSections(Collection<Feature> secs) {

		LOGGER.info("Decompose into non-coln");
		secs = FeatureUtil.getFeaturesWithSimpleGeometrie(secs);
		LOGGER.info(secs.size());

		LOGGER.info("Fix section intersection");
		secs = fixSectionsIntersectionIterative(secs);
		LOGGER.info(secs.size());

		LOGGER.info("Decompose into non-coln");
		secs = FeatureUtil.getFeaturesWithSimpleGeometrie(secs);
		LOGGER.info(secs.size());

		LOGGER.info("Add vertices at node intersections");
		NodingUtil.fixLineStringsIntersectionNoding(secs);
		LOGGER.info(secs.size());

		LOGGER.info("Ensure node reduction");
		secs = ensureNodeReduction(secs);
		LOGGER.info(secs.size());

		return secs;
	}


	public static Collection<Feature> ensureNodeReduction(Collection<Feature> secs) {
		return ensureNodeReduction(secs, NodeReduction.DEFAULT_NODE_REDUCTION_CRITERIA);
	}
	public static Collection<Feature> ensureNodeReduction(Collection<Feature> secs, NodeReductionCriteria nrc) {
		Graph g = GraphBuilder.buildFromLinearFeaturesNonPlanar(secs);
		NodeReduction.ensure(g, nrc);
		GraphToFeature.updateEdgeLinearFeatureGeometry(g.getEdges());
		secs = GraphToFeature.getAttachedFeatures(g.getEdges());
		return secs;
	}



	/**
	 * Check some linear features do not intersect along linear parts.
	 * This should be avoided to build a planar network.
	 * 
	 * @param secs
	 */
	public static void checkSectionsIntersection(Collection<Feature> secs) {

		//build spatial index for features
		STRtree si = FeatureUtil.getSTRtree(secs);

		//go through pairs of sections
		for(Feature sec1 : secs) {
			Geometry g1 = sec1.getGeometry();
			@SuppressWarnings("unchecked")
			Collection<Feature> secs_ = (Collection<Feature>)si.query(g1.getEnvelopeInternal());
			for(Feature sec2 : secs_) {
				if(sec1==sec2) continue;
				if(sec1.getID().compareTo(sec2.getID()) < 0) continue;

				Geometry g2 = sec2.getGeometry();
				if(!g1.getEnvelopeInternal().intersects(g2.getEnvelopeInternal())) continue;

				Geometry inter = g1.intersection(g2);
				if(inter.isEmpty() || inter.getLength() == 0) continue;

				LOGGER.warn("Unexpected intersection between "+sec1.getID()+" and "+sec2.getID() + " around " + inter.getCentroid().getCoordinate());
				LOGGER.warn("   Inter length = "+inter.getLength());
				LOGGER.warn("   Length 1 = "+g1.getLength());
				LOGGER.warn("   Length 2 = "+g2.getLength());
			}
		}
	}

	/**
	 * Fix intersection issue.
	 * If sections are fully overlapped by other, they might be removed.
	 * The list of remaining sections with non-empty geometries is returned.
	 * These geometries might be multilinestring. To get only simple geometries, use FeatureUtil.getFeaturesWithSimpleGeometrie
	 * 
	 * @param secs
	 * @return
	 */
	public static Collection<Feature> fixSectionsIntersection(Collection<Feature> secs) {
		Collection<Feature> out = new ArrayList<Feature>();
		out.addAll(secs);

		//build spatial index for features
		Quadtree si = FeatureUtil.getQuadtree(secs);

		//go through pairs of sections
		for(Feature sec1 : secs) {
			@SuppressWarnings("unchecked")
			Collection<Feature> secs_ = (Collection<Feature>)si.query(sec1.getGeometry().getEnvelopeInternal());
			for(Feature sec2 : secs_) {
				if(sec1 == sec2) continue;
				if(sec1.getID().compareTo(sec2.getID()) < 0) continue;

				Geometry g1 = sec1.getGeometry();
				if(g1.isEmpty()) { out.remove(sec1); break; }
				Geometry g2 = sec2.getGeometry();
				if(g2.isEmpty()) { out.remove(sec2); continue; }
				if( ! g1.getEnvelopeInternal().intersects(g2.getEnvelopeInternal()) ) continue;

				Geometry inter = g1.intersection(g2);
				if(inter.isEmpty() || inter.getLength() == 0) continue;

				//choose the one to fix: give priority to longest one.
				Feature sec; Geometry diff;
				if(g1.getLength() > g2.getLength()) {
					sec = sec2;
					diff = g2.difference(g1);
				} else {
					sec = sec2;
					diff = g1.difference(g2);
				}

				boolean b = si.remove(sec.getGeometry().getEnvelopeInternal(), sec);
				if(!b) LOGGER.warn("Problem when trying to remove section from spatial index");
				sec.setGeometry(diff);
				if(diff.isEmpty())
					out.remove(sec);
				else
					si.insert(diff.getEnvelopeInternal(), sec);
			}
		}
		return out;
	}

	public static Collection<Feature> fixSectionsIntersectionIterative(Collection<Feature> secs) {
		Collection<Feature> out = fixSectionsIntersection(secs);
		int nb = out.size(), nb_ = Integer.MAX_VALUE;
		while(nb<nb_) {
			out = fixSectionsIntersection(out);
			nb_ = nb;
			nb = out.size();
		}
		return out;
	}

	
}
