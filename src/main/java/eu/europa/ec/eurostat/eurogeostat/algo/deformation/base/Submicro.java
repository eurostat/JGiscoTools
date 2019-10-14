package eu.europa.ec.eurostat.eurogeostat.algo.deformation.base;

import java.util.ArrayList;

public class Submicro {

	private ArrayList<GPoint> points = new ArrayList<GPoint>();
	public ArrayList<GPoint> getPoints() { return this.points; }

	public double getX() {
		double x = 0.0;
		for(GPoint p : points) x += p.getX();
		return x / points.size();
	}

	public double getY() {
		double y = 0.0;
		for(GPoint p : points) y += p.getY();
		return y / points.size();
	}

	public void clean(){
		points.clear();
	}

}
