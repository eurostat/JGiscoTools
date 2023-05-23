package eu.europa.ec.eurostat.jgiscotools.gisco_processes.buildingstats;

/**
 * @author gaffuju
 *
 */
public class BuildingStat {
	double res = 0;
	double agri = 0;
	double indus = 0;
	double commServ = 0;

	public BuildingStat() {
		this(0,0,0,0);
	}

	public BuildingStat(double res, double agri, double indus, double commServ) {
		this.res = res;
		this.agri = agri;
		this.indus = indus;
		this.commServ = commServ;
	}

	public boolean isValid() {
		return this.res>=0 && this.agri>=0 && this.indus>=0 && this.commServ>=0;
	}

	@Override
	public String toString() {
		return "(" + this.res + ", " + this.agri + ", " + this.indus + ", " + this.commServ + ")";
	}
}
