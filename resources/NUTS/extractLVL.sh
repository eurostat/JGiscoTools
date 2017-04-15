#!/usr/bin/env bash
for year in "2013"
do
  for lod in "1M" "3M" "10M" "20M" "60M"
  do
    for proj in "LAEA" "ETRS89"
    do
    for level in "0" "1" "2" "3"
    do
      echo "Extract NUTS level $level for $year-$lod-$proj"

      dir="$year/$lod/$proj/"
      mkdir -p $dir/lvl$level

      ogr2ogr -overwrite \
          -sql "SELECT * FROM RG WHERE STAT_LEVL_="$level \
          $dir/lvl$level/RG.shp \
          $dir/RG.shp

      ogr2ogr -overwrite \
          -sql "SELECT * FROM BN WHERE STAT_LEVL_<="$level \
          $dir/lvl$level/BN.shp \
          $dir/BN.shp

    done
    done
  done
done
