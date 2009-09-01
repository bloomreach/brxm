/*
 * Copyright 2009 Hippo
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
 * Provides a singleton manager for accordion panels
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom, event, container, hashmap
 * @module accordionmanager
 * @beta
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.AccordionManager) {
    (function() {
        var Dom = YAHOO.util.Dom, Lang = YAHOO.lang;

        YAHOO.hippo.AccordionManagerImpl = function() {
        };

        YAHOO.hippo.AccordionManagerImpl.prototype = {
            configurations : new YAHOO.hippo.HashMap(),
            
            create : function(id, config) {
                if(!this.configurations.containsKey(id)) {
                    this.configurations.put(id, config);
                }
            },

            renderUnit : function(id, unitId) {
                var el = Dom.get(id);
                if(el != null) {
                    if (Lang.isUndefined(el.accordion)) {
                        el.accordion = new YAHOO.hippo.Accordion(id, unitId, this.configurations.get(id));
                    } else {
                        el.accordion.update();
                    }
                } else {
                    YAHOO.log("Failed to render accordion, element[" + id + "] not found", "error");
                }
            }

        };

        YAHOO.hippo.Accordion = function(id, unitId, config) {
            this.config(id, unitId, config);
            this.init();
        };

        YAHOO.hippo.Accordion.prototype = {
            initialized: false,
            calculated: false,
            timeoutID: null,

            id: null,
            cfg: {
                timeoutLength: 200,
                ancestorClassName: 'yui-layout-bd',
                unitClassName: 'hippo-accordion-unit',
                unitHeaderHeight: 25,
                calculateTotalHeight: false,
                setHeightToClassname: null,
                throttleUpdate: true 
            },
            current: null,
            region: null,
            totalHeight: 0,
    
            config: function(id, currentUnitId, config) {
                this.id = id;
                this.current = currentUnitId;
                //TODO: handle configuration
            },
    
            init: function() {
                if(this.initialized) {
                    return;
                }
                this.initialized = true; //set because registration below does a direct callback
    
                var me = this;
                YAHOO.hippo.LayoutManager.registerResizeListener(Dom.get(this.id), me, function() {
                    me.calculated = false;
                    me.update();
                });
                YAHOO.hippo.LayoutManager.registerRenderListener(Dom.get(this.id), me, function() {
                    me.update(true);
                }, true);
            },

            calculate : function() {
                if(this.calculated) {
                    return;
                }
                
                var parent = Dom.getAncestorByClassName(this.id, this.cfg.ancestorClassName);
                if(parent != null) {
                    this.region = Dom.getRegion(parent);
                } else {
                    YAHOO.log('Could not find parent element, error calculating available height', 'error');
                    return;
                }
    
                var children = Dom.getElementsByClassName(this.cfg.unitClassName, 'div', this.id);
                if(this.cfg.calculateTotalHeight) {
                    //set display of tree to none to calculate height of section headers
                    if(this.current != null) {
                        Dom.setStyle(this.current, 'display', 'none');
                    }

                    var calculatedHeight = 0;
                    for(var i=0; i<children.length; i++) {
                        var r = Dom.getRegion(children[i]);
                        calculatedHeight += (r.bottom - r.top);
                    }
                    this.totalHeight = calculatedHeight;
                } else {
                    this.totalHeight = children.length * this.cfg.unitHeaderHeight;
                }
            },
    
            render : function(id) {
                this.init();
                this.calculate();

                var height = (this.region.bottom - this.region.top) - this.totalHeight;
                
                var centerEl = this.findElement(id, this.cfg.unitClassName + '-center');
                var bottomEl = this.findElement(id, this.cfg.unitClassName + '-bottom');
                
                if(bottomEl != null && this.findElement(bottomEl, this.cfg.unitClassName + '-add', 'span') != null) {
                    height -= 26; //temp workaround
                }

                if(height > 0) {
                    Dom.setStyle(centerEl, 'height', height + 'px');
                }
                
                Dom.setStyle(bottomEl, 'display', 'block');
                
                this.current = id;
            },
    
            update: function(bOverride) {
                if(this.current == null) {
                    return;
                }
                
                this.calculated = false;
                if(!this.cfg.throttleUpdate || bOverride) {
                    this.render(this.current);
                } else {
                    if(this.timeoutID != null) {
                        window.clearTimeout(this.timeoutID);
                    }
                    var me = this;
                    this.timeoutID = window.setTimeout(function() {
                        me.render(me.current);
                    }, this.cfg.timeoutLength);
                }
            },
            
            findElement : function(parent, cls, tag) {
                if(Lang.isUndefined(tag)) {
                    tag = 'div';
                }
                var ar = Dom.getElementsByClassName(cls, tag, parent);
                if(!Lang.isUndefined(ar.length) && ar.length > 0) {
                    return ar[0];
                }
                return null;
            }
        };

    })();

    YAHOO.hippo.AccordionManager = new YAHOO.hippo.AccordionManagerImpl();
    YAHOO.register("AccordionManager", YAHOO.hippo.AccordionManager, {
        version: "2.7.0", build: "1799"            
    });
}