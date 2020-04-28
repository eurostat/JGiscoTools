package eu.europa.ec.eurostat.jgiscotools.algo.deformationgael;

public class SMSinglePoint extends GAELSubmicro {

	private GAELPoint pt;
	public GAELPoint getPoint() { return this.pt; }

	public SMSinglePoint(GAELPoint pt){
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
