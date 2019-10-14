package eu.europa.ec.eurostat.eurogeostat.algo.triangulation;

public class TTriangleFactoryImpl implements TTriangleFactory {

	public TTriangle create(TPoint p1, TPoint p2, TPoint p3) {
		return new TTriangleImpl(p1, p2, p3);
	}

}
