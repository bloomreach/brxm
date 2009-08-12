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
                    if(typeof(el.accordion) == 'undefined') {
                        YAHOO.log("Creating accordion instance[" + id + "]");
                        el.accordion = new YAHOO.hippo.Accordion(id, this.configurations.get(id));
                    }
                    YAHOO.log('Render unit[' + unitId + '] on parent[' + id + ']');
                    el.accordion.render(unitId);
                } else {
                    YAHOO.log("Failed to render accordion, element[" + id + "] not found", "error");
                }
            }

        };

        YAHOO.hippo.Accordion = function(id, config) {
            this.config(id, config);
        };

        YAHOO.hippo.Accordion.prototype = {
            initialized: false,
            calculated: false,
            timeoutID: null,

            id: null,
            cfg: {
                timeoutLength: 300,
                ancestorClassName: 'yui-layout-bd',
                unitClassName: 'hippo-accordion-unit',
                unitHeaderHeight: 25,
                calculateTotalHeight: false,
                throttleUpdate: true, 
                addScrollbar: true
            },
            current: null,
            region: null,
            totalHeight: 0,
    
            config: function(id, config) {
                this.id = id;
                //TODO: handle configuration
            },
    
            init: function() {
                if(this.initialized) {
                    return;
                }
    
                var me = this;
                YAHOO.hippo.LayoutManager.registerResizeListener(Dom.get(this.id), me, function() {
                    me.update();
                });
                this.initialized = true;
            },
    
            calculate : function() {
                if(this.calculated) {
                    return;
                }
                
                var parent = Dom.getAncestorByClassName(this.id, this.cfg.ancestorClassName);
                if(parent != null) {
                    this.region = Dom.getRegion(parent);
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
                Dom.setStyle(id, 'height', height + 'px');
                Dom.setStyle(id, 'display', 'block');
                if (this.cfg.addScrollbar) {
                    Dom.setStyle(id, 'overflow', 'scroll');
                    if(YAHOO.env.ua.ie > 0 && YAHOO.env.ua.ie < 8) {
                        Dom.setStyle(id, '-ms-overflow-x', 'hidden');
                        Dom.setStyle(id, '-ms-overflow-y', 'auto');
                    } else {
                        Dom.setStyle(id, 'overflow-x', 'auto');
                        Dom.setStyle(id, 'overflow-y', 'auto');
                    }
                }
                this.current = id;
            },
    
            update: function() {
                if(this.current == null) {
                    return;
                }
    
                if(!this.cfg.throttleUpdate) {
                    this.calculated = false;
                    this.render(this.current);
                } else {
                    if(this.timeoutID != null) {
                        window.clearTimeout(this.timeoutID);
                    }
                    var me = this;
                    this.timeoutID = window.setTimeout(function() {
                        this.calculated = false;
                        me.render(me.current);
                    }, this.cfg.timeoutLength);
                }
            }
        };

    })();

    YAHOO.hippo.AccordionManager = new YAHOO.hippo.AccordionManagerImpl();
    YAHOO.register("AccordionManager", YAHOO.hippo.AccordionManager, {
        version : "2.6.0",
        build : "1321"
    });
}