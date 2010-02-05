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
                if(el == null) {
                    return;
                }
                el['treeHelper'] = {
                        cfg: cfg,
                        layoutUnit: null
                }
            },
            
            register : function(id) {
                var el = Dom.get(id);
                if(el == null) {
                    return;
                }
                if(el.treeHelper.cfg.bindToLayoutUnit && Lang.isUndefined(el.treeHelper.onRender)) {
                    var me = this;
                    YAHOO.hippo.LayoutManager.registerResizeListener(el, me, function() {
                        me.render(id);
                    });
                    el.treeHelper.layoutUnit = YAHOO.hippo.LayoutManager.findLayoutUnit(el);
                    el.treeHelper.onRender = {set: true};
                }
            },
            
            updateMouseListeners : function(id) {
                var el = Dom.get(id);
                if(el == null) {
                    return;
                }
                if(el.treeHelper.cfg.registerContextMenu) {
                    var items = Dom.getElementsByClassName('a_', 'div', id);
                    for(var i=0; i<items.length; i++) {
                        this.updateMouseListener(items[i].parentNode);
                    }
                }
            },
            
            updateMouseListener : function(el) {
                if(Lang.isUndefined(el.registeredContextMenu)) {
                    //TODO: make methods configurable
                    YAHOO.util.Event.on(el, 'mouseover', function(eventType, myId){ Hippo.ContextMenu.showContextLink(myId);}, el.id);
                    YAHOO.util.Event.on(el, 'mouseout',  function(eventType, myId){ Hippo.ContextMenu.hideContextLink(myId);}, el.id);
                    el.registeredContextMenu = {set: true};
                }
            },
            
            render : function(id) {
                this.register(id);
                var width = 0;
                var el = Dom.get(id);
                if(el == null) {
                    return;
                }
                
                if(el.treeHelper.cfg.treeAutowidth) {
                    var computedWidth = 0;
                    var items = Dom.getElementsByClassName('a_', 'div', id);
                    
                    var isWin = (/windows|win32/).test(navigator.userAgent.toLowerCase());
                    for(var i=0; i<items.length; i++) {
                        var item = items[i];
                        var itemChildNodes = Dom.getChildren(item);
                        for(var j=0; j<itemChildNodes.length; j++ ) {
                            var childNode = itemChildNodes[j];
                            var reg = Dom.getRegion(childNode);
                            computedWidth += reg.width;
                        }
                        //Add margin since the above calculation isn't pixel-perfect.
                        //Windows browsers need 10 pixels more
                        computedWidth += 39; //somehow YUI seems to miss the correct width of the text labels icon
                        if(isWin) {
                            //computedWidth += 5;
                        }
                        if(computedWidth > width) {
                            width = computedWidth;
                        }
                        computedWidth = 0;
                    }
                }
                if(width > 0) {
                    //try to set width to child element with classname 'hippo-tree'. We can't directly take 
                    //the childnode of 'id' because of the wicket:panel elements in dev-mode
                    var ar = Dom.getElementsByClassName(el.treeHelper.cfg.setWidthToClassname, 'div', id);
                    if(!Lang.isUndefined(ar.length) && ar.length > 0) {
                        //Also check if the maxFound width in the tree isn't smaller than the layoutMax width
                        var layoutMax = this.getLayoutMax(el);
                        if(layoutMax != null && layoutMax > width) {
                            width = layoutMax;
                        }

                        var regionTree = Dom.getRegion(ar[0]);
                        var regionUnitCenter = Dom.getRegion(Dom.getAncestorByClassName(ar[0], 'hippo-accordion-unit-center'));
                        if(regionTree.height > regionUnitCenter.height) {
                            //there is vertical scrolling, remove pixels from width to remove horizontal scrollbar
                            width -= (isWin ? 17: 15);
                        }

                        Dom.setStyle(ar[0], 'width', width + 'px');
                    }
                }
            },
            
            update: function(id) {
                this.render(id);
            },
            
            getLayoutMax : function(el) {
                if(el.treeHelper.cfg.bindToLayoutUnit && el.treeHelper.layoutUnit != null) {
                    return el.treeHelper.layoutUnit.getSizes().body.w;
                } else if(el.treeHelper.cfg.useWidthFromClassname != null) {
                    var e = Dom.getAncestorByClassName(el, el.treeHelper.cfg.useWidthFromClassname);
                    return parseInt(Dom.getStyle(e, 'width'));
                }
                return null;
            }
        };

    })();

    YAHOO.hippo.TreeHelper = new YAHOO.hippo.TreeHelperImpl();
    YAHOO.register("TreeHelper", YAHOO.hippo.TreeHelper, {
        version: "2.7.0", build: "1799"            
    });
}
