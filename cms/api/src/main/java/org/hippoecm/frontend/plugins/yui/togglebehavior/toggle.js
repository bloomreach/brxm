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

// TODO refactor. These functions should be set up like for example is done in the scroll.js
function toggleBox(boxName) {
    var box = document.getElementById('toggle-box-' + boxName);
    if (box == null) {
        return;
    }
    if (box.style.display == 'none') {
        showBox(boxName);
        updateToggleBoxCookie(boxName, true);
    } else if (box.style.display == 'block') {
        hideBox(boxName);
        updateToggleBoxCookie(boxName, false);
    }
}
function updateToggleBoxCookie(boxName, boxOpen) {
    var closedBoxes = getClosedBoxes();
    var boxNameIndex = closedBoxes.indexOf(boxName);
    if (boxOpen) {
        // remove boxName from array
        if (boxNameIndex != -1) {
            closedBoxes.splice(boxNameIndex, 1);
        }
    } else {
        // add boxName to array
        if (boxNameIndex == -1) {
            closedBoxes.push(boxName);
        }
    }
    setClosedBoxes(closedBoxes);
}
function getClosedBoxes() {
    var closedBoxesString = getCookie("ConsoleClosedBoxes");
    if (closedBoxesString == null) {
        closedBoxesString = '';
    }
    return closedBoxesString.split(":");
}
function setClosedBoxes(closedBoxes) {
    var closedBoxesString = "";
    var concat = "";
    for (var i = 0; i < closedBoxes.length; i++) {
        if (closedBoxes[i] !== '') {
            closedBoxesString += concat + closedBoxes[i];
            concat = ":";
        }
    }
    setCookie("ConsoleClosedBoxes", closedBoxesString, 30);
}
function showBox(boxName) {
    updateBox(boxName, 'block', 'group-collapsed', 'group-expanded');
}
function hideBox(boxName) {
    updateBox(boxName, 'none', 'group-expanded', 'group-collapsed');
}
function updateBox(boxName, display, oldGroup, newGroup) {
    if (boxName === '') {
        return;
    }
    console.log('updating box \'' + boxName + '\': display=' + display + ',oldGroup=' + oldGroup + ',newGroup=' + newGroup);
    var box = document.getElementById('toggle-box-' + boxName);
    if (box != null) {
        box.style.display = display;
    }
    var toggled = document.getElementById('toggle-' + boxName);
    if (toggled != null) {
        toggled.src = toggled.src.replace(oldGroup, newGroup);
    }
}
function setCookie(c_name, value, exdays) {
    var exdate = new Date();
    exdate.setDate(exdate.getDate() + exdays);
    var c_value = encodeURI(value) + ((exdays == null) ? "" : "; expires=" + exdate.toUTCString());
    document.cookie = c_name + "=" + c_value;
}
function getCookie(c_name) {
    var i, x, y, ARRcookies = document.cookie.split(";");
    for (i = 0; i < ARRcookies.length; i++) {
        x = ARRcookies[i].substr(0, ARRcookies[i].indexOf("="));
        y = ARRcookies[i].substr(ARRcookies[i].indexOf("=") + 1);
        x = x.replace(/^\s+|\s+$/g, "");
        if (x == c_name) {
            return decodeURI(y);
        }
    }
}
function boxesInit() {
    var closedBoxes = getClosedBoxes();
    for (var i = 0; i < closedBoxes.length; i++) {
        hideBox(closedBoxes[i]);
    }
}
