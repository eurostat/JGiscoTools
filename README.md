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

### Loading/saving data

TODO

## Documentation

See the [Javadoc API](https://eurostat.github.io/JGiscoTools/src/site/apidocs/).

## Support and contribution

Feel free to [ask support](https://github.com/eurostat/JGiscoTools/issues/new), fork the project or simply star it (it's always a pleasure).
