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
fil="[amenity=hospital]"
fil="["amenity"="hospital"][!"amenity"="parking_entrance"][!"highway"="crossing"][!"entrance"="main"][!"crossing"="zebra"]"
for cnt in "SE"
#for cnt in "BE" "BG" "CH" "CY" "CZ" "DE" "DK" "EE" "ES" "FI" "FR" "GB" "GR" "HU" "IE" "IS" "IT" "LT" "LV" "LU" "MT" "NO" "NL" "PL" "PT" "RO" "SE" "SI" "SK"
do
	echo "****** $cnt ******"
	echo Get raw ORM data for $cnt
	#wget -O osmxml/orm_$cnt.osm "http://overpass-api.de/api/map?data=[out:xml];(area['ISO3166-1:alpha2'=$cnt][admin_level=2];)->.a;(node"$fil"(area.a);way[railway](area.a);relation[railway](area.a););(._;>;);out;"
	wget -O osmxml/orm_node_$cnt.osm "http://overpass-api.de/api/map?data=[out:xml];(area['ISO3166-1:alpha2'=$cnt][admin_level=2];)->.a;node(area.a)"$fil";out;"
	wget -O osmxml/orm_way_$cnt.osm "http://overpass-api.de/api/map?data=[out:xml];(area['ISO3166-1:alpha2'=$cnt][admin_level=2];)->.a;(way(area.a)"$fil";>;);out;"
	wget -O osmxml/orm_relation_$cnt.osm "http://overpass-api.de/api/map?data=[out:xml];(area['ISO3166-1:alpha2'=$cnt][admin_level=2];)->.a;(relation(area.a)"$fil";>>;);out;"
done

#node[power=""];          // not supported
#node[power~"^$"];        // use regular expression instead


http://overpass-api.de/api/map?data=[out:xml];(area[%27ISO3166-1:alpha2%27=LU][admin_level=2];)->.a;node(area.a)[amenity];out;

#map(5): http://overpass-api.de/api/map?data=[out:xml];(area[%27ISO3166-1:alpha2%27=LU][admin_level=2];)-%3E.a;(node[%22amenity%22=%22hospital%22](area.a);way[%22amenity%22=%22hospital%22](area.a);relation[%22amenity%22=%22hospital%22](area.a););(._;%3E;);out;
#map(6): http://overpass-api.de/api/map?data=[out:xml];(area[%27ISO3166-1:alpha2%27=LU][admin_level=2];)-%3E.a;(node[%22amenity%22=%22hospital%22](area.a);way[%22amenity%22=%22hospital%22](area.a);relation[%22amenity%22=%22hospital%22](area.a);area[%22amenity%22=%22hospital%22](area.a););(._;%3E;);out;
#map(7): http://overpass-api.de/api/map?data=[out:xml];(area[%27ISO3166-1:alpha2%27=LU][admin_level=2];)-%3E.a;(node[%22amenity%22=%22hospital%22](area.a);way[%22amenity%22=%22hospital%22](area.a);relation[%22amenity%22=%22hospital%22](area.a);area[%22amenity%22=%22hospital%22](area.a););(._;%3E;);out;
#map(8): http://overpass-api.de/api/map?data=[out:xml];(area[%27ISO3166-1:alpha2%27=LU][admin_level=2];)-%3E.a;(node["amenity"="hospital"](area.a);area["amenity"="hospital"]["building"="hospital"](area.a););(._;%3E;);out;

#(did not work) #map(6.1): http://overpass-api.de/api/map?data=[out:xml];(area['ISO3166-1:alpha2'=LU][admin_level=2];)->.a;(node["amenity"="hospital"][!"crossing"](area.a);node["healthcare"="hospital"](area.a);area["amenity"="hospital"](area.a);area["healthcare"="hospital"](area.a););(._;>;);out;

#(no spatial data and has hospitals in the UK?) #map(7.1)http://overpass-api.de/api/map?data=[out:xml];(area['ISO3166-1:alpha2'=LU][admin_level=2];)->.a;(node["amenity"="hospital"](area.a);node["healthcare"="hospital"](area.a);area["amenity"="hospital"](area.a);area["healthcare"="hospital"](area.a););(._;>;);out;

http://overpass-api.de/api/map?data=[out:xml];(area[%27ISO3166-1:alpha2%27=LU][admin_level=2];)-%3E.a;(node[%22amenity%22=%22hospital%22](area.a);way[%22amenity%22=%22hospital%22](area.a);relation[%22amenity%22=%22hospital%22](area.a);area[%22amenity%22=%22hospital%22](area.a););(._;%3E;);out;

http://overpass-api.de/api/map?data=[out:xml];
(area['ISO3166-1:alpha2'=LU][admin_level=2];)->.a;
(node["amenity"="hospital"](area.a);
way["amenity"="hospital"](area.a);
relation["amenity"="hospital"](area.a);
node["healthcare"="hospital"](area.a);
way["healthcare"="hospital"](area.a);
relation["healthcare"="hospital"](area.a);
node["building"="hospital"](area.a);
way["building"="hospital"](area.a);
relation["building"="hospital"](area.a);
node["building"="yes"]["healthcare"="hospital"](area.a);
way["building"="yes"]["healthcare"="hospital"](area.a);
relation["building"="yes"]["healthcare"="hospital"](area.a););(._;>;);out;

http://overpass-api.de/api/map?data=[out:xml];
(area['ISO3166-1:alpha2'=LU][admin_level=2];)->.a;
(nwr["amenity"="hospital"](area.a);
nwr["healthcare"="hospital"](area.a);
nwr["building"="hospital"](area.a);
nwr["building"="yes"]["healthcare"="hospital"](area.a);
way["building"="yes"]["healthcare"="hospital"](area.a););(._;>;);out;

http://overpass-api.de/api/map?data=[out:xml];(area['ISO3166-1:alpha2'=LU][admin_level=2];)->.a;(node["amenity"="hospital"](area.a);way["amenity"="hospital"](area.a);relation["amenity"="hospital"](area.a);area["amenity"="hospital"](area.a););(._;>;);out;