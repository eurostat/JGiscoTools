package eu.europa.ec.eurostat.jgiscotools.geocoding;

import eu.europa.ec.eurostat.jgiscotools.deprecated.NUTSUtils;

public class GeocodingAddress {

	//TODO
	//check ESRI geocoder
	//https://developers.arcgis.com/rest/geocode/api-reference/overview-world-geocoding-service.htm

	public String street;

	public String housenumber;
	public String streetname;

	public String city;
	public String countryCode;
	public String postalcode;

	public GeocodingAddress(String street, String housenumber, String streetname, String city, String countryCode, String postalcode) {
		this.street = street;
		this.housenumber = housenumber;
		this.streetname = streetname;
		this.city = city;
		this.countryCode = countryCode;
		this.postalcode = postalcode;
	}

	public String getCountryName() {
		return NUTSUtils.getName(this.countryCode);
	}

}
