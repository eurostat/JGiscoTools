/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.mains;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.locationtech.jts.geom.Point;

import eu.europa.ec.eurostat.eurogeostat.datamodel.Feature;
import eu.europa.ec.eurostat.eurogeostat.io.SHPUtil;
import eu.europa.ec.eurostat.eurogeostat.tesselationGeneralisation.TesselationGeneralisation;
import eu.europa.ec.eurostat.eurogeostat.util.ProjectionUtil.CRSType;

/**
 * @author julien Gaffuri
 *
 */
public class TesselationGeneralisationMain {

	public static void main(String[] args) {

		Options options = new Options();
		options.addOption(Option.builder("i").longOpt("inputFile").desc("Input file (SHP format).")
				.hasArg().argName("file").build());
		options.addOption(Option.builder("o").longOpt("outputFile").desc("Optional. Output file (SHP format). Default: 'out.shp'.")
				.hasArg().argName("file").build());
		options.addOption(Option.builder("ip").longOpt("inputPointFile").desc("Optional. Input file for points (SHP format).")
				.hasArg().argName("file").build());
		options.addOption(Option.builder("id").desc("Optional. Id property to link the units and the points.")
				.hasArg().argName("string").build());
		options.addOption(Option.builder("s").longOpt("scaleDenominator").desc("Optional. The scale denominator for the target data. Default: 50000")
				.hasArg().argName("double").build());
		options.addOption(Option.builder("inb").longOpt("roundNb").desc("Optional. Number of iterations of the process. Default: 10.")
				.hasArg().argName("int").build());
		options.addOption(Option.builder("mcn").longOpt("Optional. maxCoordinatesNumber").desc("Default: 1000000.")
				.hasArg().argName("int").build());
		options.addOption(Option.builder("omcn").longOpt("Optional. objMaxCoordinateNumber").desc("Default: 1000.")
				.hasArg().argName("int").build());
		options.addOption(Option.builder("h").desc("Show this help message").build());

		CommandLine cmd = null;
		try { cmd = new DefaultParser().parse( options, args); } catch (ParseException e) {
			System.err.println( "Parsing failed.  Reason: " + e.getMessage() );
			return;
		}

		//help statement
		if(cmd.hasOption("h")) {
			new HelpFormatter().printHelp("java -jar opencarto-XXX.jar", options);
			return;
		}

		//String inFile = "src/test/resources/testTesselationGeneralisation.shp";
		String inFile = cmd.getOptionValue("i");
		if(inFile==null) {
			System.err.println("An input file should be specified with -i option. Use -h option to show the help message.");
			return;
		} else if(!new File(inFile).exists()) {
			System.err.println("Input file does not exist: "+inFile);
			return;
		}
		String outFile = cmd.getOptionValue("o");
		if(outFile == null) outFile = Paths.get("").toAbsolutePath().toString()+"/out.shp";
		String inPtFile = cmd.getOptionValue("ip");
		String idProp = cmd.getOptionValue("id");
		double scaleDenominator = cmd.getOptionValue("s") != null? Integer.parseInt(cmd.getOptionValue("s")) : 50000;
		int roundNb = cmd.getOptionValue("inb") != null? Integer.parseInt(cmd.getOptionValue("inb")) : 10;
		int maxCoordinatesNumber = cmd.getOptionValue("mcn") != null? Integer.parseInt(cmd.getOptionValue("mcn")) : 1000000;
		int objMaxCoordinateNumber = cmd.getOptionValue("omcn") != null? Integer.parseInt(cmd.getOptionValue("omcn")) : 1000;


		System.out.println("Load data from "+inFile);
		Collection<Feature> units = SHPUtil.loadSHP(inFile).fs;
		if(idProp != null && !"".equals(idProp)) for(Feature unit : units) unit.setID( unit.getAttribute(idProp).toString() );

		HashMap<String, Collection<Point>> points = null;
		if(inPtFile != null && !"".equals(inPtFile)) {
			System.out.println("Load point data from "+inPtFile);
			points = TesselationGeneralisation.loadPoints(inPtFile, idProp);
		}

		System.out.println("Launch generalisation");
		CRSType crsType = SHPUtil.getCRSType(inFile);
		units = TesselationGeneralisation.runGeneralisation(units, points, crsType, scaleDenominator, roundNb, maxCoordinatesNumber, objMaxCoordinateNumber);

		System.out.println("Save output to "+outFile);
		SHPUtil.saveSHP(units, outFile, SHPUtil.getCRS(inFile));

		System.out.println("End");
	}

}
