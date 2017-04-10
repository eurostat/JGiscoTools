/**
 * 
 */
package eu.ec.estat.geostat.nuts;

import java.awt.Color;
import java.util.HashMap;

import org.geotools.brewer.color.ColorBrewer;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.function.Classifier;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.ec.estat.geostat.MappingUtils;
import eu.ec.estat.geostat.StatisticalMap;
import eu.ec.estat.java4eurostat.io.CSV;

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


	//TODO solve time+" " issue
	/*public NUTSMap(int nutsLevel, int lod, String databaseCode, Classifier classifier, String... dimLabelValues){
		this(nutsLevel, lod, EurobaseIO.getData(databaseCode, dimLabelValues), classifier, dimLabelValues);
	}
	public NUTSMap(int nutsLevel, int lod, StatsHypercube sh, Classifier classifier, String... dimLabelValues){
		this(nutsLevel, lod, "geo", sh.selectDimValueEqualTo(dimLabelValues).shrinkDims().toMap(), classifier);
	}*/

	public NUTSMap(int nutsLevel, int lod, HashMap<String, Double> statData, Classifier classifier){
		super(null, "NUTS_ID", statData, null, classifier);
		this.nutsLevel = nutsLevel;
		this.lod = lod;

		//stat units
		this.statisticalUnits = NUTSShapeFile.get(this.lod, "RG").getFeatureCollection(NUTSShapeFile.getFilterByLevel(this.nutsLevel));

		CoordinateReferenceSystem crs = statisticalUnits.getSchema().getCoordinateReferenceSystem();
		this.map.getViewport().setCoordinateReferenceSystem(crs);
		this.map.getViewport().setBounds(new ReferencedEnvelope(2580000.0, 7350000.0, 1340000.0, 5450000.0, crs));

		setGraticule();
	}

	public NUTSMap make(){

		//graticules
		if(graticulesFS != null)
			map.addLayer( new FeatureLayer(graticulesFS, MappingUtils.getLineStyle(this.graticulesColor, this.graticulesWidth)) );

		//countries
		map.addLayer( new FeatureLayer(NUTSShapeFile.getCNTR(lod, "RG").getFeatureCollection(NUTSShapeFile.CNTR_NEIG_CNTR), MappingUtils.getPolygonStyle(cntrRGColor, null)) );
		map.addLayer( new FeatureLayer(NUTSShapeFile.getCNTR(lod, "BN").getFeatureCollection("COAS_FLAG='F'"), MappingUtils.getLineStyle(cntrBNColor, 0.2)) );

		//join stat data, if any
		String valuePropName = "Prop"+((int)(1000000*Math.random()));
		SimpleFeatureCollection[] out = MappingUtils.join(this.statisticalUnits, "NUTS_ID", this.statData, valuePropName);
		SimpleFeatureCollection fcRG = out[0], fcRGNoData = out[1];

		StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();
		FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();

		//RG
		Style RGStyleNoData = MappingUtils.getPolygonStyle(this.noDataColor, null);
		Style RGStyle = RGStyleNoData;
		if(fcRG.size()>0){
			Stroke stroke = styleFactory.createStroke( filterFactory.literal(Color.WHITE), filterFactory.literal(0.0001), filterFactory.literal(0));
			this.colors = ColorBrewer.instance().getPalette(paletteName).getColors(classNb);
			if(this.classifier==null) this.classifier = MappingUtils.getClassifier(fcRG, valuePropName, classifierName, classNb);
			RGStyle = MappingUtils.getThematicStyle(fcRG, valuePropName, this.classifier, this.colors, stroke);
		}

		map.addLayer( new FeatureLayer(fcRGNoData, RGStyleNoData) );
		map.addLayer( new FeatureLayer(fcRG, RGStyle) );

		//BN
		if(this.nutsLevel == 0){
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection("STAT_LEVL_<=0 AND COAS_FLAG='F'"), MappingUtils.getLineStyle(nutsBNColor2, 0.8)) );
		} else if(this.nutsLevel == 1){
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection("STAT_LEVL_<=1 AND COAS_FLAG='F'"), MappingUtils.getLineStyle(nutsBNColor1, 0.3)) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection("STAT_LEVL_<=0 AND COAS_FLAG='F'"), MappingUtils.getLineStyle(nutsBNColor2, 1)) );
		} else if(this.nutsLevel == 2){
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection("STAT_LEVL_<=2 AND COAS_FLAG='F'"), MappingUtils.getLineStyle(nutsBNColor1, 0.3)) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection("STAT_LEVL_<=0 AND COAS_FLAG='F'"), MappingUtils.getLineStyle(nutsBNColor2, 1)) );
		} else if(this.nutsLevel == 3){
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection("STAT_LEVL_<=2 AND COAS_FLAG='F'"), MappingUtils.getLineStyle(nutsBNColor1, 0.5)) );
			map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "BN").getFeatureCollection("STAT_LEVL_<=0 AND COAS_FLAG='F'"), MappingUtils.getLineStyle(nutsBNColor2, 1)) );
		}

		//sepa and join
		if(showJoin || showSepa){
			Style sepaJoinStyle = MappingUtils.getLineStyle(Color.GRAY, 0.3);
			if(showJoin) map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "JOIN").getFeatureCollection(NUTSShapeFile.getFilterSepaJoinLoD(this.lod)), sepaJoinStyle) );
			if(showSepa) map.addLayer( new FeatureLayer(NUTSShapeFile.get(lod, "SEPA").getFeatureCollection(NUTSShapeFile.getFilterSepaJoinLoD(this.lod)), sepaJoinStyle) );
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

		String outPath = "H:/desktop/";
		//String outPath = "/home/juju/Bureau/";
		//String dataPath = "stat_cache/";

		//EurobaseIO.update(dataPath, "tour_occ_nim", "tour_occ_nin2");

		/*/load stat data
		StatsHypercube data = EurostatTSV.load(dataPath+"tour_occ_nin2.tsv").selectDimValueEqualTo("unit","NR","nace_r2","I551-I553","indic_to","B006").shrinkDims();
		//data = NUTSUtils.computePopRatioFigures(data, 1000, true);
		data = NUTSUtils.computeDensityFigures(data);

		RangedClassifier cl = getClassifier(data.getQuantiles(8));
		for(int year = 2010; year<=2015; year++) {
			new NUTSMap(2, 60, "geo", data.selectDimValueEqualTo("time",year+" ").shrinkDims().toMap(), null)
			//.makeDark()
			.setTitle(year+"")
			.setClassifier(cl)
			.make()
			//.printClassification()
			.saveAsImage(outPath + "map_"+year+".png", 1000, true, true)
			//.saveLegendAsImage(outPath + "legend.png")
			.dispose()
			;
		}*/

		HashMap<String, Double> statData =
				CSV.load("H:/methnet/geostat/out/tour_occ_nin2_nuts3.csv", "value").selectDimValueEqualTo("nace_r2", "I551-I553", "time", "2015 ")
				.shrinkDims().toMap();
		new NUTSMap(3, 60, statData, null)
		.makeDark()
		.setTitle("2015")
		.make()
		.saveAsImage(outPath+"map.png")
		.dispose();

		System.out.println("End.");
	}

}
