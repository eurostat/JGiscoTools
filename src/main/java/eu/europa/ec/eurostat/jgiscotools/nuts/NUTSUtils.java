package eu.europa.ec.eurostat.jgiscotools.nuts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;

import eu.europa.ec.eurostat.java4eurostat.base.Stat;
import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.base.StatsIndex;
import eu.europa.ec.eurostat.java4eurostat.io.EurobaseIO;
import eu.europa.ec.eurostat.java4eurostat.io.EurostatTSV;
import eu.europa.ec.eurostat.jgiscotools.io.ShapeFile;

/**
 * 
 * @author julien Gaffuri
 *
 */
public class NUTSUtils {

	public static void main(String[] args) {
		//for(int time=1985; time<=2020; time++) System.out.println(getNUTSPopulation("FR", time));
		//for(int time=1985; time<=2020; time++) System.out.println(getNUTSPopulation("FR", time) / getNUTSArea("FR", time));
		//EurostatTSV.load("stat_cache/demo_r_d3area.tsv").selectDimValueEqualTo("unit","KM2","geo","FR").printInfo();
	}


	//compute figures divided by nuts area (in km2)
	public static StatsHypercube computeDensityFigures(StatsHypercube sh){ return computeDensityFigures(sh, 1, false); }
	public static StatsHypercube computeDensityFigures(StatsHypercube sh, int multi, boolean showMessages){
		StatsHypercube out = new StatsHypercube(sh.getDimLabels());
		for(Stat s : sh.stats){
			String geo = s.dims.get("geo");
			double area = getNUTSArea(geo);
			if(Double.isNaN(area)){
				if(showMessages) System.err.println("Could not find area of NUTS region "+geo);
				continue;
			}
			Stat s2 = new Stat(s); s2.value = multi*1e6*s.value/area;
			out.stats.add(s2);
		}
		return out;
	}

	//compute figures divided by nuts population
	public static StatsHypercube computePopRatioFigures(StatsHypercube sh){ return computePopRatioFigures(sh, 1000, false); }
	public static StatsHypercube computePopRatioFigures(StatsHypercube sh, int multi, boolean showMessages){
		StatsHypercube out = new StatsHypercube(sh.getDimLabels());
		for(Stat s : sh.stats){
			String geo = s.dims.get("geo");
			int year = Integer.parseInt(s.dims.get("time").replace(" ", ""));
			double pop = getNUTSPopulation(geo, year);
			if(Double.isNaN(pop)){
				if(showMessages) System.err.println("Could not find population of NUTS region "+geo+" in "+year);
				continue;
			}
			Stat s2 = new Stat(s); s2.value = multi*s.value/pop;
			out.stats.add(s2);
		}
		return out;
	}



	//Population on 1 January by broad age group, sex and NUTS 3 region (demo_r_pjanaggr3)	AGE=TOTAL;SEX=T;UNIT="NR"
	private static StatsIndex nutsPop = null;
	public static double getNUTSPopulation(String nutsCode, int year){
		if(nutsPop == null){
			EurobaseIO.update("stat_cache/", "demo_r_pjanaggr3");
			nutsPop = new StatsIndex(
					EurostatTSV.load("stat_cache/demo_r_pjanaggr3.tsv").selectDimValueEqualTo("age","TOTAL", "sex","T", "unit","NR")
					.delete("age").delete("sex").delete("unit"),
					"time", "geo"
					);
		}
		return nutsPop.getSingleValue(year+"", nutsCode);
	}

	//Area by NUTS 3 region (demo_r_d3area) LANDUSE=L0008;TOTAL  UNIT=KM2
	/*private static StatsIndex nutsArea = null;
	public static double getNUTSArea(String nutsCode, int time){ return getNUTSArea(nutsCode, time, "TOTAL"); }
	public static double getNUTSArea(String nutsCode, int year, String landuse){
		if(nutsArea == null){
			EurobaseIO.update("stat_cache/", "demo_r_d3area");
			nutsArea = new StatsIndex(
					EurostatTSV.load("stat_cache/demo_r_d3area.tsv").selectDimValueEqualTo("unit","KM2")
					.delete("unit"),
					"landuse", "time", "geo"
					);
		}
		return nutsArea.getSingleValue(landuse, year, nutsCode);
	}*/

	private static HashMap<String,Double> nutsArea = null;
	public static Double getNUTSArea(String geo){
		if(nutsArea == null){
			nutsArea = new HashMap<String,Double>();
			ShapeFile shp = NUTSShapeFile.getRGForArea();
			FeatureIterator<SimpleFeature> it = shp.getFeatures();
			while (it.hasNext()) {
				SimpleFeature f = it.next();
				String geo_ = f.getAttribute("NUTS_ID").toString();
				double area = ((Geometry)f.getDefaultGeometry()).getArea();
				nutsArea.put(geo_, area);
			}
			it.close();
		}
		Double area = nutsArea.get(geo);
		if(area==null) return Double.NaN;
		return area.doubleValue();
	}







	private static class NUTSChange{
		public String codeIni, codeFin, change, explanation;
		@Override
		public String toString() { return codeIni+" -> "+codeFin+" - "+change+" - "+explanation; }
	}

	private static HashSet<NUTSChange> changes2010_2013 = null;
	private static HashMap<String,NUTSChange> changes2010_2013I1 = null;
	private static void load2010_2013(){
		if(changes2010_2013 != null) return;
		changes2010_2013 = new HashSet<NUTSChange>();
		changes2010_2013I1 = new HashMap<String,NUTSChange>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("resources/nuts_changes/NUTS_changes_2010_2013.csv"));
			//skip first line
			String line = br.readLine();
			//read data
			while ((line = br.readLine()) != null) {
				String[] elts = line.split(",", -1);
				if(elts.length != 4) System.out.println(elts.length);
				NUTSChange nc = new NUTSChange(); nc.codeIni=elts[0]; nc.codeFin=elts[1]; nc.change=elts[2]; nc.explanation=elts[3];
				changes2010_2013.add(nc);
				if(nc.codeIni != null && !"".equals(nc.codeIni)) changes2010_2013I1.put(nc.codeIni, nc);
			}
		} catch (IOException e) { e.printStackTrace();
		} finally {
			try { if (br != null)br.close(); } catch (Exception ex) { ex.printStackTrace(); }
		}
	}

	public String get2010To2013Code(String code2010){
		load2010_2013();
		NUTSChange nc = changes2010_2013I1.get(code2010);
		if(nc == null) return null;
		if("".equals(nc.codeFin)) return null;
		return nc.codeFin;
	}

}
