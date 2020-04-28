package eu.europa.ec.eurostat.jgiscotools.algo.deformationgael;

import java.util.ArrayList;

public class GAELSubmicro {

	private ArrayList<GAELPoint> points = new ArrayList<GAELPoint>();
	public ArrayList<GAELPoint> getPoints() { return this.points; }

	public double getX() {
		double x = 0.0;
		for(GAELPoint p : points) x += p.getX();
		return x / points.size();
	}

	public double getY() {
		double y = 0.0;
		for(GAELPoint p : points) y += p.getY();
		return y / points.size();
	}

	public void clean(){
		points.clear();
	}

}
