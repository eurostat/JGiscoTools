package eu.europa.ec.eurostat.jgiscotools.gisco_processes.csvMultiResolution;

import java.util.ArrayList;
import java.util.Map;

import eu.europa.ec.eurostat.jgiscotools.grid.processing.GridMultiResolutionProduction;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class Proc {
    public static void main(String[] args) {
        System.out.println("Loading");
        ArrayList<Map<String, String>> cells = CSVUtil.load("/home/juju/gisco/census_2021_v3_production/ESTAT_Census_2021_V3.csv");
        //GRD_ID,T,M,F,Y_LT15,Y_1564,Y_GE65,EMP,NAT,EU_OTH,OTH,SAME,CHG_IN,CHG_OUT,LAND_SURFACE,POPULATED,CNTR_ID

        //remove unecessary columns
        for(Map<String, String> cell : cells) {
            cell.remove("CNTR_ID");
            cell.remove("POPULATED");
            cell.remove("LAND_SURFACE");
        }
        System.out.println("Cells: " + cells.size());

        cells = GridMultiResolutionProduction.gridAggregation(cells, "GRD_ID", 5000, 10000);
        System.out.println("Cells: " + cells.size());
        
        CSVUtil.save(cells, "/home/juju/gisco/census_2021_v3_production/multi_res/ESTAT_Census_2021_V3_5km.csv");
    }
}
