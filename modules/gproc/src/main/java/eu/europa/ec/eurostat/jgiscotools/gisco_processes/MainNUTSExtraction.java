package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

public class MainNUTSExtraction {
/*
	public static void main(String[] args) {
		System.setProperty("org.geotools.referencing.forceXY", "true");

		String outPath = "/home/juju/Bureau/drafts/cnts/";

		//load nuts regions
		ArrayList<Feature> fs = SHPUtil.loadSHP("/home/juju/Bureau/drafts/NUTS_RG_2016_RG_01M_DRAFT.shp").fs; //4258 4326
		ArrayList<Feature> fsLAEA = SHPUtil.loadSHP("/home/juju/Bureau/drafts/NUTS_RG_2016_RG_01M_DRAFT_LAEA.shp").fs;

		//extract all cnt ids
		HashSet<String> cnts = new HashSet<String>();
		for(Feature f : fs) cnts.add(f.getAttribute("CNTR_ID").toString());


		for(String cnt : cnts) {
			//for(String cnt : new String[] { "BE" }) {

			//for(String cnt : cnts) {
			System.out.println(cnt);


			//filter - nuts 3 regions for cnt
			ArrayList<Feature> fs_ = new ArrayList<Feature>();
			for(Feature f : fs)
				if(f.getAttribute("CNTR_ID").equals(cnt))
					fs_.add(f);

			//save as new shp file
			SHPUtil.saveSHP(fs_, outPath+cnt+"/NUTS_RG_2016_01M_DRAFT_"+cnt+".shp", ProjectionUtil.getETRS89_2D_CRS());




			//filter - nuts 3 regions for cnt
			ArrayList<Feature> fsLAEA_ = new ArrayList<Feature>();
			for(Feature f : fsLAEA)
				if(f.getAttribute("CNTR_ID").equals(cnt))
					fsLAEA_.add(f);

			//save as new shp file
			SHPUtil.saveSHP(fsLAEA_, outPath+cnt+"/NUTS_RG_2016_01M_DRAFT_"+cnt+"_LAEA.shp", ProjectionUtil.getETRS89_LAEA_CRS());


			//make map image
			SimpleFeatureCollection sfc = SHPUtil.getSimpleFeatures(outPath+cnt+"/NUTS_RG_2016_01M_DRAFT_"+cnt+"_LAEA.shp");
			SimpleFeatureCollection sfcAll = SHPUtil.getSimpleFeatures("/home/juju/Bureau/drafts/NUTS_RG_2016_RG_01M_DRAFT_LAEA.shp");
			CoordinateReferenceSystem crs = sfc.getSchema().getCoordinateReferenceSystem();
			if(cnt.equals("ES")) {
				makeMap(sfc, sfcAll, outPath, cnt+"_1", new ReferencedEnvelope(new Envelope(2655354, 4000000, 1421741, 2500000), crs));
				makeMap(sfc, sfcAll, outPath, cnt+"_2", new ReferencedEnvelope(new Envelope(1502241, 2077374, 885520, 1160748), crs));
			} else if(cnt.equals("FR")) {
				makeMap(sfc, sfcAll, outPath, cnt+"_1", new ReferencedEnvelope(new Envelope(3105054, 4394340, 1965782, 3158887), crs));
				makeMap(sfc, sfcAll, outPath, cnt+"_2", new ReferencedEnvelope(new Envelope(-2849020, -2436815, 550545, 1047481), crs));
				makeMap(sfc, sfcAll, outPath, cnt+"_3", new ReferencedEnvelope(new Envelope(-2700168, -2530706, 2453558, 3018050), crs));
				makeMap(sfc, sfcAll, outPath, cnt+"_4", new ReferencedEnvelope(new Envelope(9951524, 10032533, -3080152, -3017892), crs));
				makeMap(sfc, sfcAll, outPath, cnt+"_5", new ReferencedEnvelope(new Envelope(8709827, 8748901, -2800554, -2764987), crs));
			} else if(cnt.equals("PT")) {
				makeMap(sfc, sfcAll, outPath, cnt+"_1", new ReferencedEnvelope(new Envelope(2526818, 3036734, 1670890, 2315203), crs));
				makeMap(sfc, sfcAll, outPath, cnt+"_2", new ReferencedEnvelope(new Envelope(1756509, 1916104, 1456449, 1558728), crs));
				makeMap(sfc, sfcAll, outPath, cnt+"_3", new ReferencedEnvelope(new Envelope(918013, 1346896, 2239111, 2802390), crs));
			} else
				makeMap(sfc, sfcAll, outPath, cnt, sfc.getBounds());

			//zip everything
			CompressUtil.createZIP(outPath+"NUTS_RG_2016_01M_DRAFT_"+cnt+".zip", outPath+cnt+"/", new String[] {
					"NUTS_RG_2016_01M_DRAFT_"+cnt+".dbf",
					"NUTS_RG_2016_01M_DRAFT_"+cnt+".fix",
					"NUTS_RG_2016_01M_DRAFT_"+cnt+".prj",
					"NUTS_RG_2016_01M_DRAFT_"+cnt+".shp",
					"NUTS_RG_2016_01M_DRAFT_"+cnt+".shx",
					//"NUTS_RG_2016_01M_DRAFT_"+cnt+".png"
			});

		}

		System.out.println("End");
	}


	//make overview image
	private static void makeMap(SimpleFeatureCollection sfc, SimpleFeatureCollection sfcAll, String outPath, String fileCodeName, ReferencedEnvelope bounds) {

		MapContent map = new MapContent();
		CoordinateReferenceSystem crs = sfc.getSchema().getCoordinateReferenceSystem();
		map.getViewport().setCoordinateReferenceSystem(crs);
		map.getViewport().setBounds(bounds);
		map.setTitle(fileCodeName+" - NUTS 3");

		//polygon color style
		HashSet<String>[] colorids = new HashSet[6];
		for(int i=0; i<=5; i++) colorids[i] = new HashSet<String>(Arrays.asList(new String[] { ""+i }));
		Stroke stroke = MappingUtils.getStroke(Color.DARK_GRAY, 0.05);
		Style colStyle = MappingUtils.getThematicStyle(sfc, "color", new ExplicitClassifier(colorids), ColorBrewer.Set3.getColorPalette(6), stroke);

		//add layer for no data
		map.addLayer( new FeatureLayer(sfcAll, MappingUtils.getPolygonStyle(new Color(217,217,217), Color.DARK_GRAY, 0.3)) );
		map.addLayer( new FeatureLayer(sfc, colStyle ));
		//map.addLayer( new FeatureLayer(sfc, MappingUtils.getPolygonStyle(new Color(253,180,98), Color.DARK_GRAY, 0.3)) );
		map.addLayer( new FeatureLayer(sfc, MappingUtils.getTextStyle("NUTS3",Color.BLACK,12,"Arial Bold",-0.5,Color.WHITE)) );

		//build image
		double scaleDenom = 750000;
		MappingUtils.saveAsImage(map, scaleDenom , new Color(128,177,211), 20, new TitleDisplayParameters(), outPath, "overview_"+fileCodeName+".png");

		//JMapFrame.showMap(map);
		map.dispose();
	}
*/
}
