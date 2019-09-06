package eu.europa.ec.eurostat.eurogeostat.io.postgis;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Connection to a PG database.
 * 
 * @author julien Gaffuri
 *
 */
public class PGConnection {

	public String db_name;
	public String host="localhost";
	public int port=5432;
	public String user;
	public String psw;

	private Connection conn=null;

	public PGConnection(){}
	public PGConnection(String db_name, String host, int port, String user, String psw){
		super();
		this.db_name = db_name;
		this.host = host;
		this.port = port;
		this.user = user;
		this.psw = psw;
	}

	public Connection getConnection(){
		if(conn==null){
			try {
				//jdbc:postgresql://host:port/database
				String url="jdbc:postgresql://"+host+":"+port+"/"+db_name;
				Class.forName("org.postgresql.Driver");
				return DriverManager.getConnection(url, user, psw);
			} catch (Exception e) { e.printStackTrace(); }
		}
		return conn;		
	}

}
