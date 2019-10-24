package eu.europa.ec.eurostat.jgiscotools.io.postgis;

import java.sql.Connection;
import java.sql.Statement;

public class PGUtil {

	//type: integer,text,etc.
	public static boolean addColumn(PGConnection pgc, String tabName, String columnName, String columnType){
		try {
			return pgc.getConnection().createStatement().execute( "ALTER TABLE "+tabName+" ADD COLUMN "+columnName+" "+columnType+";" );
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static boolean createIndex(PGConnection pgc, String tabName, String columnName, String indexName){
		try {
			return pgc.getConnection().createStatement().execute("CREATE INDEX "+indexName+" ON "+tabName+" ("+columnName+" ASC NULLS LAST);");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}




	public static boolean deleteDuplicates(Connection c, String table, double xMin, double xMax, double yMin, double yMax) {
		boolean b = false;
		try {
			Statement st = c.createStatement();
			String qu = "DELETE FROM " + table + " WHERE "
					+ "st_intersects(geom,ST_MakeEnvelope("+xMin+","+yMin+","+xMax+","+yMax+",3035)) "
					+ "AND "
					+ "gid NOT IN ("
					+ "select max(dup.gid) from "+table+" as dup WHERE st_intersects(geom,ST_MakeEnvelope("+xMin+","+yMin+","+xMax+","+yMax+",3035)) group by geom"
					+ ");";
			System.out.println(qu);
			try { b = st.execute(qu); } catch (Exception e) { e.printStackTrace(); }
			finally { st.close(); }
		} catch (Exception e) { e.printStackTrace(); }
		return b;
	}

}
