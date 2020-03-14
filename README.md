# JGiscoTools

[JGiscoTools](https://github.com/eurostat/JGiscoTools) is a Java library for the manipulation of geospatial and statistical data, with a focus on European data produced by [Eurostat](http://ec.europa.eu/eurostat) and [Eurostat-GISCO](https://ec.europa.eu/eurostat/web/gisco). The main functionalities are listed [here](#components)

[JGiscoTools](https://github.com/eurostat/JGiscoTools) is mainly based on [GeoTools](http://www.geotools.org/), [JTS Topology Suite](https://locationtech.github.io/jts/) and [java4eurostat](https://github.com/eurostat/java4eurostat) libraries.

## Quick start

### Load

To load geographical features from a [GeoPackage](https://www.geopackage.org/), a [Shapefile](https://en.wikipedia.org/wiki/Shapefile) or a [GeoJSON](https://geojson.org/) file, use:

```java
Collection<Feature> featuresGPKG = GeoData.getFeatures("C:/myFile.gpkg");
Collection<Feature> featuresSHP = GeoData.getFeatures("C:/myFile.shp");
Collection<Feature> featuresGEOJSON = GeoData.getFeatures("C:/myFile.geojson");
```

### Read

A `Feature` object has an identifier, a geometry and some attributes. This information can be accessed with:

```java
//load features
Collection<Feature> features = ...;

//print number of features
System.out.println(features.size());

//go through features 
for(Feature f : features) {
	//print the feature identifier
	System.out.println(f.getID());
	//print the feature geometry
	System.out.println(f.getGeometry());
	//print the feature geometry area
	System.out.println(f.getGeometry().getArea());
	//print the feature geometry length
	System.out.println(f.getGeometry().getLength());
	//print the attribute names
	System.out.println(f.getAttributes().keySet());
	//print the attribute "myAttribute1"
	f.getAttribute("myAttribute1");
	//print the attribute "myAttribute2"
	f.getAttribute("myAttribute2");
	...
}
```

### Modify

A `Feature` object can be modified directly. See for example how to change an attribute value and change the geometry with a buffer of 10m distance:

```java
Feature f = ...
f.setAttribute("myAttribute1", "new value");
f.setGeometry( f.getGeometry().buffer(10) );
```

### Save

To save data as a [GeoPackage](https://www.geopackage.org/), a [Shapefile](https://en.wikipedia.org/wiki/Shapefile) or a [GeoJSON](https://geojson.org/) file, use:

```java
GeoData.save(features, "C:/myFile.gpkg", crs);
GeoData.save(features, "C:/myFile.shp", crs);
GeoData.save(features, "C:/myFile.geojson", crs);
```

The CRS (Coordinate Reference System) has to be specified, either from an input dataset, or from its EPSG code:

```java
CoordinateReferenceSystem crs = GeoData.getCRS("C:/myFile.gpkg");
CoordinateReferenceSystem crsEPSG = CRS.decode("EPSG:3035")
```

## More information

### Setup

[JGiscoTools](https://github.com/eurostat/JGiscoTools) uses [Apache Maven](http://maven.apache.org/). To use JGiscoTools, add it as a dependency to the *pom.xml* file:

```
<dependency>
	<groupId>eu.europa.ec.eurostat</groupId>
	<artifactId>JGiscoTools</artifactId>
	<version>X.Y.Z</version>
</dependency>
```

Where *X.Y.Z* is the current version number, as available [Maven central repository](https://search.maven.org/artifact/eu.europa.ec.eurostat/JGiscoTools).

For more information on how to setup a coding environment based on Eclipse, see [this page](https://github.com/eurostat/README/blob/master/docs/howto/java_eclipse_maven_git_quick_guide.md).

### Documentation

See the [Javadoc API](https://eurostat.github.io/JGiscoTools/src/site/apidocs/).

### Components

JGiscoTools allows:
- Manipulation and transformation of vector geographical data such as clustering, generalisation, deformation, filtering, edge matching and partitionning.
- [Generalisation of geographical tesselations](/src/site/regionsimplify) such as administrative units.
- [Production of gridded datasets](/src/site/gridmaker) in various GIS formats.
- [Change detection between two versions of a same dataset](/src/site/changedetection).
- Routing and accessibility computation. (TODO document)
- Automatic production statistical maps (with a focus on maps based on Eurostat data and NUTS regions). (TODO document)
- Various analyses based on NUTS regions and NUTS codes. (TODO document)
- Some experiments on the combined use of geographical and statistical data such as
  - Statistical data disaggregation based on [dasymetric mapping](https://en.wikipedia.org/wiki/Dasymetric_map). (TODO document)
  - The computation of the intersection matrix between two statistical units datasets. (TODO document)
- ...

## Support and contribution

Feel free to [ask support](https://github.com/eurostat/JGiscoTools/issues/new), fork the project or simply star it (it's always a pleasure).
