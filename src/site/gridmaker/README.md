# Grid maker

[GridMaker](https://github.com/eurostat/GridMaker) can be used as a Java library as part of JGiscoTools.

Here is an example showing how to create a 10m resolution grid over 1km² starting at point (0,0):

```java

StatGrid grid = new StatGrid()
		.setResolution(10)
		.setGeometryToCover(new Envelope(0, 1000, 0, 1000));

Collection<Feature> cells = grid.getCells();
```

This other example creates a 5km resolution grid covering Luxembourg (code LU) and a 1km margin, based on the European ETRS89-LAEA coordinate reference system ([EPSG:3035](https://spatialreference.org/ref/epsg/etrs89-etrs-laea/)). The cells are saved as a **.shp* file:

```java
//get country geometry
Geometry cntGeom = CountriesUtil.getEuropeanCountry("LU", true).getDefaultGeometry();

//build cells
StatGrid grid = new StatGrid()
		.setResolution(5000)
		.setEPSGCode("3035")
		.setGeometryToCover(cntGeom)
		.setToleranceDistance(1000);

//save cells as SHP file
SHPUtil.saveSHP(grid.getCells(), "path_to_my/file.shp", CRS.decode("EPSG:3035"));
```

Input geometries can be loaded from [*GeoPackage*](https://www.geopackage.org/), [*Shapefile*](https://en.wikipedia.org/wiki/Shapefile) or [*GeoJSON*](https://geojson.org/) files or simply specified as rectangular extent. The grid cell geometries can be squared surfaces or points located at the center of these cells. Each grid cell is identified with a standard code such as *CRS3035RES200mN1453400E1452800*. The output grid cells can be saved as [*GeoPackage*](https://www.geopackage.org/), [*Shapefile*](https://en.wikipedia.org/wiki/Shapefile) or [*GeoJSON*](https://geojson.org/) files.

For further utilisation, see the [Javadoc](https://eurostat.github.io/JGiscoTools/src/site/apidocs/eu/europa/ec/eurostat/jgiscotools/grid/package-summary.html).
