package eu.europa.ec.eurostat.eurogeostat.io.postgis;


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

}
