package eu.europa.ec.eurostat.jgiscotools.algo.deformation.base;

import java.util.ArrayList;
import java.util.HashSet;

import org.locationtech.jts.geom.Coordinate;

public class GDeformable {
	//private static Logger logger = Logger.getLogger(GDeformable.class.getName());

	public GDeformable(HashSet<GPoint> pts) {
		this.pts = pts;
	}

	private HashSet<GPoint> pts;
	public HashSet<GPoint> getPoints(Coordinate c) {
		HashSet<GPoint> pts_ = new HashSet<GPoint>();
		for(GPoint pt : pts) if(pt.getPosition().x == c.x && pt.getPosition().y == c.y ) pts_.add(pt);
		return pts_;
	}

	private ArrayList<GPoint> activationStack = new ArrayList<GPoint>();
	private Boolean isActivated = new Boolean(false);

	public void activatePoints(int max, double resolution) {

		synchronized (isActivated) {
			if(isActivated.booleanValue()) return;
			isActivated = new Boolean(true);
		}

		//if(logger.isLoggable(Level.FINE)) logger.fine("Load non balanced points of " + this);

		activationStack.clear();
		Coordinate c0 = new Coordinate(0,0);
		for (GPoint p : pts) {
			if( p.isFrozen() ) continue;
			Coordinate dis = p.getdisplacement();
			if(dis.distance(c0) > resolution * 0.5) {
				//if(logger.isLoggable(Level.FINE)) logger.fine(p + " added");
				activationStack.add(p);
			}
		}

		//if(logger.isLoggable(Level.FINE)) logger.fine("Activation of the points of " + this + " (" + activationStack.size() + " initial non-balanced points).");

		GPoint p;
		int i = 0;
		c0 = new Coordinate(0,0);
		while (true){
			if (activationStack.isEmpty()) {
				//if(logger.isLoggable(Level.FINE)) logger.fine("No more point to activate: deformation terminated.");
				isActivated = new Boolean(false);
				return;
			}

			if (max>0 && i>=max) {
				//if(logger.isLoggable(Level.FINE)) logger.fine("Limit number of activation reached: deformation stopped.");
				isActivated = new Boolean(false);
				return;
			}
			i++;

			//get random point
			p = activationStack.get((int)(activationStack.size()*Math.random()));
			//if(logger.isLoggable(Level.FINE)) logger.fine("Activation of " + p);

			if( p.isFrozen() ) continue;

			//get displacement
			Coordinate dis = p.getdisplacement();
			double length = dis.distance(c0);
			//if(logger.isLoggable(Level.FINE)) logger.fine("Displacement: " + dis.x + "," + dis.y + " - length=" + length);

			if(length < resolution*0.5) {
				//if(logger.isLoggable(Level.FINE)) logger.fine("point balanced");
				activationStack.remove(p);
			} else {
				//if(logger.isLoggable(Level.FINE)) logger.fine("point displaced");
				p.displace(dis);
				//compute new displacement length
				double length_ = p.getdisplacement().distance(c0);

				if(length_ > length) {
					//if(logger.isLoggable(Level.FINE)) logger.fine("   no improvement: go back and remove");
					p.displace( new Coordinate(-dis.x, -dis.y) );
					activationStack.remove(p);
				} else {
					//if(logger.isLoggable(Level.FINE)) logger.fine("   improvement: diffuse to neigbours");
					diffuse(p);
				}
			}
		}
	}

	//diffuse activation to the points in relation
	protected void diffuse(GPoint gp){
		for (GPoint p : gp.getPointsRel()) {
			if ( p.isFrozen() || activationStack.contains(p) ) continue;
			activationStack.add(p);
		}
	}


	public void clean(){
		deleteConstraints();

		for(GPoint p : pts)
			p.clean();
		if(activationStack != null) activationStack.clear();
		pts = null;
	}

	public void deleteConstraints(){
		for (GPoint gp : pts) if( gp.getConstraints() != null ) gp.getConstraints().clear();
	}

}
