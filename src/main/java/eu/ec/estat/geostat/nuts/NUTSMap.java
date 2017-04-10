/**
 * 
 */
package eu.ec.estat.geostat.nuts;

import java.awt.Color;
import java.util.HashMap;

import org.geotools.brewer.color.ColorBrewer;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.function.Classifier;
import org.geotools.map.FeatureLayer;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.MultiPolygon;

import eu.ec.estat.geostat.MappingUtils;
import eu.ec.estat.geostat.StatisticalMap;

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

	static CoordinateReferenceSystem LAEA_CRS = null;
	static{
		//try { LAEA_CRS = CRS.decode("EPSG:3035"); } catch (Exception e) { e.printStackTrace(); }
		LAEA_CRS = NUTSShapeFile.get(60, "RG").getCRS();
	}

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

	public NUTSMap(int nutsLevel, int lod, String propName, HashMap<String, Double> statData, Classifier classifier){
		super(propName, statData, classifier);
		this.nutsLevel = nutsLevel;
		this.lod = lod;
	}

	public NUTSMap make(){
		this.map.getViewport().setCoordinateReferenceSystem(LAEA_CRS);
		this.setBounds(1340000.0, 5450000.0, 2580000.0, 7350000.0);

		//graticules
		if(this.showGraticules)
			map.addLayer( new FeatureLayer(NUTSShapeFile.getGraticules().getFeatureCollection(NUTSShapeFile.GRATICULE_FILTER_5), MappingUtils.getLineStyle(this.graticulesColor, this.graticulesWidth)) );

		//countries
		map.addLayer( new FeatureLayer(NUTSShapeFile.getCNTR(lod, "RG").getFeatureCollection(NUTSShapeFile.CNTR_NEIG_CNTR), MappingUtils.getPolygonStyle(cntrRGColor, null)) );
		map.addLayer( new FeatureLayer(NUTSShapeFile.getCNTR(lod, "BN").getFeatureCollection("COAS_FLAG='F'"), MappingUtils.getLineStyle(cntrBNColor, 0.2)) );


		//get region features
		SimpleFeatureCollection fcRG = NUTSShapeFile.get(this.lod, "RG").getFeatureCollection(NUTSShapeFile.getFilterByLevel(this.nutsLevel));
		SimpleFeatureCollection fcRGNoDta = null;

		//join stat data, if any
		SimpleFeatureCollection[] out = join(fcRG, this.statData, this.propName);
		fcRG = out[0]; fcRGNoDta = out[1];

		StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();
		FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();

		//RG
		Style RGStyleNoData = MappingUtils.getPolygonStyle(Color.GRAY, null);
		Style RGStyle = RGStyleNoData;
		if(fcRG.size()>0){
			Stroke stroke = styleFactory.createStroke( filterFactory.literal(Color.WHITE), filterFactory.literal(0.0001), filterFactory.literal(0));
			this.colors = ColorBrewer.instance().getPalette(paletteName).getColors(classNb);
			if(this.classifier==null) this.classifier = MappingUtils.getClassifier(fcRG, propName, classifierName, classNb);
			RGStyle = MappingUtils.getThematicStyle(fcRG, propName, this.classifier, this.colors, stroke);
		}

		map.addLayer( new FeatureLayer(fcRGNoDta, RGStyleNoData) );
		map.addLayer( new FeatureLayer(fcRG, RGStyle) );

		//BN
		//TODO propose generic border display pattern - level-width-color
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
		this.imgBckgrdColor = Color.BLACK;
		this.cntrRGColor = Color.DARK_GRAY;
		this.cntrBNColor = Color.BLACK;
		this.nutsBNColor1 = Color.DARK_GRAY;
		this.nutsBNColor2 = Color.BLACK;
		this.fontColor = Color.WHITE;
		this.graticulesColor = new Color(40,40,40);
		return this;
	}


	//TODO generic, to mapping utils
	private static SimpleFeatureCollection[] join(SimpleFeatureCollection fc, HashMap<String, Double> statData, String propName) {
		try {
			//fc.getSchema().getGeometryDescriptor().getType()
			SimpleFeatureType ft = DataUtilities.createType("NUTS_RG_joined","NUTS_ID:String,the_geom:MultiPolygon,"+propName+":Double"); //:srid=3035
			ft = DataUtilities.createSubType(ft, null, fc.getSchema().getCoordinateReferenceSystem());
			DefaultFeatureCollection fcRGJoin = new DefaultFeatureCollection("fc_joined", ft);
			DefaultFeatureCollection fcRGNoData = new DefaultFeatureCollection("fc_nodata", ft);

			SimpleFeatureIterator it = fc.features();
			while (it.hasNext()) {
				SimpleFeature f = it.next();
				String id = (String) f.getAttribute("NUTS_ID");
				Double value = null;
				if(statData != null) value = statData.get(id);
				if(value==null) {
					SimpleFeature f2 = SimpleFeatureBuilder.build( ft, new Object[]{ id, (MultiPolygon)f.getAttribute("the_geom"), 0 }, null);
					fcRGNoData.add(f2);
				} else {
					SimpleFeature f2 = SimpleFeatureBuilder.build( ft, new Object[]{ id, (MultiPolygon)f.getAttribute("the_geom"), value.doubleValue() }, null);
					fcRGJoin.add(f2);
				}
			}
			it.close();
			return new SimpleFeatureCollection[]{fcRGJoin, fcRGNoData};
		} catch (Exception e) { e.printStackTrace(); }
		return null;
	}



	public static void main(String[] args) throws Exception {
		System.out.println("Start.");

		//String outPath = "H:/desktop/";
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

		/*HashMap<String, Double> statData =
				CSV.load("H:/methnet/geostat/out/tour_occ_nin2_nuts3.csv", "value").selectDimValueEqualTo("nace_r2", "I551-I553", "time", "2015 ")
				.shrinkDims().toMap();
		new NUTSMap(3, 60, "geo", statData, null)
		.make()
		//.show()
		.saveAsImage(outPath+"map.png", 1000, true, true)
		.dispose();*/
		System.out.println("End.");
	}

}
