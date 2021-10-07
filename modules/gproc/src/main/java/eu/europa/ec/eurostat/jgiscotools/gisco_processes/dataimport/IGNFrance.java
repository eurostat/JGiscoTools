package eu.europa.ec.eurostat.jgiscotools.gisco_processes.dataimport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class IGNFrance {

	public static void main(String[] args) throws Exception {
		System.out.println("Start");

		String path = "/home/juju/Bureau/gisco/fr/bdtopo/";
		
		//get zip files
		Stream<Path> zips = Files.find(Paths.get(path),
		           Integer.MAX_VALUE,
		           (path_, basicFileAttributes) -> path_.toFile().getName().matches("*.zip")
		);
		System.out.println(zips.count());


		//https://geoservices.ign.fr/services-web-experts-topographie
		//https://geoservices.ign.fr/services-web-inspire

		//https://geoservices.ign.fr/telechargement

		System.out.println("End");
	}

}
