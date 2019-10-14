package eu.europa.ec.eurostat.eurogeostat.algo.triangulation;

public class TSegmentFactoryImpl implements TSegmentFactory {

	public TSegment create(TPoint point1, TPoint point2) {
		return new TSegmentImpl(point1, point2);
	}

}
