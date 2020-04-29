package eu.europa.ec.eurostat.jgiscotools.io.geo;

import java.util.Map;
import java.util.Map.Entry;

import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.util.CRSType;
import junit.framework.TestCase;

public class CRSTypeTest extends TestCase {

	/*public static void main(String[] args) {
		junit.textui.TestRunner.run(CRSTypeTest.class);
	}*/

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
				assertEquals(CRSUtil.getCRSType(crs), e.getValue());
			} catch (Exception e1) { e1.printStackTrace(); }
	}

}
