package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridaggregation;

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
}
