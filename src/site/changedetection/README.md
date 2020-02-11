# Change detection

To analyse the changes between two versions of a same datasets, use:

```java
ChangeDetection cd = new ChangeDetection(featuresIni, featuresFin);
```

where `featuresIni` and `featuresFin` are the two datasets to compare, in the initial and final versions loaded, for example, from a [GeoPackage](https://www.geopackage.org/) source with:

```java
Collection<Feature> featuresIni = GeoPackageUtil.getFeatures("C:/myDatasetVersion2015.gpkg");
Collection<Feature> featuresFin = GeoPackageUtil.getFeatures("C:/myDatasetVersion2020.gpkg");
```

The change detection is based on an identifier, which is expected to be stable between the two versions (identical and similar features in both versions should have the same identifier). This attribute used as an identifier should be specified on both dataset versions with:

```java
FeatureUtil.setId(featuresIni, "identifierAttribute");
FeatureUtil.setId(featuresFin, "identifierAttribute");
```

## Get changes

To retrieve the changes between the two versions, use:

```java
Collection<Feature> changes = cd.getChanges();
```

This new datasets contains:
- Features that have been **deleted** (attribute *change* set to *D*)
- Features that have been **inserted** (attribute *change* set to *I*)
- Features that have been **modified**,
    * Either their geometry (attribute *change* set to *G*)
    * attribute values (attribute *change* set to *An* where *n* is the number of attributes which changed)
    * or both (attribute *change* set to *GAn*)

(TODO: show example, with snapshot)

Unchanged features can also be retrieved with:

```java
Collection<Feature> unchanged = cd.getUnchanged();
```

## Analyse changes

To have an overview of the geometrical changes use: 

```java
Collection<Feature> hfgeoms = cd.getHausdorffGeomChanges();
```

This produces a set of linear features representing the [Hausdorf segment](https://en.wikipedia.org/wiki/Hausdorff_distance) between the two versions of the geometries.


```java
Collection<Feature> geomch = cd.getGeomChanges();
```


For further analysis of the changes, use the following methods.

```java
Collection<Feature> sus = ChangeDetection.findIdStabilityIssues(changes, 500);
```

## Apply changes

