package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Scanner;

/**
 * Manage parameters from "user.home\eclipse.config" file.
 * 
 * @author gaffuju
 *
 */
public class LocalParameters {


	/**
	 * Return a parameter name.
	 * 
	 * @param key
	 * @return
	 */
	public static String get(String key) {
		try {
			Scanner scanner = new Scanner(new File(System.getProperty("user.home")+"\\eclipse.config"));
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if("".equals(line))
					continue;
				String[] parts = line.split("=");

				if(parts[0].equals(key)) {
					scanner.close();
					return parts[1];
				}
			}
			scanner.close();
		} catch (FileNotFoundException e) { e.printStackTrace(); }
		return null;
	}


	/**
	 * 
	 */
	public static void loadProxySettings() {
		System.setProperty("http.proxyHost", "pslux.ec.europa.eu");
		System.setProperty("http.proxyPort", "8012");
		System.setProperty("https.proxyHost", "pslux.ec.europa.eu");
		System.setProperty("https.proxyPort", "8012");

		Authenticator.setDefault(new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(get("proxy_username"), get("proxy_pwd").toCharArray());
			}
		});

	}

}
