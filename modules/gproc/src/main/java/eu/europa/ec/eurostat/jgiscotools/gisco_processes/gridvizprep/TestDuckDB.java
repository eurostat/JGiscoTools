package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import eu.europa.ec.eurostat.jgiscotools.ParquetUtil;

public class TestDuckDB {

	public static void main(String[] args) throws Throwable {
		System.out.println("Start");

		ParquetUtil.convertCSVToParquet("/home/juju/Bureau/ex.csv", "/home/juju/Bureau/", "ex", "GZIP");


		/*
		//see https://duckdb.org/docs/installation/
		//https://duckdb.org/docs/api/java.html

		Class.forName("org.duckdb.DuckDBDriver");
		Connection conn = DriverManager.getConnection("jdbc:duckdb:");

		// create a table
		Statement stmt = conn.createStatement();
		//stmt.execute("CREATE TABLE items (item VARCHAR, value DECIMAL(10,2), count INTEGER)");
		// insert two items into the table
		//stmt.execute("INSERT INTO items VALUES ('jeans', 20.0, 1), ('hammer', 42.2, 2)");

		/*/
		/*ResultSet rs = stmt.executeQuery("SELECT * FROM items");
		while (rs.next()) {
			System.out.println(rs.getString(1));
			System.out.println(rs.getInt(3));
		}
		rs.close();
		 */

		//CSV loading
		//https://duckdb.org/docs/data/overview
		//https://duckdb.org/docs/data/csv

		//stmt.execute("CREATE TABLE ex AS SELECT * FROM read_csv_auto('/home/juju/Bureau/ex.csv', delim=',', header=True)");
		//stmt.execute("COPY ex FROM '/home/juju/Bureau/ex.csv' (FORMAT 'csv', delimiter ',', header 1)");
		//COPY items FROM '/home/juju/Bureau/exp/items.csv' (FORMAT 'csv', quote '"', delimiter ',', header 0);

		/*
		ResultSet rs = stmt.executeQuery("SELECT * FROM ex");
		while (rs.next()) {
			//x,y,CNTR_ID,pop2006,pop2011,pop2018
			System.out.println(rs.getInt(1) + "   " + rs.getInt(2) +"   "+ rs.getString(3));
		}
		rs.close();
		 */

		//export as parquet
		//see https://observablehq.com/@observablehq/csv-to-parquet
		//client.query(`EXPORT DATABASE '/tmp/duckdbexportparquet' (FORMAT 'parquet', CODEC 'GZIP')`);
		//db.query(`COPY (${modifySQL}) TO 'modified.parquet' (FORMAT 'parquet', CODEC 'GZIP')`)

		//stmt.execute("EXPORT DATABASE '/home/juju/Bureau/exp' (FORMAT PARQUET, CODEC 'GZIP')");
		//stmt.execute("EXPORT DATABASE '/home/juju/Bureau/exp' (FORMAT CSV, delim ',', header True)");
		//System.out.println(a2);


		//stmt2.close();
		//conn.close();

		System.out.println("End");
	}

}
