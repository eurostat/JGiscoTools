/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.grid;

import eu.europa.ec.eurostat.jgiscotools.grid.Grid;
import eu.europa.ec.eurostat.jgiscotools.grid.GridCell.GridCellGeometryType;
import junit.framework.TestCase;

/**
 * @author Julien Gaffuri
 *
 */
public class StatGridTest extends TestCase {

	public static void main(String[] args) {
		junit.textui.TestRunner.run(StatGridTest.class);
	}

	public void test1() throws Exception {
		Grid sg = new Grid();

		//SHPUtil.saveSHP(sg.getCells(), "C:/Users/gaffuju/Desktop/test.shp", CRS.decode("EPSG:3035"));

		assertEquals("3035", sg.getEPSGCode());
		assertEquals(100000.0, sg.getResolution());
		assertEquals(0.0, sg.getToleranceDistance());
		assertEquals(GridCellGeometryType.SURFACE, sg.getGridCellGeometryType());
		assertEquals(10201, sg.getCells().size());
	}

	public void test2() throws Exception {
		Grid sg = new Grid();
		sg.setEPSGCode("1464412");
		sg.setResolution(50000);
		sg.setToleranceDistance(500000.0);
		sg.setGridCellGeometryType(GridCellGeometryType.CENTER_POINT);

		//SHPUtil.saveSHP(sg.getCells(), "C:/Users/gaffuju/Desktop/test.shp", CRS.decode("EPSG:3035"));

		assertEquals("1464412", sg.getEPSGCode());
		assertEquals(50000.0, sg.getResolution());
		assertEquals(500000.0, sg.getToleranceDistance());
		assertEquals(GridCellGeometryType.CENTER_POINT, sg.getGridCellGeometryType());
		assertEquals(48748, sg.getCells().size());
	}

}
