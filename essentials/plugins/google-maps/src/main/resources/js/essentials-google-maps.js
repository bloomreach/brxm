"use strict";
/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var myGoogleMap;

function getMapType(mapType) {
    if (mapType=="ROADMAP") {
        return google.maps.MapTypeId.ROADMAP;
    }
    else if (mapType=="SATELLITE") {
        return google.maps.MapTypeId.SATELLITE;
    }
    else if (mapType=="TERRAIN") {
        return google.maps.MapTypeId.TERRAIN;
    }
    else {
        return google.maps.MapTypeId.HYBRID;
    }
}

function initializeGoogleMaps(address, longitude, latitude, zoomFactor, mapType) {
    var latlng = new google.maps.LatLng(0, 0);
    if (latitude != "" || longitude != "") {
        latlng = new google.maps.LatLng(latitude, longitude);
    }
    var mapOptions = {
        center: latlng,
        zoom: zoomFactor,
        mapTypeId: getMapType(mapType)
    };
    myGoogleMap = new google.maps.Map(document.getElementById("map-canvas"), mapOptions);
    if (address != "") {
        geoCodeAddress(address);
    }
}

function geoCodeAddress(address) {
    var myGeoCoder = new google.maps.Geocoder();
    myGeoCoder.geocode( {'address': address}, function(results, status) {
        if (status == google.maps.GeocoderStatus.OK) {
            myGoogleMap.setCenter(results[0].geometry.location);
            new google.maps.Marker({
                map: myGoogleMap,
                position: results[0].geometry.location
            });
        } else {
            // geocode failed, do nothing
        }
    });
}
