/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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

(function () {
    var Dom = YAHOO.util.Dom, Cookie = YAHOO.util.Cookie,
        CLOSED_BOXES = 'ConsoleClosedBoxes';

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

    function updateBox(boxName, display, oldGroup, newGroup) {
        var box, toggled, cook;

        if (boxName === '') {
            return;
        }
        box = Dom.get('toggle-box-' + boxName);
        if (box !== null && box !== undefined) {
            Dom.setStyle(box, 'display', display ? 'block' : 'none');
        }
        toggled = Dom.get('toggle-' + boxName);
        if (toggled !== null && toggled !== undefined) {
            toggled.src = toggled.src.replace(oldGroup, newGroup);
        }

        cook = getCookie(CLOSED_BOXES) || {};
        cook['box-' + boxName] = display ? 0 : 1;
        setCookie(CLOSED_BOXES, cook, 30);
    }

    function showBox(boxName) {
        updateBox(boxName, true, 'group-collapsed', 'group-expanded');
    }

    function hideBox(boxName) {
        updateBox(boxName, false, 'group-expanded', 'group-collapsed');
    }

    YAHOO.hippo.ToggleInit = function (d) {
        var c = getCookie(CLOSED_BOXES),
            l = Dom.getElementsByClassName('toggle-header', 'h3', d || null).length,
            m = Dom.getElementsByClassName('toggle-header', 'h4', d || null),
            n, i, id, boxName, boxId;

        if (!c || !l) {
            return;
        }

        // toggle big boxes
        for (i = 0; i <= l; i++) {
            if (c['box-' + i] === '1') {
                hideBox(i);
            }
        }

        // toggle property namespace boxes of big box 2 ("Properties")
        if (m) {
            for (i = 0; i <= m.length; i++) {
                n = m[i];
                if (n) {
                    id = n.id; // 'toggle-header-jcr'
                    if (id) {
                        boxName = id.replace('header', 'box'); // 'toggle-box-jcr'
                        boxId = boxName.substr(7); // 'box-jcr'
                        if (c[boxId] === '1') {
                            hideBox(boxId.substr(4)); // 'jcr'
                        }
                    }
                }
            }
        }
    };

    YAHOO.hippo.ToggleBox = function (boxName) {
        var box, dStyle;

        box = Dom.get('toggle-box-' + boxName);
        if (box === null || box === undefined) {
            return;
        }
        dStyle = Dom.getStyle(box, 'display');
        if (dStyle === 'none') {
            showBox(boxName);
        } else if (dStyle === 'block') {
            hideBox(boxName);
        }
    };

}());
