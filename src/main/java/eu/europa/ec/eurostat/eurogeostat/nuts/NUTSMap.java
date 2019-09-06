/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.nuts;

import java.awt.Color;
import java.util.HashMap;

import org.geotools.brewer.color.ColorBrewer;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.function.Classifier;
import org.geotools.filter.function.RangedClassifier;
import org.geotools.map.FeatureLayer;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.opencarto.io.GifSequenceWriter;
import org.opengis.filter.FilterFactory;

import eu.europa.ec.eurostat.eurogeostat.core.MappingUtils;
import eu.europa.ec.eurostat.eurogeostat.core.StatisticalMap;
import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.io.EurobaseIO;

/**
 * 
 * Library to easily produce maps based on NUTS regions.
 * 
 * @author Julien Gaffuri
 *
 */
public class NUTSMap extends StatisticalMap {
	//TODO show DOM
	//TODO logo + copyright text "Administrative boundaries: (C) Eurogeographics (C) UN-FAO (C) Turksat"

	public int nutsLevel = 3; //NUTS level. can be 0, 1, 2, 3
	public int lod = 20; //Level of detail / scale. can be 1, 3, 10, 20 or 60

	boolean showJoin=false, showSepa=false;

	public Color cntrRGColor = Color.LIGHT_GRAY;
	public Color cntrBNColor = Color.WHITE;
	public Color nutsBNColor1 = Color.LIGHT_GRAY;
	public Color nutsBNColor2 = Color.WHITE;


	public NUTSMap(int nutsLevel, String databaseCode, Classifier classifier, String... dimLabelValues){
		this(nutsLevel, 20, EurobaseIO.getData(databaseCode, dimLabelValues), classifier, dimLabelValues);
	}
	public NUTSMap(int nutsLevel, int lod, String databaseCode, Classifier classifier, String... dimLabelValues){
		this(nutsLevel, lod, EurobaseIO.getData(databaseCode, dimLabelValues), classifier, dimLabelValues);
	}
	public NUTSMap(int nutsLevel, int lod, StatsHypercube sh, Classifier classifier, String... dimLabelValues){
		this(nutsLevel, lod, sh.selectDimValueEqualTo(dimLabelValues).shrinkDims().toMap(), classifier);
	}

	public NUTSMap(int nutsLevel, int lod, HashMap<String, Double> statData, Classifier classifier){
		super(null, "NUTS_ID", statData, null, classifier);
		this.nutsLevel = nutsLevel;
		this.lod = lod;

		//stat units
		this.statisticalUnits = NUTSShapeFile.getRG(this.nutsLevel, this.lod).getFeatureCollection();

		this.map.getViewport().setCoordinateReferenceSystem( this.statisticalUnits.getSchema().getCoordinateReferenceSystem() );
		this.setBounds(2580000.0, 7350000.0, 1340000.0, 5450000.0);

		setGraticule();
	}

	public NUTSMap make(){

		//graticules
		if(graticulesFS != null)
			map.addLayer( new FeatureLayer(graticulesFS, MappingUtils.getLineStyle(this.graticulesColor, this.graticulesWidth)) );

		//countries
		map.addLayer( new FeatureLayer(NUTSShapeFile.getCNTR("RG", lod).getFeatureCollection(NUTSShapeFile.CNTR_NEIG_CNTR), MappingUtils.getPolygonStyle(cntrRGColor, null)) );
		map.addLayer( new FeatureLayer(NUTSShapeFile.getCNTR("BN", lod).getFeatureCollection("COAS_FLAG='F'"), MappingUtils.getLineStyle(cntrBNColor, 0.2)) );

		//join stat data, if any
		String valuePropName = "Prop"+((int)(1000000*Math.random()));
		SimpleFeatureCollection[] out = MappingUtils.join(this.statisticalUnits, "NUTS_ID", this.statData, valuePropName);
		SimpleFeatureCollection fsRG = out[0], fsRGNoDta = out[1];

		StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();
		FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();

		//add layer for no data
		map.addLayer( new FeatureLayer(fsRGNoDta, MappingUtils.getPolygonStyle(this.noDataColor, null)) );

		//add layer for data
		if(fsRG.size()>0){
			Stroke stroke = styleFactory.createStroke( filterFactory.literal(Color.WHITE), filterFactory.literal(0.0001), filterFactory.literal(0));
			this.colors = ColorBrewer.instance().getPalette(paletteName).getColors(classNb);
			if(this.classifier==null) this.classifier = MappingUtils.getClassifier(fsRG, valuePropName, classifierName, classNb);
			Style s = MappingUtils.getThematicStyle(fsRG, valuePropName, this.classifier, this.colors, stroke);
			map.addLayer( new FeatureLayer(fsRG, s) );
		}

		//BN
		if(this.nutsLevel == 0){
			map.addLayer( new FeatureLayer(NUTSShapeFile.get("BN", lod).getFeatureCollection("STAT_LEVL_<=0 AND COAS_FLAG='F'"), MappingUtils.getLineStyle(nutsBNColor2, 0.8)) );
		} else if(this.nutsLevel == 1){
			map.addLayer( new FeatureLayer(NUTSShapeFile.get("BN", lod).getFeatureCollection("STAT_LEVL_<=1 AND COAS_FLAG='F'"), MappingUtils.getLineStyle(nutsBNColor1, 0.3)) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get("BN", lod).getFeatureCollection("STAT_LEVL_<=0 AND COAS_FLAG='F'"), MappingUtils.getLineStyle(nutsBNColor2, 1)) );
		} else if(this.nutsLevel == 2){
			map.addLayer( new FeatureLayer(NUTSShapeFile.get("BN", lod).getFeatureCollection("STAT_LEVL_<=2 AND COAS_FLAG='F'"), MappingUtils.getLineStyle(nutsBNColor1, 0.3)) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get("BN", lod).getFeatureCollection("STAT_LEVL_<=0 AND COAS_FLAG='F'"), MappingUtils.getLineStyle(nutsBNColor2, 1)) );
		} else if(this.nutsLevel == 3){
			map.addLayer( new FeatureLayer(NUTSShapeFile.get("BN", lod).getFeatureCollection("STAT_LEVL_<=2 AND COAS_FLAG='F'"), MappingUtils.getLineStyle(nutsBNColor1, 0.5)) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get("BN", lod).getFeatureCollection("STAT_LEVL_<=0 AND COAS_FLAG='F'"), MappingUtils.getLineStyle(nutsBNColor2, 1)) );
		}

		//sepa and join
		if(showJoin || showSepa){
			Style sepaJoinStyle = MappingUtils.getLineStyle(Color.GRAY, 0.3);
			if(showJoin) map.addLayer( new FeatureLayer(NUTSShapeFile.get("JOIN", lod).getFeatureCollection(NUTSShapeFile.getFilterSepaJoinLoD(this.lod)), sepaJoinStyle) );
			if(showSepa) map.addLayer( new FeatureLayer(NUTSShapeFile.get("SEPA", lod).getFeatureCollection(NUTSShapeFile.getFilterSepaJoinLoD(this.lod)), sepaJoinStyle) );
		}

		return this;
	}

	public NUTSMap makeDark() {
		super.makeDark();
		this.nutsBNColor1 = Color.DARK_GRAY;
		this.nutsBNColor2 = Color.BLACK;
		this.cntrRGColor = Color.DARK_GRAY;
		this.cntrBNColor = Color.BLACK;
		return this;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("Start.");

		//String outPath = "H:/desktop/", dataPath = "stat_cache/";
		String outPath = "/home/juju/Bureau/", dataPath = "/home/juju/stat_cache/";

		//EurobaseIO.update(dataPath, "tour_occ_nim", "tour_occ_nin2");


		//TODO bug in legend: always same values...
		/*/TODO map ratio
		new NUTSMap(1, 60, "tour_occ_nin2", null, "unit","NR","nace_r2","I551-I553","indic_to","B006","time","2012")
		.makeDark()
		.make()
		//.printClassification()
		.saveAsImage(outPath + "map.png", 1000, true, true)
		.saveLegendAsImage(outPath + "legend.png")
		.dispose()
		;*/


		//gif test
		RangedClassifier classifier = MappingUtils.getClassifier(4,5,6,7,8,10,13,20);
		String[] imgs = new String[12];
		for(int year = 2005; year<=2016; year++) {
			new NUTSMap(2, 60, "lfst_r_lfu3rt", classifier, "unit","PC","sex","T","age","Y_GE15","time",year+"")
			.setTitle("Unemployment rate in "+year)
			//.makeDark()
			.make()
			.saveAsImage(outPath + "map_"+year+".png", 1000, true, true)
			.saveLegendAsImage(outPath + "legend.png")
			.dispose()
			;
			imgs[year-2005] = outPath + "map_"+year+".png";
		}
		GifSequenceWriter.make(imgs, null, 1000, 5, outPath+"map.gif");


		/*
		StatsHypercube data = EurostatTSV.load(dataPath+"tour_occ_nin2.tsv").selectDimValueEqualTo("unit","NR","nace_r2","I551-I553","indic_to","B006").shrinkDims();
		//data = NUTSUtils.computePopRatioFigures(data, 1000, true);
		data = NUTSUtils.computeDensityFigures(data);

		RangedClassifier cl = MappingUtils.getClassifier(data.getQuantiles(8));
		for(int year = 2010; year<=2015; year++) {
			new NUTSMap(2, 60, data.selectDimValueEqualTo("time",year+"").shrinkDims().toMap(), cl)
			//.makeDark()
			.setTitle(year+"")
			.make()
			//.printClassification()
			.saveAsImage(outPath + "map_"+year+".png", 1000, true, true)
			.saveLegendAsImage(outPath + "legend.png")
			.dispose()
			;
		}*/


		/*HashMap<String, Double> statData =
				CSV.load("H:/methnet/geostat/out/tour_occ_nin2_nuts3.csv", "value").selectDimValueEqualTo("nace_r2", "I551-I553", "time", "2015 ")
				.shrinkDims().toMap();
		new NUTSMap(3, 60, statData, null)
		.setBounds(5580000.0, 6350000.0, 2340000.0, 3450000.0)
		//.makeDark()
		.setTitle("2015")
		.make()
		.saveAsImage(outPath+"map.png")
		.dispose();*/



		System.out.println("End.");
	}

}
