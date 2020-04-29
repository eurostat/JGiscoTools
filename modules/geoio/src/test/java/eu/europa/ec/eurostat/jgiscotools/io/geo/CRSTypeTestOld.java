package eu.europa.ec.eurostat.jgiscotools.io.geo;

import junit.framework.TestCase;

public class CRSTypeTestOld extends TestCase {

	/*public static void main(String[] args) {
		junit.textui.TestRunner.run(CRSTypeTest2.class);
	}*/

/*
	private static CRSType getCRSType(CoordinateReferenceSystem crs) {
		Unit<?> unit = CRSUtilities.getUnit(crs.getCoordinateSystem());
		if(unit == null) return CRSType.UNKNOWN;
		switch (unit.toString()) {
		case "": return CRSType.UNKNOWN;
		case "°": return CRSType.GEOG;
		case "deg": return CRSType.GEOG;
		case "dms": return CRSType.GEOG;
		case "degree": return CRSType.GEOG;
		case "m": return CRSType.CARTO;
		default:
			System.err.println("Unexpected unit of measure for projection: "+unit);
			return CRSType.UNKNOWN;
		}
	}

	private Map<String, CRSType> testData = Map.of(
			"4326", CRSType.GEOG,
			"4258", CRSType.GEOG,
			"27700", CRSType.CARTO,
			"5243", CRSType.CARTO,
			"5673", CRSType.CARTO,
			"4087", CRSType.CARTO,
			"26956", CRSType.CARTO,
			"8857", CRSType.CARTO,
			"3035", CRSType.CARTO,
			"3857", CRSType.CARTO
			);

	public void test() {
		for(Entry<String, CRSType> e : testData.entrySet())
			try {
				CoordinateReferenceSystem crs = CRS.decode("EPSG:"+e.getKey());
				assertEquals(getCRSType(crs), e.getValue());
			} catch (Exception e1) { e1.printStackTrace(); }
	}
*/
}
