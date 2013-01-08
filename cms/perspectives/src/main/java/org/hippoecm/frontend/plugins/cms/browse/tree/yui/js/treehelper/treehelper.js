/*
 * Copyright 2008-2013 Hippo
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
 * @requires yahoo, dom, event, layoutmanager
 * @constructor
 * @param {String} id the id of the linked element
 * @param {String} config
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.TreeHelper) {
    (function() {
        var Dom = YAHOO.util.Dom, Lang = YAHOO.lang;

        YAHOO.hippo.TreeHelperImpl = function() {
        };

        YAHOO.hippo.TreeHelperImpl.prototype = {
            cfg : null,
            timeoutID: null,
            timeoutLength: 200,
                
            init : function(id, cfg){
                YAHOO.log('Register[' + id + '] cfg=' + Lang.dump(cfg), 'info', 'TreeHelper');
                var el = Dom.get(id);
                if (el === null || el === undefined) {
                    return;
                }
                el.treeHelper = {
                        cfg: cfg,
                        layoutUnit: null
                };
            },
            
            register : function(id) {
                var el, self;
                el = Dom.get(id);
                if (el === null || el === undefined) {
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
            
            updateMouseListeners : function(id) {
                var el, items, i, len;
                el = Dom.get(id);
                if (el === null || el === undefined) {
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
                    YAHOO.util.Event.on(el, 'mouseover', function(eventType, myId){ Hippo.ContextMenu.showContextLink(myId);}, el.id);
                    YAHOO.util.Event.on(el, 'mouseout',  function(eventType, myId){ Hippo.ContextMenu.hideContextLink(myId);}, el.id);
                    el.registeredContextMenu = {set: true};
                }
            },
            
            render : function(id) {
                var width, el, computedWidth, computedHeight, items, i, iLen, item, itemChildNodes, j, jLen,
                    childNode, reg, ref;

                this.register(id);
                width = 0;
                el = Dom.get(id);
                if (el === null || el === undefined) {
                    return;
                }

                this._setWidth(el, id, 2000);

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
            },

            _setWidth : function(el, id, width) {
                var ar, layoutMax, regionTree, regionUnitCenter, isWin;

                //try to set width to child element with classname 'hippo-tree'. We can't directly take
                //the childnode of 'id' because of the wicket:panel elements in dev-mode
                ar = Dom.getElementsByClassName(el.treeHelper.cfg.setWidthToClassname, 'div', id);
                if (!Lang.isUndefined(ar.length) && ar.length > 0) {
                    //Also check if the maxFound width in the tree isn't smaller than the layoutMax width
                    layoutMax = this.getLayoutMax(el);
                    if (layoutMax !== null && layoutMax !== undefined && layoutMax > width) {
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
