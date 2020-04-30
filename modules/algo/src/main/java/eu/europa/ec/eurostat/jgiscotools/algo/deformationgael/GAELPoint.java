package eu.europa.ec.eurostat.jgiscotools.algo.deformationgael;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.locationtech.jts.geom.Coordinate;

public class GAELPoint {
	private static Logger logger = Logger.getLogger(GAELPoint.class.getName());

	public GAELPoint(Coordinate dp) {
		getCoordinates().add(dp);
		this.cIni = new Coordinate(dp.getX(), dp.getY(), dp.getZ());
	}

	private ArrayList<Coordinate> coordinates = new ArrayList<Coordinate>();
	public ArrayList<Coordinate> getCoordinates() {return this.coordinates;}
	public Coordinate getPosition() {return this.coordinates.get(0);}

	public double getX(){ return getCoordinates().get(0).x; }
	public void setX(double x){ for(Coordinate c : getCoordinates()) c.x = x; }
	public double getY(){ return getCoordinates().get(0).y; }
	public void setY(double y){ for(Coordinate c : getCoordinates()) c.y = y; }

	private Coordinate cIni;
	public Coordinate getInitialPosition() { return this.cIni; }
	public double getXIni() { return getInitialPosition().x; }
	public double getYIni() { return getInitialPosition().y; }

	private ArrayList<GAELConstraint> constraints = new ArrayList<GAELConstraint>();
	public ArrayList<GAELConstraint> getConstraints() { return this.constraints; }

	//the points in relation (belonging to the same sm)
	private ArrayList<GAELPoint> prs = new ArrayList<GAELPoint>();
	public ArrayList<GAELPoint> getPointsRel() { return this.prs; }

	private boolean frozen = false;
	public boolean isFrozen() {return this.frozen;}
	public void setFrozen(boolean f) {this.frozen = f;}


	//the total displacement returned by the constraints
	public Coordinate getdisplacement() {
		Coordinate dis = new Coordinate(0,0);
		Coordinate c;
		for(GAELConstraint gc : getConstraints()) {
			c = gc.getDisplacement(this);
			if(c==null) {
				logger.severe(gc.getClass().getSimpleName() + " has returned a null displacement.");
				continue;
			}
			if(logger.isLoggable(Level.FINEST)) logger.log(Level.FINEST, "  Constraint: " + gc.getClass().getSimpleName() + " - displacment: (" + c.x + ", " + c.y + ")");
			dis.x += c.x;
			dis.y += c.y;
		}
		return dis;
	}

	double is = -1.0;
	public double getImportanceSum() {
		if( this.is == -1.0 ) {
			this.is = 0.0;
			for(GAELConstraint gc : getConstraints())this.is += gc.getImportance();
		}
		return this.is;
	}



	//displace the point
	public void displace(Coordinate dis) {
		setX( getX()+dis.x );
		setY( getY()+dis.y );
	}

	public double getDistance(double x_,double y_) { return Math.hypot(getX()-x_, getY()-y_);}
	public double getDistance(GAELPoint p) { return getDistance(p.getX(),p.getY());}

	public double getIniDistance(double x_,double y_) { return Math.sqrt((getXIni()-x_)*(getXIni()-x_)+(getYIni()-y_)*(getYIni()-y_));}
	public double getIniDistance(GAELPoint p) { return getIniDistance(p.getXIni(),p.getYIni());}

	public double getDistanceToInitialPosition() { return Math.sqrt((getXIni()-getX())*(getXIni()-getX())+(getYIni()-getY())*(getYIni()-getY()));}

	//within [-Pi,Pi]
	public double getOrientation(GAELPoint p) {
		return Math.atan2(p.getY()-getY(),p.getX()-getX());
	}
	public double getIniOrientation(GAELPoint p) {	return Math.atan2(p.getYIni()-getYIni(),p.getXIni()-getXIni()); }

	public double getOrientationGap(GAELPoint p) {
		double ecart=getOrientation(p)-getIniOrientation(p);
		if (ecart<-Math.PI) return ecart+2.0*Math.PI;
		else if (ecart>Math.PI) return ecart-2.0*Math.PI;
		else return ecart;
	}

	public void clean() {
		if( getConstraints() != null ) getConstraints().clear();
		getPointsRel().clear();
	}

}
