package eu.europa.ec.eurostat.jgiscotools;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CommandUtil {

	public static void run(String cmd) {
		//ProcessBuilder processBuilder = new ProcessBuilder(cmd);
		//ProcessBuilder processBuilder = new ProcessBuilder("gdalwarp", inF, outF, "-tr", resT+"", resT+"", "-r", "mode");
		ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", cmd);
		//ProcessBuilder processBuilder = new ProcessBuilder();
		//processBuilder.command("bash", "-c", "ls /home/juju/");
		//processBuilder.command(cmd);

		try {
			Process process = processBuilder.start();
			StringBuilder output = new StringBuilder();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line + "\n");
			}

			int exitVal = process.waitFor();
			if (exitVal == 0) {
				System.out.println("Success!");
				System.out.println(output);
				System.exit(0);
			} else {
				System.err.println("Problem");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
