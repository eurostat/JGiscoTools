/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation;

import org.locationtech.jts.operation.valid.IsValidOp;
import org.locationtech.jts.operation.valid.TopologyValidationError;

import eu.europa.ec.eurostat.jgiscotools.transfoengine.Constraint;

/**
 * @author julien Gaffuri
 *
 */
public class CUnitValidity  extends Constraint<AUnit> {
	//private final static Logger LOGGER = Logger.getLogger(CUnitValidity.class.getName());

	TopologyValidationError error = null;

	public CUnitValidity(AUnit agent) { super(agent); }

	@Override
	public void computeCurrentValue() {
		//LOGGER.info("CUnitValid "+getAgent().getObject().id);
		//valid = getAgent().getObject().getGeom().isValid();
		IsValidOp ivo = new IsValidOp( getAgent().getObject().getGeometry() );
		error = ivo.getValidationError();
	}

	@Override
	public void computeSatisfaction() {
		satisfaction = error==null? 10 : 0;
	}

	@Override
	public String getMessage(){
		return super.getMessage() + ( error!=null? ("," + error.getCoordinate().toString().replaceAll(",", ";") + "," + error.getMessage()) : ",no error" );
	}
}
