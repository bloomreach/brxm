if (typeof Hippo === 'undefined') {
    Hippo = {};
}
if (typeof Hippo.Hst === 'undefined') {
    Hippo.Hst = {};
}

Hippo.Hst.AsyncPage = {

    load : function() {
        var result, divs, i, length;
        result = [];

        if (document.getElementsByClassName) {
            result = document.getElementsByClassName('_async');
        } else {
            divs = document.getElementsByTagName('div');
            for (i = 0, length = divs.length; i < length; i++) {
                if (divs[i].className === '_async') {
                    result.push(divs[i]);
                }
            }
        }

        // Find all script elements, extract them from the fragment and store
        // them into a new array. Cloning the nodes does not work as the script
        // will not be executed that way, so we have to clone them manually.
        function extractScriptNodes(fragment) {
            var i, j, length, node, nodes, script, scripts, atts;

            atts = ['async', 'charset', 'defer', 'src', 'type'];
            scripts = fragment.querySelectorAll('script');
            nodes = [];

            for (i = 0, length = scripts.length; i < length; i++) {
                script = scripts[i];
                node = document.createElement('script');
                if (window.addEventListener) {
                    node.appendChild(document.createTextNode(script.innerHTML));
                } else { // IE8 or less
                    node.text = script.innerHTML;
                }
                for (j = 0; j < atts.length; j++) {
                    if (script.hasAttribute(atts[j])) {
                        node.setAttribute(atts[j], script.getAttribute(atts[j]));
                    }
                }
                script.parentNode.removeChild(script);
                nodes.push(node);
            }

            return nodes;
        }

        // If next is undefined, simply append the nodes to the parent,
        // otherwise insert the nodes before the reference node
        function insertScriptNodes(nodes, parent, reference) {
            var i, length;
            for (i = 0, length = nodes.length; i < length; i++) {
                if (reference) {
                    parent.insertBefore(nodes[i], reference);
                } else {
                    parent.appendChild(nodes[i]);
                }
            }
        }

        for (i = 0, length = result.length; i < length; i++) {
            (function(element) {
                this.sendRequest(element.id, function(xmlHttp) {
                    var fragment, tmpDiv, parent, scriptNodes, next;

                    // Convert the responseText into HTML nodes
                    tmpDiv = document.createElement('div');
                    if (window.addEventListener) {
                        tmpDiv.innerHTML = xmlHttp.responseText;
                    } else { // IE8 or less
                        // If firstChild is a script/style element IE will
                        // ignore it, so we add a textNode first, then 
                        // immediately remove it again and IE will show the 
                        // script/style elements 
                        tmpDiv.innerHTML = '<span>&#160;</span>' + xmlHttp.responseText;
                        tmpDiv.removeChild(tmpDiv.firstChild);
                    }

                    // Move the nodes into a fragment
                    fragment = document.createDocumentFragment();
                    while (tmpDiv.firstChild) {
                        fragment.appendChild(tmpDiv.firstChild);
                    }

                    // Extract the script nodes so we can re-insert and execute
                    // them after the DOM has been updated with the new HTML
                    scriptNodes = extractScriptNodes(fragment);

                    // Save a reference to the parent and the next sibling
                    parent = element.parentNode;
                    next = element.nextSibling;

                    // Update the DOM
                    parent.replaceChild(fragment, element);

                    // Insert the script nodes
                    insertScriptNodes(scriptNodes, parent, next);
                });
            }).call(this, result[i]);
        }
    },

    sendRequest : function(url, callback) {
        var xmlHttpRequest;
        try {
            xmlHttpRequest = window.XMLHttpRequest ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP");
            xmlHttpRequest.open("GET", url, true);
            xmlHttpRequest.onreadystatechange = function () {
                if (xmlHttpRequest.readyState !== 4) {
                    return;
                }
                if (xmlHttpRequest.status !== 200 && xmlHttpRequest.status !== 304) {
                    return;
                }
                callback(xmlHttpRequest);
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
