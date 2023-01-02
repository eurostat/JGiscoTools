package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestDuckDB {

	public static void main(String[] args) throws Throwable {
		System.out.println("Start");

		//see https://duckdb.org/docs/installation/

		//Class.forName("org.duckdb.DuckDBDriver");
		Connection conn = DriverManager.getConnection("jdbc:duckdb:");
		Statement stmt = conn.createStatement();
		//ResultSet rs = stmt.executeQuery("SELECT 42");


		//create table
		//boolean a = stmt.execute("CREATE TEMP TABLE mytable(name VARCHAR, id INTEGER, qt REAL)");
		//System.out.println(a);


		//"CREATE TEMP TABLE t1 AS SELECT * FROM read_csv_auto('/home/juju/Bureau/ex.csv');"

		boolean a = stmt.execute("CREATE TEMP TABLE t AS SELECT * FROM read_csv_auto('/home/juju/Bureau/ex.csv');");
		System.out.println(a);

		//CREATE TABLE csv_file(Choose STRING, a STRING, file STRING)
		///home/juju/Bureau/ex.csv

		//load csv
		//COPY weather FROM '/home/user/weather.csv';
		//client.query(`CREATE TABLE csv_file(Choose STRING, a STRING, file STRING)`);
		//javascript: client.insertCSV("csv_file", buffer, {})


		//query
		//SELECT * FROM weather;

		/*
		while (rs.next()) {
			System.out.println(rs);
		}*/

		//export as parquet
		//see https://observablehq.com/@observablehq/csv-to-parquet
		//client.query(`EXPORT DATABASE '/tmp/duckdbexportparquet' (FORMAT 'parquet', CODEC 'GZIP')`);
		//db.query(`COPY (${modifySQL}) TO 'modified.parquet' (FORMAT 'parquet', CODEC 'GZIP')`)

		Statement stmt2 = conn.createStatement();
		boolean a2 = stmt.execute("EXPORT DATABASE '/home/juju/Bureau/exp' (FORMAT 'parquet', CODEC 'GZIP')");
		System.out.println(a2);


		stmt.close();
		conn.close();

		System.out.println("End");
	}

}
