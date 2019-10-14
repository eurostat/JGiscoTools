/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.transfoengine;

import org.apache.log4j.Logger;

import eu.europa.ec.eurostat.eurogeostat.util.ProjectionUtil;
import eu.europa.ec.eurostat.eurogeostat.util.ProjectionUtil.CRSType;

/**
 * Threshold parameters for cartographic representations
 * 
 * @author julien Gaffuri
 *
 */
public class CartographicResolution {
	private final static Logger LOGGER = Logger.getLogger(CartographicResolution.class.getName());

	private double resolutionM;
	public double getResolutionM() { return resolutionM; }
	private double perceptionFilledPointSizeM;
	public double getPerceptionFilledPointSizeM() { return perceptionFilledPointSizeM; }
	private double perceptionFilledSquareSizeM;
	public double getPerceptionFilledSquareSizeM() { return perceptionFilledSquareSizeM; }
	private double perceptionPointSizeM;
	public double getPerceptionPointSizeM() { return perceptionPointSizeM; }
	private double perceptionSquareSizeM;
	public double getPerceptionSquareSizeM() { return perceptionSquareSizeM; }
	private double perceptionLineSizeM;
	public double getPerceptionLineSizeM() { return perceptionLineSizeM; }
	private double perceptionSizeSqMeter;
	public double getPerceptionSizeSqMeter() { return perceptionSizeSqMeter; }
	private double separationDistanceMeter;
	public double getSeparationDistanceMeter() { return separationDistanceMeter; }

	/**
	 * @param scaleDenominator The scale denominator. Ex: 1e6 for 1:1M scale.
	 */
	public CartographicResolution(double scaleDenominator, CRSType type) {

		//resolution is 0.1mm map. 0.1mm at 1:1M -> 1e-4*1e6 = 1e2 = 100m
		if(type == CRSType.CARTO) resolutionM = scaleDenominator*1e-4;
		else if(type == CRSType.GEOG) resolutionM = scaleDenominator*1e-4 / ProjectionUtil.ED;
		else {
			LOGGER.warn("Could not define resolution for CRS of type: "+type+". CRS is assumed to be in meters.");
			resolutionM = scaleDenominator*1e-4;
		}

		//0.2mm
		perceptionFilledPointSizeM = 2*resolutionM;
		//0.4mm
		perceptionFilledSquareSizeM = 4*resolutionM;
		//0.3mm
		perceptionPointSizeM = 3*resolutionM;
		//0.5mm
		perceptionSquareSizeM = 5*resolutionM;
		//0.1mm
		perceptionLineSizeM = 1*resolutionM;

		perceptionSizeSqMeter = 25 * resolutionM*resolutionM;

		//0.2mm
		separationDistanceMeter = 2*resolutionM;
	}

}
