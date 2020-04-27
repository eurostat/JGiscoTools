package eu.europa.ec.eurostat.jgiscotools.algo.measure;

import org.locationtech.jts.algorithm.MinimumDiameter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

public class Orientation {

	//orientation number to test within [0, Pi/2[ for the sides mean orientation computation
	private static int TESTED_ORIENTATIONS_NB = 30;

	private Geometry geom = null;

	public Orientation(Geometry geom){
		this.geom = geom;
	}

	private double[] votes = null;
	public double[] getVotes() {
		if (this.votes==null) computeVotes((Polygon)this.geom);
		return this.votes;
	}

	private double sidesOrientation = -999.9;
	private double maxVote = -999.9;

	/**
	 * @return The sides orientation measure (see [Duchene et al 2003])
	 */
	public double getSidesOrientation() {
		if (this.sidesOrientation == -999.9) computeSidesOrientation();
		return this.sidesOrientation ;
	}

	private double sidesOrientationIndicator=-999.9;
	public double getSidesOrientationIndicator() {
		if (this.sidesOrientationIndicator == -999.9) computeSidesOrientationIndicator();
		return this.sidesOrientationIndicator;
	}

	/**
	 * Geometry orientation (in radian, within [0,Pi], from (Ox) axis),computed from the smallest surrounding rectangle orientation.
	 * Returns 999.9 if the SSR is not defined or if it is square.
	 * 
	 * @return
	 */
	public double getGeneralOrientation(){

		//get ssr
		//Polygon ssr = SmallestSurroundingRectangle.get(this.geom);
		Polygon ssr = (Polygon) new MinimumDiameter(this.geom).getMinimumRectangle();

		if (ssr == null)
			return 999.9;

		//get longest side
		Coordinate[] coords = ssr.getCoordinates();
		double lg1 = coords[0].distance(coords[1]);
		double lg2 = coords[1].distance(coords[2]);
		if (lg1==lg2) return 999.9;

		//orientation is (c1,c2)
		Coordinate c1,c2;
		if (lg1>lg2) { c1=coords[0]; c2=coords[1]; }
		else { c1=coords[1]; c2=coords[2]; }

		//computes longest side orientation
		double angle = Math.atan((c1.y-c2.y)/(c1.x-c2.x));
		if (angle<0) angle += Math.PI;
		return angle;
	}

	/**
	 * computes the sides orientation
	 * (in radian, within [0, Pi/2[, from (Ox) axis)
	 */
	private void computeSidesOrientation(){

		//compute side votes
		if (this.votes==null) computeVotes();

		//get the index of the maximum vote
		int iMax = 0;
		double vote = this.votes[iMax];
		this.maxVote = vote;
		for(int i=1; i<this.votes.length; i++) {
			vote = this.votes[i];
			if (vote > this.maxVote) {
				this.maxVote = vote;
				iMax=i;
			}
		}

		//returns the corresponding angle
		this.sidesOrientation = 0.5*Math.PI*iMax/TESTED_ORIENTATIONS_NB;
	}

	private void computeVotes(){
		if (this.geom instanceof Polygon) computeVotes( (Polygon)this.geom );
		else if (this.geom instanceof LineString) computeVotes( (LineString)this.geom );
		else {
			this.sidesOrientation =-999.9;
			return;
		}
	}

	private void computeVotes(Polygon poly){
		//initialisation of the votes table
		this.votes = new double[TESTED_ORIENTATIONS_NB];
		for(int i=0; i<TESTED_ORIENTATIONS_NB; i++) this.votes[i] = 0.0;

		//add votes of the exterior ring
		addVotes(poly.getExteriorRing());

		//add votes of the interior rings
		for(int i=0; i<poly.getNumInteriorRing(); i++) addVotes(poly.getInteriorRingN(i));
	}

	private void computeVotes(LineString ls){
		//initialisation of the votes table
		this.votes = new double[TESTED_ORIENTATIONS_NB];
		for(int i=0; i<TESTED_ORIENTATIONS_NB; i++) this.votes[i] = 0.0;

		//add votes
		addVotes(ls);
	}

	/**
	 * Computes the votes of each side for each tested orientation.
	 * Each vote depends to the length of the segment and the difference between its orientation and the tested orientation
	 * 
	 * @param ls
	 */
	private void addVotes(LineString ls){
		double orientation, lg, delta;
		int index;

		//go through the sides and compute its vote
		Coordinate[] coord = ls.getCoordinates();
		Coordinate c1 = coord[0], c2;
		double pasOrientation = Math.PI*0.5/TESTED_ORIENTATIONS_NB;
		for(int i=1; i<coord.length; i++) {
			c2 = coord[i];

			//orientation mod PI/2 from c1 to c2
			if (c1.x==c2.x) orientation=0.0; else orientation=Math.atan( ((c1.y-c2.y)/(c1.x-c2.x)) );
			if (orientation<0) orientation+=0.5*Math.PI;

			//get the smallest angle index for which the side vote
			index = (int) (orientation/pasOrientation);

			//add vote
			lg = c1.distance(c2);
			delta = orientation/pasOrientation - index;
			this.votes[index]+=lg*(1-delta);
			if (index+1 == TESTED_ORIENTATIONS_NB) this.votes[0]+=lg*delta; else this.votes[index+1]+=lg*delta;

			//next
			c1=c2;
		}
	}

	private void computeSidesOrientationIndicator() {
		if (this.sidesOrientation == -999.9) computeSidesOrientation();
		this.sidesOrientationIndicator = this.maxVote / this.geom.getLength();
	}

}
