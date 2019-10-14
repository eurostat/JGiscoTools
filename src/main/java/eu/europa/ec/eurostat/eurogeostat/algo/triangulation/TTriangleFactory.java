package eu.europa.ec.eurostat.eurogeostat.algo.triangulation;

public interface TTriangleFactory {

	public TTriangle create(TPoint pt1, TPoint pt2, TPoint pt3);

}
