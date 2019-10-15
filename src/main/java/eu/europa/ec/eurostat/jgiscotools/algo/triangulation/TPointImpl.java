package eu.europa.ec.eurostat.jgiscotools.algo.triangulation;

import org.locationtech.jts.geom.Coordinate;

public class TPointImpl implements TPoint {

	private Coordinate position;
	public Coordinate getPosition() { return this.position; }

	public TPointImpl(Coordinate position) {
		this.position = position;
	}

}
