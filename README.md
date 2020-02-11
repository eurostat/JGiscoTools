# JGiscoTools

[JGiscoTools](https://github.com/eurostat/JGiscoTools) is a Java library for the manipulation of geographical and statistical data, with a focus on European data produced by [Eurostat](http://ec.europa.eu/eurostat).

JGiscoTools allows:
- Manipulation and transformation of vector geographical data such as clustering, generalisation, deformation, filtering, edge matching and partitionning.
- Generalisation of geographical tesselations.
- Routing and accessibility computation.
- [Production of gridded datasets](https://github.com/eurostat/JGiscoTools/tree/master/src/site/gridmaker) in various GIS formats.
- Change detection between two version of a same dataset.
- Automatic production statistical maps (with a focus on maps based on Eurostat data and NUTS regions).
- Various analyses based on NUTS regions and NUTS codes.
- Some experiments on the combined use of geographical and statistical data such as
  - Statistical data disaggregation based on [dasymetric mapping](https://en.wikipedia.org/wiki/Dasymetric_map).
  - The computation of the intersection matrix between two statistical units datasets.
- ...

[JGiscoTools](https://github.com/eurostat/JGiscoTools) is mainly based on [GeoTools](http://www.geotools.org/), [JTS Topology Suite](https://locationtech.github.io/jts/) and [java4eurostat](https://github.com/eurostat/java4eurostat) libraries.

## Quick start

### Load

To load geographical features from a [GeoPackage](https://www.geopackage.org/) or a [Shapefile](https://en.wikipedia.org/wiki/Shapefile) file, use:

```java
//load gpkg file
Collection<Feature> featuresGPKG = GeoPackageUtil.getFeatures("C:/myFile.gpkg");
//load shp file
Collection<Feature> featuresSHP = SHPUtil.getFeatures("C:/myFile.shp");
```

### Read

An `Feature` object is simple an object with a geometry and some attributes. This information can be accessed with:

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
f.setGeometry(f.getGeometry().buffer(10));
```

### Save

To save data as [GeoPackage](https://www.geopackage.org/) or [Shapefile](https://en.wikipedia.org/wiki/Shapefile) file, use:

```java
//save as gpkg file
GeoPackageUtil.save(features, "C:/myFile.gpkg", crs, true);
//save as shp file
SHPUtil.save(features, "C:/myFile.shp", crs, true);
```

The CRS (Coordinate Reference System) has to be specified, either from an input dataset, or from its EPSG code:

```java
CoordinateReferenceSystem crsGPKG = GeoPackageUtil.getCRS("C:/myFile.gpkg");
CoordinateReferenceSystem crsSHP = SHPUtil.getCRS("C:/myFile.shp");
CoordinateReferenceSystem crsEPSG = CRS.decode("EPSG:3035")
```

## Documentation

See the [Javadoc API](https://eurostat.github.io/JGiscoTools/src/site/apidocs/).

## Setup

[JGiscoTools](https://github.com/eurostat/JGiscoTools) uses [Apache Maven](http://maven.apache.org/). To use JGiscoTools, add it as a dependency to the *pom.xml* file:

```
<dependency>
	<groupId>eu.europa.ec.eurostat</groupId>
	<artifactId>JGiscoTools</artifactId>
	<version>X.Y.Z</version>
</dependency>
```

Where *X.Y.Z* is the current version number, as available [Maven central repository](https://search.maven.org/artifact/eu.europa.ec.eurostat/JGiscoTools).

## Support and contribution

Feel free to [ask support](https://github.com/eurostat/JGiscoTools/issues/new), fork the project or simply star it (it's always a pleasure).
