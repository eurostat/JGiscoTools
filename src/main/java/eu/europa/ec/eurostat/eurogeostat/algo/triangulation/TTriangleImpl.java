package eu.europa.ec.eurostat.eurogeostat.algo.triangulation;

public class TTriangleImpl implements TTriangle {

	private TPoint pt1;
	public TPoint getPt1() { return this.pt1; }

	private TPoint pt2;
	public TPoint getPt2() { return this.pt2; }

	private TPoint pt3;
	public TPoint getPt3() { return this.pt3; }

	public TTriangleImpl(TPoint pt1, TPoint pt2, TPoint pt3) {
		this.pt1 = pt1;
		this.pt2 = pt2;
		this.pt3 = pt3;
	}

}
