/*
 *  Copyright 2012 Hippo.
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
if (typeof Hippo === 'undefined') {
    Hippo = {};
}
if (typeof Hippo.Hst == 'undefined') {
    Hippo.Hst = {};
}
Hippo.Hst.AsyncPage = {

    xmlHttpFactories : [
        function () { return new XMLHttpRequest() },
        function () { return new ActiveXObject("Msxml2.XMLHTTP") },
        function () { return new ActiveXObject("Msxml3.XMLHTTP") },
        function () { return new ActiveXObject("Microsoft.XMLHTTP") }
    ],


    load : function() {
        var result = document.getElementsByClassName("_async");
        for (var i=0, length=result.length; i< length; i++) {
            (function(element) {
                this.sendRequest(element.id, function(xmlHttp) {

                    var fragment = document.createDocumentFragment();
                    var tmpDiv = document.createElement('tmpDiv');
                    tmpDiv.innerHTML = xmlHttp.response;
                    while (tmpDiv.firstChild) {
                        fragment.appendChild(tmpDiv.firstChild);
                        tmpDiv.removeChild(tmpDiv.firstChild);
                    }

                    element.parentNode.replaceChild(fragment, element);
                });
            }).call(this, result[i]);
        }

    },

    sendRequest : function(url, callback) {
        var xmlHttp = this.createXmlHttpObject();
        if (!xmlHttp) {
            return;
        }
        xmlHttp.open("GET", url, true);
        xmlHttp.setRequestHeader('User-Agent', 'XMLHTTP/1.0');
        var self = this;
        xmlHttp.onreadystatechange = function () {
            if (xmlHttp.readyState !== 4) {
                return;
            }
            if (xmlHttp.status !== 200 && xmlHttp.status !== 304) {
                return;
            }
            callback.call(self, xmlHttp);
        };
        xmlHttp.send();
    },

    createXmlHttpObject : function() {
        var xmlHttp = false;
        for (var i = 0; i < this.xmlHttpFactories.length; i++) {
            try {
                xmlHttp = this.xmlHttpFactories[i]();
            } catch (e) {
                continue;
            }
            break;
        }
        return xmlHttp;
    }
};