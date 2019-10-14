package eu.europa.ec.eurostat.eurogeostat.algo.deformation.submicro;

import eu.europa.ec.eurostat.eurogeostat.algo.deformation.base.GPoint;
import eu.europa.ec.eurostat.eurogeostat.algo.deformation.base.Submicro;

public class GSinglePoint extends Submicro {

	private GPoint pt;
	public GPoint getPoint() { return this.pt; }

	public GSinglePoint(GPoint pt){
		this.getPoints().add(pt);
		this.pt = pt;
	}

	@Override
	public double getX(){
		return getPoint().getX();
	}
	@Override
	public double getY(){
		return getPoint().getY();
	}

	@Override
	public void clean(){
		super.clean();
		this.pt = null;
	}

}
