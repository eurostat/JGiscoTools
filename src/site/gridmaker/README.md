# Grid maker

[GridMaker](https://github.com/eurostat/GridMaker) can be used as a Java library. To quickly setup a development environment, see [these instructions](https://eurostat.github.io/README/howto/java_eclipse_maven_git_quick_guide).

Download and install [GridMaker](https://github.com/eurostat/GridMaker) with:

```
git clone https://github.com/eurostat/GridMaker.git
cd GridMaker
mvn clean install
```

and then use it in your Java project as a dependency by adding it to the *pom.xml* file:

```
<dependencies>
	...
	<dependency>
		<groupId>eu.europa.ec.eurostat</groupId>
		<artifactId>GridMaker</artifactId>
		<version>1.0</version>
	</dependency>
</dependencies>
```

You can then start using [GridMaker](https://github.com/eurostat/GridMaker) in your project. Here is an example showing how to create a 10m resolution grid over 1km² starting at point (0,0):

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

Input geometries can be loaded from [*Shapefile*](https://en.wikipedia.org/wiki/Shapefile) or [*GeoJSON*](https://geojson.org/) files or simply specified as rectangular extent. The grid cell geometries can be squared surfaces or points located at the center of these cells. Each grid cell is identified with a standard code such as *CRS3035RES200mN1453400E1452800*. The output grid cells can be saved as [*Shapefile*](https://en.wikipedia.org/wiki/Shapefile) or [*GeoJSON*](https://geojson.org/) files.
