/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.rail;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.TopologyException;

import eu.europa.ec.eurostat.jgiscotools.algo.Partition;
import eu.europa.ec.eurostat.jgiscotools.algo.Partition.PartitionedOperation;
import eu.europa.ec.eurostat.jgiscotools.algo.aggregation.BufferAggregation;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.util.JTSGeomUtil;

/**
 * @author julien Gaffuri
 *
 */
public class RailwayServiceAreasBufferDetection {
	public final static Logger LOGGER = LogManager.getLogger(RailwayServiceAreasBufferDetection.class.getName());

	//TODO do partitionning for large datasets

	private Collection<Feature> secs;

	private int quad = 5;
	private double bufferDistance = 12;
	private double shrinkingDistance = 3;
	private double sizeSel = bufferDistance*bufferDistance * 3;

	private Collection<Polygon> serviceAreas = null;
	public Collection<Polygon> getServiceAreas() { return serviceAreas; }

	private Collection<Polygon> doubleTrackAreas = null;
	public Collection<Polygon> getDoubleTrackAreas() { return doubleTrackAreas; }


	public RailwayServiceAreasBufferDetection(Collection<Feature> secs) {
		this.secs = secs;
	}

	public void compute() {

		if(LOGGER.isDebugEnabled()) LOGGER.debug("Buffer-union of all geometries");
		Geometry g = new BufferAggregation(bufferDistance, -1, quad, -1, false).aggregateGeometries(FeatureUtil.getGeometries(secs));
		if(LOGGER.isDebugEnabled()) LOGGER.debug("Buffer in");
		g = g.buffer(-bufferDistance);

		if(LOGGER.isDebugEnabled()) LOGGER.debug("Buffers for service areas");
		Geometry servArea = g.buffer(-shrinkingDistance, quad).buffer(shrinkingDistance*1.001, quad);
		if(LOGGER.isDebugEnabled()) LOGGER.debug("Difference for double tracks");

		Geometry doubleTrack = null;
		try {
			doubleTrack = g.difference(servArea);
		} catch (TopologyException e) {
			//TODO better
			doubleTrack = g.difference(servArea.buffer(bufferDistance * 0.001));
		}

		g = null;

		if(LOGGER.isDebugEnabled()) LOGGER.debug("Decomposition into polygons + filter by size");
		serviceAreas = JTSGeomUtil.getPolygons(servArea, sizeSel);
		servArea = null;
		doubleTrackAreas = JTSGeomUtil.getPolygons(doubleTrack, sizeSel);
		doubleTrack = null;

		if(LOGGER.isDebugEnabled()) LOGGER.debug("   nbAreas = " + serviceAreas.size() + "   nbDoubleTracks = " + doubleTrackAreas.size());
	}


	//compute with partitionning
	public void compute(int maxCoordinatesNumber, int objMaxCoordinateNumber) {

		serviceAreas = new ArrayList<Polygon>();
		doubleTrackAreas = new ArrayList<Polygon>();

		Partition.runRecursively(secs, new PartitionedOperation() {
			@Override
			public void run(Partition p) {
				LOGGER.info(p.toString());

				RailwayServiceAreasBufferDetection rsad = new RailwayServiceAreasBufferDetection(p.getFeatures());
				rsad.compute();

				serviceAreas.addAll(rsad.getServiceAreas());
				doubleTrackAreas.addAll(rsad.getDoubleTrackAreas());

			}}, maxCoordinatesNumber, objMaxCoordinateNumber, true, Partition.GeomType.ONLY_LINES, 0);
	}

}
