package eu.europa.ec.eurostat.jgiscotools.geocoding.base;

public class GeocodingAddress {

	public String street;

	public String housenumber;
	public String streetname;

	public String city;
	public String countryCode;
	public String countryName;
	public String postalcode;

	public GeocodingAddress(String street, String housenumber, String streetname, String city, String countryCode, String countryName, String postalcode) {
		this.street = street;
		this.housenumber = housenumber;
		this.streetname = streetname;
		this.city = city;
		this.countryCode = countryCode;
		this.countryName = countryName;
		this.postalcode = postalcode;
	}

}
