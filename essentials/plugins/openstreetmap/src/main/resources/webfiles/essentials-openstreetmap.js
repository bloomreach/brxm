/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

  function placeMap (elementId, latitude, longitude, address, zoomFactor, mapType, mapOverlay, showMarker, markerText) {
    var container = L.DomUtil.get(elementId);
    if (container._leaflet_id) {
      return; // map already initialized
    }
    var mymap = L.map(elementId).setView([latitude, longitude], zoomFactor);

    L.tileLayer(mapType, {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      detectRetina: false,
      maxZoom: 18
    }).addTo(mymap);

    if (mapOverlay) {
      L.tileLayer(mapOverlay, {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
        detectRetina: false,
        maxZoom: 18
      }).addTo(mymap);
    }
    
    if (showMarker !== 'no') {
      var marker = L.marker([latitude, longitude]).addTo(mymap);
      var text;
      switch (showMarker) {
        case 'withoutText':
          break;
        case 'withAddress':
          text = address;
          break;
        default:
          text = markerText;
      }
      if (text) {
        marker.bindPopup(text).openPopup(); 
      }
    }
  }

  var he, osm;
  if (!window.HippoEssentials) {
    window.HippoEssentials = {};
  }
  he = window.HippoEssentials;

  if (!he.OpenStreetMap) {
    he.OpenStreetMap = {};
  }
  osm = he.OpenStreetMap;

  if (!osm.showMap) {
    osm.showMap =   function showMap (elementId, latitude, longitude, address, zoomFactor, mapType, mapOverlay, showMarker, markerText) {
      const DEFAULT_LATITUDE = '37.390088';
      const DEFAULT_LONGITUDE = '-122.066242';
      const myLatitude = latitude || DEFAULT_LATITUDE;
      const myLongitude = longitude || DEFAULT_LONGITUDE;

      if (myLatitude === '37.390088' && myLongitude === '-122.066242' && address) {
        $.getJSON('https://nominatim.openstreetmap.org/search?q=' + address + '&limit=1&format=json')
          .done(function (result) {
            if (result[0]) {
              placeMap(elementId, result[0].lat, result[0].lon, address, zoomFactor, mapType, mapOverlay, showMarker, markerText);
            } else {
              console.warn('Address lookup gave no result. Using default coordinates.');
              placeMap(elementId, DEFAULT_LATITUDE, DEFAULT_LONGITUDE, address, zoomFactor, mapType, mapOverlay, 'error', 'Address lookup failed.');
            }
          })
          .fail(function () {
            console.warn('Address lookup failed. Cannot render the map.');
          });
      } else {
        placeMap(elementId, myLatitude, myLongitude, address, zoomFactor, mapType, mapOverlay, showMarker, markerText);
      }
    }
  }
})();
