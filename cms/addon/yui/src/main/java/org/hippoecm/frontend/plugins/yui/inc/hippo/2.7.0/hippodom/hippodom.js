/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @description
 * <p>
 * Resolves id's of the form element_id:yui_id to elements. Resolution consists
 * of finding the element with id element_id and then traversing the Dom tree to
 * find an element with attribute yui:id equal to yui_id. Traversal is limited
 * to elements with no (HTML) id
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom
 * @module hippodom
 * @beta
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.Dom) { // Ensure only one hippo dom exists
    ( function() {
        var Dom = YAHOO.util.Dom;

        YAHOO.hippo.Dom = function() {
        };

        YAHOO.hippo.Dom.resolveElement = function(_id) {
            var pathEls = _id.split(':');
            if (pathEls.length > 0) {
                var baseId = pathEls[0];
                var element = YAHOO.util.Dom.get(baseId);
                if (element != null && pathEls.length > 1) {
                    var yuiId = pathEls[1];
                    var children = [];
                    var traverse = function(node) {
                        try {
                            var value = node.getAttribute("yui:id", 2);
                            if (value && value == yuiId) {
                                children[children.length] = node;
                                return;
                            }
                            if (node.hasChildNodes()) {
                                var childNodes = Dom.getChildrenBy(node,
                                        YAHOO.hippo.Dom.isValidChildNode);
                                for (var i = 0; i < childNodes.length; i++) {
                                    traverse(childNodes[i]);
                                }
                            }
                        }catch(e) {
                            //ignore
                        }
                    };
                    traverse(element);
                    if (children.length > 0) {
                        return children[0];
                    }
                } else {
                    return element;
                }
            }

            return null;
        };

        YAHOO.hippo.Dom.enhance = function(el, id) {
            if(el == null) {
                return;
            }
            var yid = el.getAttribute("yui:id");
            if (yid && yid == id.split(':')[1] && el.id != id) {
                el.id = id;
                // workaround: css3 selectors allow a [yui|id=...] syntax
                Dom.addClass(el, yid);
            }
        };

        YAHOO.hippo.Dom.isValidChildNode = function(node) {
            if (node.nodeType == 1
                    && (!node.getAttribute("id") || node.getAttribute("yui:id"))) {
                return true;
            }
            return false;
        };

    })();

    YAHOO.register("hippodom", YAHOO.hippo.Dom, {
        version: "2.7.0", build: "1799"
    });
}

/*
 * Proposed patch for 'Empty string passed to getElementById()' error in FF3
 * with Firebug 1.2.0b2.
 */

//TODO: not needed anymore?
/*
YAHOO.util.Dom.get = function(el) {
    if (el) {
        if (el.nodeType || el.item) { // Node, or NodeList
            return el;
        }

        if (typeof el === 'string') { // id
            return document.getElementById(el);
        }

        if ('length' in el) { // array-like
            var c = [];
            for ( var i = 0, len = el.length; i < len; ++i) {
                c[c.length] = YAHOO.util.Dom.get(el[i]);
            }

            return c;
        }

        return el; // some other object, just pass it back
    }

    return null;
};
*/