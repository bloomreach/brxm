if (typeof Hippo === 'undefined') {
    Hippo = {};
}
if (typeof Hippo.Hst === 'undefined') {
    Hippo.Hst = {};
}

Hippo.Hst.AsyncPage = {

    load : function() {
        var result,divs , i, length;
        result = [];

        if (document.getElementsByClassName) {
            result = document.getElementsByClassName('_async');
        } else {
            divs = document.getElementsByTagName('div');
            for (i=0, length=divs.length; i<length; i++) {
                if (divs[i].className === '_async') {
                    result.push(divs[i]);
                }
            }
        }

        for (i=0, length=result.length; i< length; i++) {
            (function(element) {
                this.sendRequest(element.id, function(xmlHttp) {
                    var fragment, tmpDiv, parent;
                    fragment = document.createDocumentFragment();
                    tmpDiv = document.createElement('tmpDiv');
                    tmpDiv.innerHTML = xmlHttp.responseText;
                    while (tmpDiv.firstChild) {
                        fragment.appendChild(tmpDiv.firstChild);
                    }
                    parent = element.parentNode;
                    parent.replaceChild(fragment, element);
                });
            }).call(this, result[i]);
        }
    },

    sendRequest : function(url, callback) {
        var self, xmlHttpRequest;
        try {
            xmlHttpRequest = window.XMLHttpRequest ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP");
            xmlHttpRequest.open("GET", url, true);
            xmlHttpRequest.setRequestHeader('User-Agent', 'XMLHTTP/1.0');
            self = this;
            xmlHttpRequest.onreadystatechange = function () {
                if (xmlHttpRequest.readyState !== 4) {
                    return;
                }
                if (xmlHttpRequest.status !== 200 && xmlHttpRequest.status !== 304) {
                    return;
                }
                callback.call(self, xmlHttpRequest);
            };

            xmlHttpRequest.send();
        } catch (e) {
            if (typeof window.console !== 'undefined') {
                if (typeof console.error !== 'undefined') {
                    console.error(e.name + ": " + e.message);
                } else if (typeof console.log !== 'undefined') {
                    console.log(e.name + ": " + e.message);
                }
            }
        }
    }
};