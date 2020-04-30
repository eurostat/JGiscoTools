package eu.europa.ec.eurostat.jgiscotools.algo.deformationgael;

import java.util.ArrayList;
import java.util.HashSet;

import org.locationtech.jts.geom.Coordinate;

public class GAELDeformable {
	//private static Logger logger = Logger.getLogger(GDeformable.class.getName());

	public GAELDeformable(HashSet<GAELPoint> pts) {
		this.pts = pts;
	}

	private HashSet<GAELPoint> pts;
	public HashSet<GAELPoint> getPoints(Coordinate c) {
		HashSet<GAELPoint> pts_ = new HashSet<GAELPoint>();
		for(GAELPoint pt : pts) if(pt.getPosition().x == c.x && pt.getPosition().y == c.y ) pts_.add(pt);
		return pts_;
	}

	private ArrayList<GAELPoint> activationStack = new ArrayList<GAELPoint>();
	private Boolean isActivated = false;

	public void activatePoints(int max, double resolution) {

		synchronized (isActivated) {
			if(isActivated) return;
			isActivated = true;
		}

		//if(logger.isLoggable(Level.FINE)) logger.fine("Load non balanced points of " + this);

		activationStack.clear();
		Coordinate c0 = new Coordinate(0,0);
		for (GAELPoint p : pts) {
			if( p.isFrozen() ) continue;
			Coordinate dis = p.getdisplacement();
			if(dis.distance(c0) > resolution * 0.5) {
				//if(logger.isLoggable(Level.FINE)) logger.fine(p + " added");
				activationStack.add(p);
			}
		}

		//if(logger.isLoggable(Level.FINE)) logger.fine("Activation of the points of " + this + " (" + activationStack.size() + " initial non-balanced points).");

		GAELPoint p;
		int i = 0;
		c0 = new Coordinate(0,0);
		while (true){
			if (activationStack.isEmpty()) {
				//if(logger.isLoggable(Level.FINE)) logger.fine("No more point to activate: deformation terminated.");
				isActivated = false;
				return;
			}

			if (max>0 && i>=max) {
				//if(logger.isLoggable(Level.FINE)) logger.fine("Limit number of activation reached: deformation stopped.");
				isActivated = false;
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
	protected void diffuse(GAELPoint gp){
		for (GAELPoint p : gp.getPointsRel()) {
			if ( p.isFrozen() || activationStack.contains(p) ) continue;
			activationStack.add(p);
		}
	}


	public void clean(){
		deleteConstraints();

		for(GAELPoint p : pts)
			p.clean();
		if(activationStack != null) activationStack.clear();
		pts = null;
	}

	public void deleteConstraints(){
		for (GAELPoint gp : pts) if( gp.getConstraints() != null ) gp.getConstraints().clear();
	}

}
