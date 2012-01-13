/*
 *  Copyright 2008,2011 Hippo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @module hippotoggle
 * @requires yahoo, dom, cookie
 *
 */

YAHOO.namespace("hippo");

(function() {
    var Dom = YAHOO.util.Dom, Cookie = YAHOO.util.Cookie;
    var CLOSED_BOXES = 'ConsoleClosedBoxes';

    YAHOO.hippo.ToggleInit = function(d) {
      var c = getCookie(CLOSED_BOXES), l = Dom.getElementsByClassName('toggle-header', 'h3', d || null).length;
      if(!c || !l) {
        return;
      }
      for(var i=1; i<=l; i++) {
        if(c['box-' + i] == '1') {
          hideBox(i);
        }
      }
    };

    YAHOO.hippo.ToggleBox = function(boxName) {
      var box = Dom.get('toggle-box-' + boxName);
      if (box == null) {
        return;
      }
      var dStyle = Dom.getStyle(box, 'display');
      if (dStyle == 'none') {
        showBox(boxName);
      } else if (dStyle == 'block') {
        hideBox(boxName);
      }
    };

    function showBox(boxName) {
        updateBox(boxName, true, 'group-collapsed', 'group-expanded');
    }

    function hideBox(boxName) {
        updateBox(boxName, false, 'group-expanded', 'group-collapsed');
    }

    function updateBox(boxName, display, oldGroup, newGroup) {
        if (boxName === '') {
            return;
        }
        var box = Dom.get('toggle-box-' + boxName);
        if (box != null) {
            Dom.setStyle(box, 'display', display ? 'block' : 'none');
        }
        var toggled = Dom.get('toggle-' + boxName);
        if (toggled != null) {
            toggled.src = toggled.src.replace(oldGroup, newGroup);
        }

        var cook = getCookie(CLOSED_BOXES) || {};
        cook['box-' + boxName] = display ? 0 : 1;
        setCookie(CLOSED_BOXES, cook, 30);
    }

    function setCookie(name, value, exdays) {
        var expires = new Date();
        expires.setDate(expires.getDate() + exdays);
        Cookie.setSubs(name, value, {
          expires: expires
        });
    }

    function getCookie(name) {
        return Cookie.getSubs(name);
    }
})();
