package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services;

import java.io.File;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Scanner;

public class ProxySetter {

	public static void loadProxySettings() {
		System.setProperty("http.proxyHost", "pslux.ec.europa.eu");
		System.setProperty("http.proxyPort", "8012");
		System.setProperty("https.proxyHost", "pslux.ec.europa.eu");
		System.setProperty("https.proxyPort", "8012");

		try {
			String proxy_username = null, proxy_pwd = null;
			
			Scanner scanner = new Scanner(new File(System.getProperty("user.home")+"\\eclipse.config"));
			while (scanner.hasNextLine()) {
			   String line = scanner.nextLine();
				if("".equals(line)) {
					continue;
				}
				String[] parts = line.split("=");
				
				if(parts[0].equals("proxy_username")) proxy_username = parts[1];
				if(parts[0].equals("proxy_pwd")) proxy_pwd = parts[1];
			}
			scanner.close();
			authenticate(proxy_username, proxy_pwd);
			
		} catch (Exception e) {
			System.err.println("Could not load proxy settings");
			e.printStackTrace();
		}

	}

	private static void authenticate(String proxy_username, String proxy_pwd) {
		Authenticator.setDefault(new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(proxy_username, proxy_pwd.toCharArray());
			}
		});
	}

}
