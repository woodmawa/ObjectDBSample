package com.softwood.model

import javax.persistence.Embeddable

@Embeddable
class GeoAddress {
    String streetNumber
    String street
    String city
    String stateOrCounty
    String country
    String postalCode
}
