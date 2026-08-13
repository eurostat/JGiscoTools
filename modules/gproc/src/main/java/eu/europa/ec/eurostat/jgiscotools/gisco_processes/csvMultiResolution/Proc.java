package eu.europa.ec.eurostat.jgiscotools.gisco_processes.csvMultiResolution;

import java.util.ArrayList;
import java.util.Map;

import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

public class Proc {
    public static void main(String[] args) {
        System.out.println("Hello World!");

        ArrayList<Map<String, String>> cells = CSVUtil.load("/home/juju/gisco/census_2021_v3_production/ESTAT_Census_2021_V3.csv");
        //GRD_ID,T,M,F,Y_LT15,Y_1564,Y_GE65,EMP,NAT,EU_OTH,OTH,SAME,CHG_IN,CHG_OUT,LAND_SURFACE,POPULATED,CNTR_ID

        System.out.println("Cells: " + cells.size());
    }
}
