package eu.europa.ec.eurostat.jgiscotools.gisco_processes.csvMultiResolution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import eu.europa.ec.eurostat.jgiscotools.grid.processing.GridMultiResolutionProduction;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class Proc {
    public static void main(String[] args) {
        System.out.println("Loading");
        List<Map<String, String>> cells = CSVUtil.load("/home/juju/gisco/census_2021_v3_production/ESTAT_Census_2021_V3.csv");
        //GRD_ID,T,M,F,Y_LT15,Y_1564,Y_GE65,EMP,NAT,EU_OTH,OTH,SAME,CHG_IN,CHG_OUT,LAND_SURFACE,POPULATED,CNTR_ID

        //remove unecessary columns
        for(Map<String, String> cell : cells) {
            cell.remove("CNTR_ID");
            cell.remove("POPULATED");
            cell.remove("LAND_SURFACE");
        }
        System.out.println("Cells: " + cells.size());

        System.out.println("Filter");
        cells = cells.stream().filter(c -> Integer.parseInt(c.get("T")) > 0 && c.get("GRD_ID") != null && !c.get("GRD_ID").contains("unallocated")).collect(Collectors.toList());
        System.out.println("Cells: " + cells.size());

        int[] resolutions = {2, 5, 10, 20, 50, 100};
        for(int resolution : resolutions) {
            System.out.println("Aggregate "+resolution+"km");
            cells = GridMultiResolutionProduction.gridAggregation(cells, "GRD_ID", resolution*1000, 10000);
            System.out.println("Cells: " + cells.size());

            System.out.println("Save");
            CSVUtil.save(cells, "/home/juju/gisco/census_2021_v3_production/multi_res/ESTAT_Census_2021_V3_" + resolution + "km.csv");
        }

    }
}
