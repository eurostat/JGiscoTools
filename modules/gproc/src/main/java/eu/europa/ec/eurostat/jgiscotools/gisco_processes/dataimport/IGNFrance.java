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

	//https://geoservices.ign.fr/services-web-experts-topographie
	//https://geoservices.ign.fr/services-web-inspire

	//https://geoservices.ign.fr/telechargement

	public static void main(String[] args) throws Exception {
		System.out.println("Start");

		String path = "/home/juju/Bureau/gisco/fr/bdtopo/";

		//get 7z files
		List<String> files = Files.walk(Paths.get(path))
				.filter(p -> !Files.isDirectory(p))
				.map(p -> p.toString())
				.filter(f -> f.endsWith("7z"))
				.collect(Collectors.toList())
				;

		System.out.println(files.size());

		//decompress
		for(String file : files) {
			System.out.println("Decompress " + file);

			String outFolder = path + file.toString().split("LAMB93_D")[1].replace(".7z","").replace("-","_") + "/";
			new File(outFolder).mkdirs();
			//System.out.println(outFolder);

			SevenZFile sevenZFile = new SevenZFile(new File(file));
			SevenZArchiveEntry entry = sevenZFile.getNextEntry();
			while(entry!=null){
				String en = entry.getName();
				String fileClass = "BATIMENT";
				if(!en.contains(fileClass)) {
					entry = sevenZFile.getNextEntry();
					continue;
				}
				System.out.println(en);

				String ext = en.substring(en.length()-3, en.length());
				FileOutputStream out = new FileOutputStream(outFolder + fileClass + "." + ext);
				byte[] content = new byte[(int) entry.getSize()];
				sevenZFile.read(content, 0, content.length);
				out.write(content);
				out.close();

				entry = sevenZFile.getNextEntry();
			}
			sevenZFile.close();

		}

		//reproject

		System.out.println("End");
	}

}
