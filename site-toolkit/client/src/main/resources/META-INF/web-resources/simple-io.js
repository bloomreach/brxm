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

function _asyncSimpleio_createxmlhttpobject() {
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

function _asyncLoad() {
    var result = document.getElementsByClassName("_async");
    for (var i=0, length=result.length; i< length; i++) {
        (function(element) {
            _asyncSimpleio_sendrequest(element.id, function(req) {
                var parent = element.parentNode;
                var newElement = document.createElement('div');
                newElement.innerHTML = req.response;
                parent.insertBefore(newElement, element);

            });
        })(result[i]);
    }

}

function _asyncSimpleio_sendrequest(url, callback) {
	var req = _asyncSimpleio_createxmlhttpobject();
    if (!req) return;
    var method = "GET";
    req.open(method, url, true);
    req.setRequestHeader('User-Agent', 'XMLHTTP/1.0');
    req.onreadystatechange = function () {
        if (req.readyState != 4) return;
        if (req.status != 200 && req.status != 304) {
            return;
        }
        callback(req);
    }
    req.send();
}


