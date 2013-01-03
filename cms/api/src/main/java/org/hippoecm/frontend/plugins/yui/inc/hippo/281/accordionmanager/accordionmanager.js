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
            
            register : function(id, config) {
                if(!this.configurations.containsKey(id)) {
                    this.configurations.put(id, config);
                }
            },

            render : function(id, unitId) {
                var el = Dom.get(id);
                if (el !== null && el !== undefined) {
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
            cfg: null,
            current: null,
            region: null,
            totalHeight: 0,
    
            config: function(id, currentUnitId, config) {
                this.id = id;
                this.current = currentUnitId;
                this.cfg = config;
            },
    
            init: function() {
                var me, root;

                if(this.initialized) {
                    return;
                }
                this.initialized = true; //set here because render registration below does a direct callback

                me = this;
                root = Dom.get(this.id);
                if (this.cfg.registerResizeListener) {
                    YAHOO.hippo.LayoutManager.registerResizeListener(root, me, function() {
                        me.calculated = false;
                        me.update();
                    });
                }
                if (this.cfg.registerRenderListener) {
                    YAHOO.hippo.LayoutManager.registerRenderListener(root, me, function() {
                        me.update(true);
                    }, true);
                } else {
                    this.update(true);
                }                
                YAHOO.hippo.HippoAjax.registerDestroyFunction(root, this.cleanup, this);
            },
            
            cleanup : function() {
                //TODO: implement cleanup with new layoutmanager
            },

            calculate : function() {
                var parent, children, i, len;

                if(this.calculated) {
                    return;
                }
                
                parent = Dom.getAncestorByClassName(this.id, this.cfg.ancestorClassname);
                if (parent !== null && parent !== undefined) {
                    this.region = Dom.getRegion(parent);
                } else {
                    YAHOO.log('Could not find parent element, error calculating available height', 'error');
                    return;
                }
    
                children = Dom.getElementsByClassName(this.cfg.unitClassname, 'div', this.id);
                if(this.cfg.calculateTotalHeight) {
                    //set display of tree to none to calculate height of section headers
                    if(this.current !== null) {
                        Dom.setStyle(this.current, 'display', 'none');
                    }
                    this.totalHeight = 0;
                    for (i = 0, len = children.length; i < len; i++) {
                        this.totalHeight += Dom.getRegion(children[i]).height;
                    }
                } else {
                    this.totalHeight = children.length * this.cfg.unitHeaderHeight;
                }
            },
    
            render : function(id) {
                var height, bottom, top;

                this.init();
                this.calculate();
                
                height = this.region.height - this.totalHeight;

                bottom = this.findElement(id, this.cfg.unitClassname + '-bottom');
                if (bottom !== null && Dom.getStyle(bottom, 'display') !== 'none') {
                    height -= Dom.getRegion(bottom).height;
                }

                top = this.findElement(id, this.cfg.unitClassname + '-top');
                if (top !== null && Dom.getStyle(top, 'display') !== 'none') {
                    height -= Dom.getRegion(top).height;
                }

                if (height > 0) {
                    Dom.setStyle(this.findElement(id, this.cfg.unitClassname + '-center'), 'height', height + 'px');
                }
                this.current = id;
            },
    
            update: function(bOverride) {
                if (this.current === null || this.current === undefined) {
                    return;
                }
                
                this.calculated = false;
                this.render(this.current);
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

    }());

    YAHOO.hippo.AccordionManager = new YAHOO.hippo.AccordionManagerImpl();
    YAHOO.register("AccordionManager", YAHOO.hippo.AccordionManager, {
        version: "2.8.1", build: "19"            
    });
}
