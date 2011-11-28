/*
 *  Copyright 2008 Hippo.
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
function toggleBox(num) {
    var box = document.getElementById('toggle-box-' + num);
    if (box==null) return;
    if(box.style.display == 'none') {
        showBox(num);
        setCookie("ConsoleToggleBox"+num, 1,30);
        return;
    }
    if(box.style.display == 'block') {
        hideBox(num);
        setCookie("ConsoleToggleBox"+num, 0,30);
        return;
    }
}
function showBox(num) {
//    console.log('show box ' + num);
    document.getElementById('toggle-box-' + num).style.display = 'block';
    var src = document.getElementById('toggle-' + num).src;
    document.getElementById('toggle-' + num).src = src.replace('group-collapsed', 'group-expanded');
}
function hideBox(num) {
//    console.log('hide box ' + num);
    document.getElementById('toggle-box-' + num).style.display = 'none';
    var src = document.getElementById('toggle-' + num).src;
    document.getElementById('toggle-' + num).src = src.replace('group-expanded', 'group-collapsed');
}
function setCookie(c_name,value,exdays) {
    var exdate = new Date();
    exdate.setDate(exdate.getDate() + exdays);
    var c_value = encodeURI(value) + ((exdays==null) ? "" : "; expires="+exdate.toUTCString());
    document.cookie=c_name + "=" + c_value;
}
function getCookie(c_name) {
var i,x,y,ARRcookies=document.cookie.split(";");
for (i=0;i<ARRcookies.length;i++) {
  x=ARRcookies[i].substr(0,ARRcookies[i].indexOf("="));
  y=ARRcookies[i].substr(ARRcookies[i].indexOf("=")+1);
  x=x.replace(/^\s+|\s+$/g,"");
  if (x==c_name) {
    return decodeURI(y);
    }
  }
}
function boxesInit() {
//    console.log('boxes init');
    for (var num=1; num <= 4; num++) {
        var openClose = getCookie("ConsoleToggleBox" + num);
        var box = document.getElementById('toggle-box-' + num);
        if(box==null) continue;
        if(openClose != null) {
            if(openClose == 1 && box.style.display == 'none') {
                showBox(num);
            }
            if(openClose == 0 && box.style.display == 'block') {
                hideBox(num);
            }
        }
    }
}