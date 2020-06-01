/*
 * Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
        var Dom = YAHOO.util.Dom, Lang = YAHOO.lang, HippoDom;

        HippoDom = function() {
        };

        HippoDom.resolveElement = function(_id) {
            var pathEls, baseId, element, yuiId, children, traverse;
            pathEls = _id.split(':');
            if (pathEls.length > 0) {
                baseId = pathEls[0];
                element = Dom.get(baseId);
                if (element !== null && element !== undefined && pathEls.length > 1) {
                    yuiId = pathEls[1];
                    children = [];
                    traverse = function(node) {
                        var value, childNodes, i, len;
                        try {
                            value = node.getAttribute("yui:id", 2);

                            if (value && value === yuiId) {
                                children[children.length] = node;
                                return;
                            }
                            if (node.hasChildNodes()) {
                                childNodes = Dom.getChildrenBy(node, Dom.isValidChildNode);
                                for (i = 0, len = childNodes.length; i < len; i++) {
                                    traverse(childNodes[i]);
                                }
                            }
                        } catch(e) {
                            console.warn("Error while searching node with yui:id '" + yuiId + "': ", e);
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

        HippoDom.enhance = function(el, id) {
            if(el === null || el === undefined) {
                return;
            }
            var yid = el.getAttribute("yui:id");
            if (yid && yid === id.split(':')[1] && el.id !== id) {
                el.id = id;
                // workaround: css3 selectors allow a [yui|id=...] syntax
                Dom.addClass(el, yid);
            }
        };

        HippoDom.isValidChildNode = function(node) {
            if (node.nodeType === 1 && (node.prefix === null || node.prefix === undefined || node.prefix === 'html') &&
                    (!node.getAttribute("id") || node.getAttribute("yui:id"))) {
                return true;
            }
            return false;
        };

        // return visible width/height including padding, border and scrollbar, excluding margin
        HippoDom.getDim = function(element) {
            return {
                w: element.offsetWidth,
                h: element.offsetHeight
            };
        };

        HippoDom.getOuterDim = function(element) {
            return {
                w: element.offsetWidth + HippoDom.getMarginWidth(element),
                h: element.offsetHeight + HippoDom.getMarginHeight(element)
            };
        };

        HippoDom.getWidthAndHeight = function(element) {
            return {
                w: this.getWidth(element),
                h: this.getHeight(element)
            };
        };

        HippoDom.getMargin = function(element) {
            var margins = {w:0, h:0};
            margins.w += this.getBorderWidth(element);
            margins.w += this.getMarginWidth(element);
            margins.w += this.getPaddingWidth(element);

            margins.h += this.getBorderHeight(element);
            margins.h += this.getMarginHeight(element);
            margins.h += this.getPaddingHeight(element);
            return margins;
        };

        HippoDom.getWidth = function(el) {
            return this.asInt(el, 'width');
        };

        HippoDom.getHeight = function(el) {
            return this.asInt(el, 'height');
        };

        HippoDom.getBorderWidth= function(el) {
            var x = this.asInt(el, 'border-left-width');
            x += this.asInt(el, 'border-right-width');
            return x;
        };

        HippoDom.getBorderHeight= function(el) {
            var y = this.asInt(el, 'border-top-width');
            y += this.asInt(el, 'border-bottom-width');
            return y;
        };

        HippoDom.getMarginWidth= function(el) {
            var x = this.asInt(el, 'margin-left');
            x += this.asInt(el, 'margin-right');
            return x;
        };
        HippoDom.getMarginHeight= function(el) {
            var y = this.asInt(el, 'margin-top');
            y += this.asInt(el, 'margin-bottom');
            return y;
        };

        HippoDom.getPaddingWidth= function(el) {
            var x = this.asInt(el, 'padding-left');
            x += this.asInt(el, 'padding-right');
            return x;
        };

        HippoDom.getPaddingHeight= function(el) {
            var y = this.asInt(el, 'padding-top');
            y += this.asInt(el, 'padding-bottom');
            return y;
        };

        HippoDom.getLeft = function(el) {
            return this.asInt(el, 'left');
        };

        HippoDom.getTop = function(el) {
            return this.asInt(el, 'top');
        };

        HippoDom.asInt = function(el, style) {
            var x = Dom.getStyle(el, style);
            if(Lang.isString(x) && x.length>2) {
                x = x.substr(0, x.indexOf('px'));
                //FF3 on Ubuntu thinks the border is something like 0.81236666 so we round it
                return Math.round(x);
            }
            return 0;
        };

        YAHOO.hippo.Dom = HippoDom;

    }());

    YAHOO.register("hippodom", YAHOO.hippo.Dom, {
        version: "2.8.1", build: "19"
    });
}
