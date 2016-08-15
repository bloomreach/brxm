/*
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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

(function () {
    'use strict';

    function getMapType(mapType) {
        if (mapType === 'ROADMAP') {
            return google.maps.MapTypeId.ROADMAP;
        }
        else if (mapType === 'SATELLITE') {
            return google.maps.MapTypeId.SATELLITE;
        }
        else if (mapType === 'TERRAIN') {
            return google.maps.MapTypeId.TERRAIN;
        }
        else {
            return google.maps.MapTypeId.HYBRID;
        }
    }

    function geoCodeAddress(address, map) {
        var myGeoCoder = new google.maps.Geocoder();
        myGeoCoder.geocode({
              address: address
          },
          function (results, status) {
              if (status === google.maps.GeocoderStatus.OK) {
                  map.setCenter(results[0].geometry.location);
                  new google.maps.Marker({
                      map: map,
                      position: results[0].geometry.location
                  });
              }
          });
    }

    var he, gm;
    if (!window.HippoEssentials) {
        window.HippoEssentials = {};
    }
    he = window.HippoEssentials;

    if (!he.GoogleMaps) {
        he.GoogleMaps = {
            queue: []
        };
    }
    gm = he.GoogleMaps;

    if (!gm.init) {
        gm.init = function() {
            for (var i = 0; i < gm.queue.length; i++) {
                gm.queue[i]();
            }
            gm.queue = [];
        };
    }

    if (!gm.render) {
        gm.render = function (elementId, address, longitude, latitude, zoomFactor, mapType) {
            if (!google) {
                if (window.console) {
                    console.warn('Could not find Google Maps API, please make sure it is loaded correctly before creating a ' +
                      'new Google Map instance. This is typically done by inserting ' +
                      '<script src="https://maps.googleapis.com/maps/api/js"></script> into the page.');
                }
                return;
            }

            var latlng;
            if (latitude !== '' || longitude !== '') {
                latlng = new google.maps.LatLng(latitude, longitude);
            } else {
                latlng = new google.maps.LatLng(0, 0);
            }
            var el = document.getElementById(elementId);
            var mapOptions = {
                center: latlng,
                zoom: zoomFactor,
                mapTypeId: getMapType(mapType)
            };
            var map = new google.maps.Map(el, mapOptions);
            if (address !== '') {
                geoCodeAddress(address, map);
            }
        };
    }
})();
