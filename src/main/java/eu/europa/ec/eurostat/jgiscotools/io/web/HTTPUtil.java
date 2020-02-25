package eu.europa.ec.eurostat.jgiscotools.io.web;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HTTPUtil {
	final static Logger LOGGER = LogManager.getLogger(HTTPUtil.class.getName());

	public static void downloadFromURL(String url, String file) {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
			new File(file).delete();
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
			String line=null;
			while ((line = in.readLine()) != null) out.println(line);
			in.close();
			out.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public static InputStream executeQuery(String url) throws MalformedURLException, IOException{
		InputStream data = null;
		try {
			data = (new URL(url)).openStream();
		} catch (UnknownHostException e) {
			LOGGER.warn("Impossible to execute query from "+url);
		}
		return data;
	}

}
