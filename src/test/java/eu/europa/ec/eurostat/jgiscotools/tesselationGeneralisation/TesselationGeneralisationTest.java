package eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation;

import junit.framework.TestCase;

public class TesselationGeneralisationTest extends TestCase {

	/*public static void main(String[] args) throws Exception {
		junit.textui.TestRunner.run(TesselationGeneralisationTest.class);
	}*/

	public void test() {
		/*for(String cntr : new String[] {"bangladesh", "chile", "china_mainland", "indonesia", "panama", "philippines"})
			for(int scaleM : new int[] {1, 5, 10, 20, 50}) {
				System.out.println(" *** " + cntr + " - scaleM: " + scaleM);

				String inFile = "src/test/resources/tessgene/cntr/" + cntr + ".shp";
				Collection<Feature> units = GeoData.getFeatures(inFile);

				CRSType crsType = GeoData.getCRSType(inFile);
				int roundNb = 10;
				int maxCoordinatesNumber = 1000000;
				int objMaxCoordinateNumber = 1000;
				boolean parallel = true;
				units = TesselationGeneralisation.runGeneralisation(units, null, crsType, scaleM * 1e6, parallel, roundNb, maxCoordinatesNumber, objMaxCoordinateNumber);

				String outFile = "target/testout/tessgene/"+cntr+"_"+scaleM+".gpkg";
				GeoData.save(units, outFile, GeoData.getCRS(inFile));
			}*/
	}

}
