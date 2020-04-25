#!/usr/bin/env bash

for year in "2013"
do
  for lod in "1M" "3M" "10M" "20M" "60M"
  do
    for type in "BN" "RG" "SEPA" "LB" "JOIN"
    do
      "Project NUTS $year $lod $type"
      #ogr2ogr -overwrite -t_srs EPSG:3035 2013/1M/LAEA/RG.shp 2013/1M/ETRS89/RG.shp
	done
  done
done


ogr2ogr -overwrite -t_srs EPSG:3035 2013/1M/LAEA/RG.shp 2013/1M/ETRS89/RG.shp
ogr2ogr -overwrite -t_srs EPSG:3035 2013/1M/LAEA/BN.shp 2013/1M/ETRS89/BN.shp
ogr2ogr -overwrite -t_srs EPSG:3035 2013/1M/LAEA/LB.shp 2013/1M/ETRS89/LB.shp
ogr2ogr -overwrite -t_srs EPSG:3035 2013/1M/LAEA/JOIN.shp 2013/1M/ETRS89/JOIN.shp
ogr2ogr -overwrite -t_srs EPSG:3035 2013/1M/LAEA/SEPA.shp 2013/1M/ETRS89/SEPA.shp

ogr2ogr -overwrite -t_srs EPSG:3035 2013/3M/LAEA/RG.shp 2013/3M/ETRS89/RG.shp
ogr2ogr -overwrite -t_srs EPSG:3035 2013/3M/LAEA/BN.shp 2013/3M/ETRS89/BN.shp
ogr2ogr -overwrite -t_srs EPSG:3035 2013/3M/LAEA/LB.shp 2013/3M/ETRS89/LB.shp
ogr2ogr -overwrite -t_srs EPSG:3035 2013/3M/LAEA/JOIN.shp 2013/3M/ETRS89/JOIN.shp
ogr2ogr -overwrite -t_srs EPSG:3035 2013/3M/LAEA/SEPA.shp 2013/3M/ETRS89/SEPA.shp

ogr2ogr -overwrite -t_srs EPSG:3035 2013/10M/LAEA/RG.shp 2013/10M/ETRS89/RG.shp
ogr2ogr -overwrite -t_srs EPSG:3035 2013/10M/LAEA/BN.shp 2013/10M/ETRS89/BN.shp
ogr2ogr -overwrite -t_srs EPSG:3035 2013/10M/LAEA/LB.shp 2013/10M/ETRS89/LB.shp
ogr2ogr -overwrite -t_srs EPSG:3035 2013/10M/LAEA/JOIN.shp 2013/10M/ETRS89/JOIN.shp
ogr2ogr -overwrite -t_srs EPSG:3035 2013/10M/LAEA/SEPA.shp 2013/10M/ETRS89/SEPA.shp

ogr2ogr -overwrite -t_srs EPSG:3035 2013/20M/LAEA/RG.shp 2013/20M/ETRS89/RG.shp
ogr2ogr -overwrite -t_srs EPSG:3035 2013/20M/LAEA/BN.shp 2013/20M/ETRS89/BN.shp
ogr2ogr -overwrite -t_srs EPSG:3035 2013/20M/LAEA/LB.shp 2013/20M/ETRS89/LB.shp
ogr2ogr -overwrite -t_srs EPSG:3035 2013/20M/LAEA/JOIN.shp 2013/20M/ETRS89/JOIN.shp
ogr2ogr -overwrite -t_srs EPSG:3035 2013/20M/LAEA/SEPA.shp 2013/20M/ETRS89/SEPA.shp

ogr2ogr -overwrite -t_srs EPSG:3035 2013/60M/LAEA/RG.shp 2013/60M/ETRS89/RG.shp
ogr2ogr -overwrite -t_srs EPSG:3035 2013/60M/LAEA/BN.shp 2013/60M/ETRS89/BN.shp
ogr2ogr -overwrite -t_srs EPSG:3035 2013/60M/LAEA/LB.shp 2013/60M/ETRS89/LB.shp
ogr2ogr -overwrite -t_srs EPSG:3035 2013/60M/LAEA/JOIN.shp 2013/60M/ETRS89/JOIN.shp
ogr2ogr -overwrite -t_srs EPSG:3035 2013/60M/LAEA/SEPA.shp 2013/60M/ETRS89/SEPA.shp

