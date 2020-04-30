package eu.europa.ec.eurostat.jgiscotools.algo.deformationgael;

import org.locationtech.jts.geom.Coordinate;

public class SMTriangle extends GAELSubmicro {

	private GAELPoint pt1;
	public GAELPoint getPt1() { return this.pt1; }

	private GAELPoint pt2;
	public GAELPoint getPt2() { return this.pt2; }

	private GAELPoint pt3;
	public GAELPoint getPt3() { return this.pt3; }

	public SMTriangle(GAELPoint pt1, GAELPoint pt2, GAELPoint pt3){
		getPoints().add(pt1);
		getPoints().add(pt2);
		getPoints().add(pt3);

		//angle (p1,p2,p3) has to be direct direct. computes the vp.
		if ((pt2.getX()-pt1.getX())*(pt3.getY()-pt1.getY())-(pt2.getY()-pt1.getY())*(pt3.getX()-pt1.getX()) > 0) {this.pt1=pt1; this.pt2=pt2; this.pt3=pt3;}
		else {this.pt1=pt1; this.pt2=pt3; this.pt3=pt2;}

		pt1.getPointsRel().add(pt2);
		pt1.getPointsRel().add(pt3);
		pt2.getPointsRel().add(pt1);
		pt2.getPointsRel().add(pt3);
		pt3.getPointsRel().add(pt1);
		pt3.getPointsRel().add(pt2);
	}


	public double getArea() {
		double s = istReverted()? -1.0:1.0;
		return s*Math.abs(getPt2().getX()*getPt1().getY()-getPt1().getX()*getPt2().getY() + getPt3().getX()*getPt2().getY()-getPt2().getX()*getPt3().getY() + getPt1().getX()*getPt3().getY()-getPt3().getX()*getPt1().getY())*0.5;
	}

	private double iniArea = -999.9;
	public double getInitialArea() {
		if (this.iniArea == -999.9)
			this.iniArea = Math.abs(getPt2().getXIni()*getPt1().getYIni()-getPt1().getXIni()*getPt2().getYIni() + getPt3().getXIni()*getPt2().getYIni()-getPt2().getXIni()*getPt3().getYIni() + getPt1().getXIni()*getPt3().getYIni()-getPt3().getXIni()*getPt1().getYIni())*0.5;
		return this.iniArea;
	}

	public boolean contains(Coordinate c){
		if      ((getPt1().getX()-c.x)*(getPt2().getY()-c.y) - (getPt1().getY()-c.y)*(getPt2().getX()-c.x) <0) return false;
		else if	((getPt2().getX()-c.x)*(getPt3().getY()-c.y) - (getPt2().getY()-c.y)*(getPt3().getX()-c.x) <0) return false;
		else if	((getPt3().getX()-c.x)*(getPt1().getY()-c.y) - (getPt3().getY()-c.y)*(getPt1().getX()-c.x) <0) return false;
		else return true;
	}

	public boolean containsInitial(Coordinate c){
		if ((getPt1().getXIni()-c.x)*(getPt2().getYIni()-c.y)-(getPt1().getYIni()-c.y)*(getPt2().getXIni()-c.x) <0) return false;
		else if	((getPt2().getXIni()-c.x)*(getPt3().getYIni()-c.y) - (getPt2().getYIni()-c.y)*(getPt3().getXIni()-c.x) <0) return false;
		else if	((getPt3().getXIni()-c.x)*(getPt1().getYIni()-c.y) - (getPt3().getYIni()-c.y)*(getPt1().getXIni()-c.x) <0) return false;
		else return true;
	}

	public String getDirection() {
		double pv=(getPt2().getX()-getPt1().getX())*(getPt3().getY()-getPt1().getY())-(getPt2().getY()-getPt1().getY())*(getPt3().getX()-getPt1().getX());
		if (pv>0) return "d";
		else if (pv<0) return "i";
		else return "n";
	}

	public boolean istReverted() {
		return !getDirection().equals("d");
	}


	@Override
	public double getX(){
		return (getPt1().getX()+getPt2().getX()+getPt3().getX())/3.0;
	}

	@Override
	public double getY(){
		return (getPt1().getY()+getPt2().getY()+getPt3().getY())/3.0;
	}

	@Override
	public void clean(){
		super.clean();
		this.pt1 = null;
		this.pt2 = null;
		this.pt3 = null;
	}
}
