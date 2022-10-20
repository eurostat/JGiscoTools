/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

/**
 * @author Julien Gaffuri
 *
 */
public class CLC2NUTSAggregation {

	public static void main(String[] args) {
		
		/*
https://www.eea.europa.eu/data-and-maps/figures/corine-land-cover-1990-by-country/legend/image_large
Artificial areas
            "1": "Continuous urban fabric",
            "2": "Discontinuous urban fabric",
            "3": "Industrial or commercial units",
            "4": "Road and rail networks and associated land",
            "5": "Port areas",
            "6": "Airports",
            "7": "Mineral extraction sites",
            "8": "Dump sites",
            "9": "Construction sites",
            "10": "Green urban areas",
            "11": "Sport and leisure facilities",

Arable land and permanent crops
            "12": "Non-irrigated arable land",
            "13": "Permanently irrigated land",
            "14": "Rice fields",
            "15": "Vineyards",
            "16": "Fruit trees and berry plantations",
            "17": "Olive groves",

Pastures and heterogeneous agricultural areas
            "18": "Pastures",
            "19": "Annual crops associated with permanent crops",
            "20": "Complex cultivation patterns",
            "21": "Land principally occupied by agriculture with significant areas of natural vegetation",
            "22": "Agro-forestry areas",

Forest
            "23": "Broad-leaved forest",
            "24": "Coniferous forest",
            "25": "Mixed forest",

Shrubs
            "26": "Natural grasslands",
            "27": "Moors and heathland",
            "28": "Sclerophyllous vegetation",
            "29": "Transitional woodland-shrub",

Open spaces with little or no vegetation
            "30": "sands",
            "31": "Bare rocks",
            "32": "Sparsely vegetated areas",
            "33": "Burnt areas",
            "34": "Glaciers and perpetual snow",

Wetlands and water bodies
            "35": "Inland marshes",
            "36": "Peat bogs",
            "37": "Salt marshes",
            "38": "Salines",
            "39": "Intertidal flats",
            "40": "Water courses",
            "41": "Water bodies",
            "42": "Coastal lagoons",
            "43": "Estuaries",
            "44": "Sea and ocean",
            "48": "No data"
		 */
		
		
		
		//load CLC
		String clcFile = "/home/juju/Bureau/gisco/clc/u2018_clc2018_v2020_20u1_geoPackage/DATA/U2018_CLC2018_V2020_20u1.gpkg";

		//load NUTS
		String nutsFile = "/home/juju/Bureau/gisco/geodata/gisco/GISCO.NUTS_RG_100K_2021.gpkg";

		
	}
	
	
}
