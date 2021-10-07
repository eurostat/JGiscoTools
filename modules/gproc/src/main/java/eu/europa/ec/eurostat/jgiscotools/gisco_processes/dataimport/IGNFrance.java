package eu.europa.ec.eurostat.jgiscotools.gisco_processes.dataimport;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

public class IGNFrance {

	public static void main(String[] args) throws Exception {
		System.out.println("Start");

		String path = "/home/juju/Bureau/gisco/fr/bdtopo/";

		//get 7z files
		List<String> files = Files.walk(Paths.get(path))
				.filter(p -> !Files.isDirectory(p))
				.map(p -> p.toString().toLowerCase())
				.filter(f -> f.endsWith("7z"))
				.collect(Collectors.toList())
				;

		//decompress
		for(String file : files) {
			System.out.println("Decompress "+file);
			
			SevenZFile sevenZFile = new SevenZFile(new File("test-documents.7z"));
		    SevenZArchiveEntry entry = sevenZFile.getNextEntry();
		    while(entry!=null){
		        System.out.println(entry.getName());
		        FileOutputStream out = new FileOutputStream(entry.getName());
		        byte[] content = new byte[(int) entry.getSize()];
		        sevenZFile.read(content, 0, content.length);
		        out.write(content);
		        out.close();
		        entry = sevenZFile.getNextEntry();
		    }
		    sevenZFile.close();
			
		}
		


		//https://geoservices.ign.fr/services-web-experts-topographie
		//https://geoservices.ign.fr/services-web-inspire

		//https://geoservices.ign.fr/telechargement

		System.out.println("End");
	}

}
