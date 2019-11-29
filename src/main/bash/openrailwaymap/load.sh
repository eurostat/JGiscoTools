#!/usr/bin/env bash

#https://blog-en.openalfa.com/how-to-query-openstreetmap-using-the-overpass-api
#https://stackoverflow.com/questions/31879288/overpass-api-way-coordinates

#http://www.overpass-api.de/api/
#http://overpass.osm.rambler.ru/cgi/
#http://api.openstreetmap.fr/api/

#http://www.overpass-api.de/api/status
#[timeout:25];


#  ["key"]            /* filter objects tagged with this key and any value */
#  [!"key"]           /* filter objects not tagged with this key and any value */
#  ["key"="value"]    /* filter objects tagged with this key and this value */
#  ["key"!="value"]   /* filter objects tagged with this key but not this value */
#  ["key"~"value"]    /* filter objects tagged with this key and a value matching a regular expression */
#  ["key"!~"value"    /* filter objects tagged with this key but a value not matching a regular expression */
#  [~"key"~"value"]   /* filter objects tagged with a key and a value matching regular expressions */
#  [~"key"~"value",i] /* filter objects tagged with a key and a case-insensitive value matching regular expressions */

#http://wiki.openstreetmap.org/wiki/OpenRailwayMap/Tagging
#meta

#See https://github.com/OpenRailwayMap/OpenRailwayMap/tree/master/import

cd ~/Bureau/gisco_rail/orm

echo "Load data from with overpass API"
mkdir -p osmxml
fil="[railway][railway!=razed][railway!=light_rail][railway!=subway][railway!=tram][railway!=miniature][railway!=switch][railway!=railway_crossing][railway!=level_crossing][railway!=crossing][railway!=derail][railway!=buffer_stop][!subway][railway!=milestone][railway!=signal_box][railway!=interlocking][railway!=crossing_box][railway!=blockpost][railway!=tram_stop][railway!=service_station][railway!=stop][railway!=turntable][railway!=vacancy_detection][railway!=isolated_track_section][railway!=owner_change]"
for cnt in "SE"
#for cnt in "BE" "BG" "CH" "CY" "CZ" "DE" "DK" "EE" "ES" "FI" "FR" "GB" "GR" "HU" "IE" "IS" "IT" "LT" "LV" "LU" "MT" "NO" "NL" "PL" "PT" "RO" "SE" "SI" "SK"
do
	echo "****** $cnt ******"
	echo Get raw ORM data for $cnt
	#wget -O osmxml/orm_$cnt.osm "http://overpass-api.de/api/map?data=[out:xml];(area['ISO3166-1:alpha2'=$cnt][admin_level=2];)->.a;(node"$fil"(area.a);way"$fil"(area.a);relation"$fil"(area.a););(._;>;);out;"
	wget -O osmxml/orm_node_$cnt.osm "http://overpass-api.de/api/map?data=[out:xml];(area['ISO3166-1:alpha2'=$cnt][admin_level=2];)->.a;node(area.a)"$fil";out;"
	wget -O osmxml/orm_way_$cnt.osm "http://overpass-api.de/api/map?data=[out:xml];(area['ISO3166-1:alpha2'=$cnt][admin_level=2];)->.a;(way(area.a)"$fil";>;);out;"
	wget -O osmxml/orm_relation_$cnt.osm "http://overpass-api.de/api/map?data=[out:xml];(area['ISO3166-1:alpha2'=$cnt][admin_level=2];)->.a;(relation(area.a)"$fil";>>;);out;"
done

#node[power=""];          // not supported
#node[power~"^$"];        // use regular expression instead





#universities
#http://overpass-api.de/api/map?data=[out:xml];(area['ISO3166-1:alpha2'=FR][admin_level=2];)->.a;(node[amenity=university](area.a);way[amenity=university](area.a);relation[amenity=university](area.a););(._;>;);out;
#http://overpass-api.de/api/map?data=[out:xml];(node[amenity=university];way[amenity=university];relation[amenity=university];);(._;>;);out;

#pharmacy
#http://overpass-api.de/api/map?data=[out:xml];(area['ISO3166-1:alpha2'=FR][admin_level=2];)->.a;(node[amenity=pharmacy](area.a);way[amenity=pharmacy](area.a);relation[amenity=pharmacy](area.a););(._;>;);out;


