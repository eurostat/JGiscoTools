package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class TestDuckDB {

	public static void main(String[] args) throws Throwable {
		System.out.println("Start");

		//see https://duckdb.org/docs/installation/
		//https://duckdb.org/docs/api/java.html

		Class.forName("org.duckdb.DuckDBDriver");
		Connection conn = DriverManager.getConnection("jdbc:duckdb:");

		// create a table
		Statement stmt = conn.createStatement();
		stmt.execute("CREATE TABLE items (item VARCHAR, value DECIMAL(10,2), count INTEGER)");
		// insert two items into the table
		stmt.execute("INSERT INTO items VALUES ('jeans', 20.0, 1), ('hammer', 42.2, 2)");


		//create table
		//boolean a = stmt.execute("CREATE TEMP TABLE mytable(name VARCHAR, id INTEGER, qt REAL)");
		//System.out.println(a);


		//"CREATE TEMP TABLE t1 AS SELECT * FROM read_csv_auto('/home/juju/Bureau/ex.csv');"

		//boolean a = stmt.execute("CREATE TEMP TABLE t AS SELECT * FROM read_csv_auto('/home/juju/Bureau/ex.csv');");
		//System.out.println(a);

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

		//Statement stmt2 = conn.createStatement();
		//boolean a2 = stmt2.execute("EXPORT DATABASE '/home/juju/Bureau/exp' (FORMAT PARQUET, CODEC 'GZIP')");
		//System.out.println(a2);


		//stmt2.close();
		conn.close();

		System.out.println("End");
	}

}
