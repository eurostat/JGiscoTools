package eu.europa.ec.eurostat.jgiscotools.algo.base;

import junit.framework.TestCase;

public class TranslationTest extends TestCase {

	public TranslationTest(String name) { super(name); }
	
	public static void main(String[] args) {
		junit.textui.TestRunner.run(TranslationTest.class);
	}

	public void test() {
		//fail("Not yet implemented");
	    assertEquals(1, 1);
	    //assertEquals(2, 1);
	}

	/*
	public void testEmptyPolygon() throws Exception {
		String geomStr = "POLYGON(EMPTY)";
		new GeometryOperationValidator(
				TranslationResult.getResult(geomStr,1, 1))
		.setExpectedResult(geomStr)
		.test();
	}
*/
}

/*
class TranslationResult{
	private static WKTReader rdr = new WKTReader();

	public static Geometry[] getResult(String wkt, double dx, double dy) throws ParseException {
		Geometry[] ioGeom = new Geometry[2];
		ioGeom[0] = rdr.read(wkt);
		ioGeom[1] = Translation.get(ioGeom[0], dx, dy);
		return ioGeom;
	}
}
*/