package eu.ec.estat.geostat.io.postgis;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class PGUtil {

	public static Collection<String> getTableNames(Connection c){ return getTableNames(c,true,true); }
	public static Collection<String> getTableNames(Connection c, boolean onlyPublicTables, boolean onlyBaseTables){
		Collection<String> col = new HashSet<String>();
		try {
			Statement st = c.createStatement();
			try {
				ResultSet res = st.executeQuery("SELECT table_name FROM information_schema.tables WHERE 1=1"+(onlyPublicTables?" AND table_schema='public'":"")+(onlyBaseTables?" AND table_type='BASE TABLE'":""));
				while (res.next()) col.add(res.getString(1));
			} finally { st.close(); }
		} catch (Exception e) { e.printStackTrace(); }
		return col;
	}

	public static int getTableSize(Connection c, String tableName){
		int s = -1;
		try {
			Statement st = c.createStatement();
			try {
				ResultSet res = st.executeQuery("SELECT count(*) FROM "+tableName);
				res.next();
				s = res.getInt(1);
			} finally { st.close(); }
		} catch (Exception e) { e.printStackTrace(); }
		return s;
	}

	public static int getSRID(Connection c, String tableName, String geomColumnName){
		int s = -1;
		try {
			Statement st = c.createStatement();
			try {
				ResultSet res = st.executeQuery("SELECT Find_SRID('public', '"+tableName+"', '"+geomColumnName+"');");
				res.next();
				s = res.getInt(1);
			} finally { st.close(); }
		} catch (Exception e) { e.printStackTrace(); }
		return s;
	}

	//get all values of attribute
	public static ArrayList<String> getValues(Connection c, String tableName, String columnName, boolean distinct){ return getValues(c, tableName, columnName, distinct, null); }
	public static ArrayList<String> getValues(Connection c, String tableName, String columnName, boolean distinct, String where){
		ArrayList<String> col = new ArrayList<String>();
		try {
			Statement st = c.createStatement();
			try {
				ResultSet res = st.executeQuery("SELECT "+(distinct?"DISTINCT ":"")+columnName+" FROM "+tableName+(where!=null?" WHERE "+where:"")+";");
				while (res.next()) col.add(res.getObject(1).toString());
			} finally { st.close(); }
		} catch (Exception e) { e.printStackTrace(); }
		return col;
	}


	public static boolean executeStatement(Connection c, String sqlStatment){
		boolean b = false;
		try {
			Statement st = c.createStatement();
			try { b = st.execute(sqlStatment); }
			finally { st.close(); }
		} catch (Exception e) { e.printStackTrace(); }
		return b;
	}

	public static String getGeometryText(Connection c, String tableName, String id) { return getGeometryText(c, tableName, id, "gid", "geom"); }
	public static String getGeometryText(Connection c, String tableName, String id, String idColumn, String geomColumn) {
		String g = null;
		try {
			Statement st = c.createStatement();
			try {
				ResultSet res = st.executeQuery("SELECT ST_AsText("+geomColumn+") FROM "+tableName+" WHERE "+idColumn+"='"+id+"';");
				if(res.next()) g = res.getString(1);
			} finally { st.close(); }
		} catch (Exception e) { e.printStackTrace(); }
		return g;
	}

	//type: integer,text,etc.
	public static boolean addColumn(Connection c, String tableName, String columnName, String columnType){
		return executeStatement(c, "ALTER TABLE "+tableName+" ADD COLUMN "+columnName+" "+columnType+";");
	}

	public static boolean createIndex(Connection c, String tabName, String columnName){ return createIndex(c, tabName, columnName, "INDEX_"+tabName+"_"+columnName); }
	public static boolean createIndex(Connection c, String tabName, String columnName, String indexName){
		return executeStatement(c, "CREATE INDEX "+indexName+" ON "+tabName+" ("+columnName+" ASC NULLS LAST);");
	}

	public static boolean createSpatialIndex(Connection c, String tabName){ return createSpatialIndex(c, tabName, "geom", "SPATIAL_INDEX_"+tabName+"_geom"); }
	public static boolean createSpatialIndex(Connection c, String tabName, String geomColumnName, String indexName){
		return executeStatement(c, "CREATE INDEX "+indexName+" ON "+tabName+" USING GIST ("+geomColumnName+");");
	}

}
