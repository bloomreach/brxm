/*
 * Copyright 2010-2015 Hippo B.V. (http://www.onehippo.com)
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
 * @class a simple helper class to calculate tree width
 * @namespace YAHOO.hippo
 * @requires yahoo, dom, event, layoutmanager, hippowidget
 * @module treewidget
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.TreeWidget) {
    (function() {
        var Dom = YAHOO.util.Dom, Lang = YAHOO.lang;

        YAHOO.hippo.TreeWidget = function(id, cfg) {
            YAHOO.hippo.TreeWidget.superclass.constructor.call(this, id, cfg);

            this.timeoutID = null;
            this.timeoutLength = 200;
        };

        YAHOO.extend(YAHOO.hippo.TreeWidget, YAHOO.hippo.Widget, {

            calculateWidthAndHeight: function(sizes) {
                var width, computedWidth, computedHeight, items, i, iLen, item, itemChildNodes, j, jLen, childNode, reg, ref;

                this._setWidth(2000); //make sure initial calculation isn't blocked by a broken UI

                width = 0;
                if (this.config.treeAutoWidth) {
                    computedWidth = 0;
                    computedHeight = 0;
                    items = Dom.getElementsByClassName('a_', 'div', this.id);

                    //Calculate width&height of items and save largest computedWidth value in var 'width'
                    for (i = 0, iLen = items.length; i < iLen; i++) {
                        item = items[i];
                        itemChildNodes = Dom.getChildren(item);
                        for (j = 0, jLen = itemChildNodes.length; j < jLen; j++) {
                            childNode = itemChildNodes[j];
                            reg = Dom.getRegion(childNode);
                            computedWidth += reg.width;
                            if (j === 0) {
                                computedHeight += reg.height;
                            }
                        }
                        if (computedWidth > width) {
                            width = computedWidth;
                        }
                        computedWidth = 0;
                    }

                    ref = Dom.getRegion(this.el.parentNode);
                    if (computedHeight > ref.height) {
                        //tree content overflows container element, browser will render scrollbars, so change width
                        width += YAHOO.hippo.HippoAjax.getScrollbarWidth();
                    }

                    //Add magic width
                    if (this.config.workflowEnabled) {
                        width += 25;
                    } else {
                        width += 15;
                    }
                }
                return {
                    width  : width,
                    height : -1
                };
            },

            _setHeight: function() {
            },

            _setWidth : function(width) {
                var ar, layoutMax, regionTree, regionUnitCenter, isWin;

                //try to set width to child element with classname 'hippo-tree'. We can't directly take
                //the childnode of 'id' because of the wicket:panel elements in dev-mode
                ar = Dom.getElementsByClassName(this.config.setWidthToClassname, 'div', this.id);
                if (!Lang.isUndefined(ar.length) && ar.length > 0) {
                    //Also check if the maxFound width in the tree isn't smaller than the layoutMax width
                    layoutMax = this.getLayoutMax();
                    if (layoutMax !== null && layoutMax > width) {
                        width = layoutMax;
                    }

                    regionTree = Dom.getRegion(ar[0]);
                    regionUnitCenter = Dom.getRegion(Dom.getAncestorByClassName(ar[0], 'section-center'));
                    if (regionTree.height > regionUnitCenter.height) {
                        //there is vertical scrolling, remove pixels from width to remove horizontal scrollbar
                        isWin = (/windows|win32/).test(navigator.userAgent.toLowerCase());
                        width -= (isWin ? 17 : 15);
                    }

                    Dom.setStyle(ar[0], 'width', width + 'px');
                }
            },

            getLayoutMax : function() {
                var layoutMax, e;

                layoutMax = null;

                if (this.config.bindToLayoutUnit && this.unit !== null) {
                    layoutMax = this.unit.getSizes().body.w;
                } else if (this.config.useWidthFromClassname !== null) {
                    e = Dom.getAncestorByClassName(this.el, this.config.useWidthFromClassname);
                    layoutMax = parseInt(Dom.getStyle(e, 'width'), 10);
                }
                return layoutMax;
            },

            updateMouseListeners : function(id) {
                var el, items, i, len;

                el = Dom.get(id);
                if (el === null || el === undefined) {
                    return;
                }
                if (this.config.workflowEnabled) {
                    items = Dom.getElementsByClassName('a_', 'div', id);
                    for (i = 0, len = items.length; i < len; i++) {
                        this.updateMouseListener(items[i].parentNode);
                    }
                }
            },

            updateMouseListener : function(el) {
                if (Lang.isUndefined(el.registeredContextMenu)) {
                    //TODO: make methods configurable
                    YAHOO.util.Event.on(el, 'mouseover', function(eventType, myId) {
                        Hippo.ContextMenu.showContextLink(myId);
                    }, el.id);
                    YAHOO.util.Event.on(el, 'mouseout', function(eventType, myId) {
                        Hippo.ContextMenu.hideContextLink(myId);
                    }, el.id);
                    el.registeredContextMenu = {set: true};
                }
            }

        });

    }());

    YAHOO.register("TreeWidget", YAHOO.hippo.TreeWidget, {
        version: "2.8.1", build: "19"
    });
}
