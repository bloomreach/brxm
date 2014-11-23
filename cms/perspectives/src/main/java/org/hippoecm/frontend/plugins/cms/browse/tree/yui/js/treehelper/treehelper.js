/*
 * Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
 * @requires yahoo, dom, event, layoutmanager, hippodom
 * @constructor
 * @param {String} id the id of the linked element
 * @param {String} config
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.TreeHelper) {
    (function() {
        "use strict";

        var Dom = YAHOO.util.Dom, 
            Lang = YAHOO.lang;

        function byClass(name, tag, root) {
            var found = Dom.getElementsByClassName(name, tag, root);
            if (!Lang.isUndefined(found.length) && found.length > 0) {
                return found[0];
            }
            return null;
        }

        function exists(o) {
            return !Lang.isUndefined(o) && !Lang.isNull(o);
        }
        
        function getWidth(el) {
            var l = Dom.getX(el), 
                r = l + el.offsetWidth;
            return r - l;
        }

        YAHOO.hippo.TreeHelperImpl = function() {
        };

        YAHOO.hippo.TreeHelperImpl.prototype = {
            cfg : null,
            timeoutID: null,
            timeoutLength: 200,
                
            init : function(id, cfg){
                YAHOO.log('Register[' + id + '] cfg=' + Lang.dump(cfg), 'info', 'TreeHelper');

                var el = Dom.get(id);
                if (exists(el)) {
                    el.treeHelper = {
                        cfg: cfg,
                        layoutUnit: null,
                        highlight: null
                    };
                    
                    // Notify the context menu the tree is scrolling so it can reposition the context menu
                    if (Dom.hasClass(el.parentNode, 'hippo-accordion-unit-center')) {
                        YAHOO.util.Event.on(el.parentNode, 'scroll', function(e) {
                            Hippo.ContextMenu.redraw();
                        });
                    }
                }
            },

            register : function(id) {
                var el, self;
                el = Dom.get(id);

                if (!exists(el)) {
                    return;
                }

                if (el.treeHelper.cfg.bindToLayoutUnit && Lang.isUndefined(el.treeHelper.onRender)) {
                    self = this;
                    YAHOO.hippo.LayoutManager.registerResizeListener(el, self, function() {
                        self.render(id);
                    });
                    el.treeHelper.layoutUnit = YAHOO.hippo.LayoutManager.findLayoutUnit(el);
                    el.treeHelper.onRender = {set: true};
                }
            },

            updateHighlight : function(id) {
                var tree = Dom.get(id),
                    helper, selected, selectedY, highlight;

                if (!(exists(tree) && exists(tree.treeHelper))) {
                    return;
                }

                helper = tree.treeHelper;
                highlight = helper.highlight;

                if (highlight === null) {
                    helper.highlight = highlight = this.createHighlight(tree);
                    if (highlight === null) {
                        return;
                    }
                }

                selected = byClass('row-selected', 'div', tree);
                if (selected === null) {
                    if (!Dom.hasClass(highlight, 'hide')) {
                        Dom.addClass(highlight, 'hide');
                    }
                } else {
                    if (Dom.hasClass(highlight, 'hide')) {
                        Dom.removeClass(highlight, 'hide');
                    }

                    selectedY = Dom.getY(selected);
                    if (selectedY !== Dom.getY(highlight)) {
                        // Move highlight widget to position of selected
                        Dom.setY(highlight, selectedY);
                    }
                    // trigger mouseout to redraw the context-icon
                    if (selected.registeredContextMenu) {
                        selected.registeredContextMenu.mouseOut();
                    }
                }
            },
            
            createHighlight: function(tree) {
                var hippoTree = byClass('hippo-tree', 'div', tree), 
                    el = null;

                if (hippoTree !== null) {
                    el = document.createElement("div");
                    hippoTree.appendChild(el);
                    Dom.addClass(el, 'hippo-tree-highlight-widget');
                }
                return el;
            },

            updateMouseListeners : function(id) {
                var el, items, i, len;

                el = Dom.get(id);
                if (!exists(el)) {
                    return;
                }

                if (el.treeHelper.cfg.workflowEnabled) {
                    items = Dom.getElementsByClassName('a_', 'div', id);
                    for (i = 0, len = items.length; i < len; i++) {
                        this.updateMouseListener(items[i].parentNode);
                    }
                }
            },

            updateMouseListener : function(el) {
                if (Lang.isUndefined(el.registeredContextMenu)) {
                    //TODO: make methods configurable
                    el.registeredContextMenu = {
                        mouseOver: function(eventType, myId) { Hippo.ContextMenu.showContextLink(myId); },
                        mouseOut: function(eventType, myId) { Hippo.ContextMenu.hideContextLink(myId); },
                        set: true
                    };

                    YAHOO.util.Event.on(el, 'mouseover', el.registeredContextMenu.mouseOver, el.id);
                    YAHOO.util.Event.on(el, 'mouseout', el.registeredContextMenu.mouseOut, el.id);
                }
            },

            render : function(id) {
                var width, el, computedWidth, computedHeight, items, i, iLen, item, itemChildNodes, 
                        j, jLen, childNode, reg, ref;

                this.register(id);
                width = 0;
                el = Dom.get(id);
                if (!exists(el)) {
                    return;
                }

                // Browser other than IE can use:
                //    width: intrinsic;           /* Safari/WebKit uses a non-standard name */
                //    width: -moz-max-content;    /* Firefox/Gecko */
                //    width: -webkit-max-content; /* Chrome */
                // which causes the child elements to define the width of the element, IE still needs help from javascript 
                if (YAHOO.env.ua.ie > 0) {
                    if (el.treeHelper.cfg.treeAutowidth) {
                        computedWidth = 0;
                        computedHeight = 0;
                        items = Dom.getElementsByClassName('a_', 'div', id);
    
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
    
                        ref = Dom.getRegion(el.parentNode);
                        if (computedHeight > ref.height) {
                            //tree content overflows container element, browser will render scrollbars, so change width
                            width += YAHOO.hippo.HippoAjax.getScrollbarWidth();
                        }
    
                        //Add magic width
                        if (el.treeHelper.cfg.workflowEnabled) {
                            width += 25;
                        } else {
                            width += 10;
                        }
                    }
                    
                    if (width > 0) {
                        this._setWidth(el, id, width);
                    }
                }
            },

            _setWidth : function(el, id, width) {
                var ar, layoutMax, regionTree, regionUnitCenter, isWin;

                //try to set width to child element with classname 'hippo-tree'. We can't directly take
                //the childnode of 'id' because of the wicket:panel elements in dev-mode
                ar = Dom.getElementsByClassName(el.treeHelper.cfg.setWidthToClassname, 'div', id);
                if (!Lang.isUndefined(ar.length) && ar.length > 0) {
                    //Also check if the maxFound width in the tree isn't smaller than the layoutMax width
                    layoutMax = this.getLayoutMax(el);
                    if (exists(layoutMax) && layoutMax > width) {
                        width = layoutMax;
                    }

                    regionTree = Dom.getRegion(ar[0]);
                    regionUnitCenter = Dom.getRegion(Dom.getAncestorByClassName(ar[0], 'hippo-accordion-unit-center'));
                    if (regionTree.height > regionUnitCenter.height) {
                        //there is vertical scrolling, remove pixels from width to remove horizontal scrollbar
                        isWin = (/windows|win32/).test(navigator.userAgent.toLowerCase());
                        width -= (isWin ? 17 : 15);
                    }

                    Dom.setStyle(ar[0], 'width', width + 'px');
                }
            },

            update: function(id) {
                this.render(id);
            },

            getLayoutMax : function(el) {
                var result, e;
                result = null;
                if (el.treeHelper.cfg.bindToLayoutUnit && el.treeHelper.layoutUnit !== null && el.treeHelper.layoutUnit !== undefined) {
                    result = el.treeHelper.layoutUnit.getSizes().body.w;
                } else if (el.treeHelper.cfg.useWidthFromClassname !== null && el.treeHelper.cfg.useWidthFromClassname !== undefined) {
                    e = Dom.getAncestorByClassName(el, el.treeHelper.cfg.useWidthFromClassname);
                    result = parseInt(Dom.getStyle(e, 'width'), 10);
                }
                return result;
            }
        };

    }());

    YAHOO.hippo.TreeHelper = new YAHOO.hippo.TreeHelperImpl();
    YAHOO.register("TreeHelper", YAHOO.hippo.TreeHelper, {
        version: "2.8.1", build: "19"
    });
}
