package eu.ec.estat.geostat.io.postgis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.StringTokenizer;

/**
 * Import/export CSV data into/from a PG database.
 * 
 * @author julien Gaffuri
 *
 */
public class CSVPostGISImportExport {

	public static void importCSV(PGConnection pgc, String inputFilePath, String separator, String tabName, boolean withOIDS, boolean replaceQuotationMark, boolean replacePoint, boolean finalVacuum, boolean saveCSVLine, String csvLine) {
		try {
			Statement st = pgc.getConnection().createStatement();

			//delete previous table (if any) and create new table
			try { st.execute("DROP TABLE "+tabName+";"); } catch (Exception e) {}
			st.execute("CREATE TABLE "+tabName+"()WITH (OIDS=" + (withOIDS?"TRUE":"FALSE") + ");");

			//open file
			BufferedReader br = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(inputFilePath))));

			//add columns
			String line=br.readLine();
			StringTokenizer sto = new StringTokenizer(line, separator);
			int nbCol=sto.countTokens();
			while(sto.hasMoreTokens()){
				String col=sto.nextToken();
				if(replaceQuotationMark) col=col.replace("\"","");
				if(replacePoint) col=col.replace(".","");
				st.execute( "ALTER TABLE "+tabName+" ADD COLUMN "+col+" text;" );
			}
			if(saveCSVLine) st.execute( "ALTER TABLE "+tabName+" ADD COLUMN "+csvLine+" text;" );

			//prepare query for data insertion
			String qu="INSERT INTO "+tabName+" VALUES (";
			for(int i=0;i<nbCol;i++){
				qu+="?";
				if(i<nbCol-1) qu+=",";
			}
			if(saveCSVLine) qu+=",?";
			qu+=");";
			PreparedStatement pst = pgc.getConnection().prepareStatement(qu);

			//insert data
			while ((line=br.readLine()) != null){
				sto = new StringTokenizer(line, separator);
				int i=1;
				while(sto.hasMoreTokens()){
					String val=sto.nextToken();
					if(replaceQuotationMark) val=val.replace("\"","");
					if(replacePoint) val=val.replace(".","");
					pst.setString(i++, val);
				}
				if(saveCSVLine) pst.setString(i++, line);
				pst.executeUpdate();
			}
			br.close();

			//vacuum
			if(finalVacuum) st.execute( "VACUUM "+tabName );

			ResultSet res = st.executeQuery("SELECT COUNT(*) FROM "+tabName);
			res.next();
			System.out.println("Done: "+res.getObject(1)+" objects created.");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//export as CSV
	public static void exportCSV(PGConnection pgc, String outputFilePath, String csvColumn, String tabName, String where) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFilePath), true));
			ResultSet res = pgc.getConnection().createStatement().executeQuery("SELECT "+csvColumn+" FROM "+tabName+where);
			while(res.next()){
				bw.write(res.getString(1));
				bw.newLine();
			}
			res.close();
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
