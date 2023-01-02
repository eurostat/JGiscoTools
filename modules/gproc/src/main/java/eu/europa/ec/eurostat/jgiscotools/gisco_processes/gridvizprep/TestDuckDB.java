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
		ResultSet rs = stmt.executeQuery("SELECT 42");


		//create table
		/*
		CREATE TABLE weather (
			    city           VARCHAR,
			    temp_lo        INTEGER, -- minimum temperature on a day
			    temp_hi        INTEGER, -- maximum temperature on a day
			    prcp           REAL,
			    date           DATE
			);
		*/
		
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

		System.out.println("End");
	}

}
