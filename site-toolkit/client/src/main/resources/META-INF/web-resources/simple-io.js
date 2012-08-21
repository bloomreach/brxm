/*
 *  Copyright 2009 Hippo.
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
var simpleio_xmlhttpfactories = [
    function () { return new XMLHttpRequest() },
    function () { return new ActiveXObject("Msxml2.XMLHTTP") },
    function () { return new ActiveXObject("Msxml3.XMLHTTP") },
    function () { return new ActiveXObject("Microsoft.XMLHTTP") }
];

function simpleio_createxmlhttpobject() {
    var xmlhttp = false;
    for (var i = 0; i < simpleio_xmlhttpfactories.length; i++) {
        try {
            xmlhttp = simpleio_xmlhttpfactories[i]();
        } catch (e) {
            continue;
        }
        break;
    }
    return xmlhttp;
}

function simpleio_sendrequest(url, callback, postData) {
	var req = simpleio_createxmlhttpobject();
    if (!req) return;
    var method = (postData ? "POST" : "GET");
    req.open(method, url, true);
    req.setRequestHeader('User-Agent', 'XMLHTTP/1.0');
    if (postData)
        req.setRequestHeader('Content-type','application/x-www-form-urlencoded');
    req.onreadystatechange = function () {
        if (req.readyState != 4) return;
        if (req.status != 200 && req.status != 304) {
            return;
        }
        callback(req);
    }
    if (req.readyState == 4) return;
    req.send(postData);
}

function simpleio_objectbyid(id) {
    if (document.getElementById)
        return document.getElementById(id);
    else if (document.all)
        return document.all[id];
    else if (document.layers)
        return document.layers[id];
    return null;
}

