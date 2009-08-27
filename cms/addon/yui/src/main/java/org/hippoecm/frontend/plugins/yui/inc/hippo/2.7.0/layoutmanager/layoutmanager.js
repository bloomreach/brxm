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
 * Provides a singleton manager for dynamically updating the layout on Wicket
 * page loads and ajax events.
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom, event, animation, dragdrop, selector, layout, resize, cookie, functionqueue, hippodom, json, hashmap
 * @module layoutmanager
 * @beta
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.LayoutManager) { // Ensure only one layout manager exists
    ( function() {
        var Dom = YAHOO.util.Dom, Lang = YAHOO.lang, HippoDom = YAHOO.hippo.Dom,  
            Event = YAHOO.util.Event;
        
        YAHOO.hippo.LayoutManagerImpl = function() {
            this.init();
        };
        
        YAHOO.hippo.LayoutManagerImpl.prototype = {
            root            : null,
            wireframes      : new YAHOO.hippo.HashMap(),
            _w              : new YAHOO.hippo.HashMap(),
            renderQueue     : new YAHOO.hippo.FunctionQueue('render'),
            throttle        : new Wicket.Throttler(true),
            throttleDelay   : 2000,
        
            init : function() {
                //Handle the resizing of the window
                var me = this;
                Event.on(window, 'resize', me.resize, me, true);
            },
            
            resize : function() {
                if(this.root != null) {// && YAHOO.env.ua.ie) {
                    try {
                        this.root.resize(); //IE is not auto-bound to the body, so bind window-resize
                    } catch(e) {
                        YAHOO.log('Error resizing root', 'error');
                    }
                }
            },
            
            render : function() {
                try {
                    this.cleanupWireframes();
                } finally{
                    this.renderWireframes();
                }
            },
            
            renderWireframes : function() {
                this.renderQueue.handleQueue();
                this._w.forEach(this, function(k, v) {
                    var o = new v.clazz(k, v.config, this);
                    this.wireframes.put(k, o);
                    o.render();
                })
                this._w.clear();
                
                //Workaround for HREPTWO-3035
                //Set width of all resize handlers in IE to 20px, else they will be blocked
                //by the scrollbars
                if (YAHOO.env.ua.ie > 0) {
                    var re = Dom.getElementsByClassName('yui-resize-handle', 'div');
                    for(var i=0; i<re.length; i++) {
                        var handle = re[i]; 
                        Dom.setStyle(handle, 'width', '20px');
                    }
                }
            },
            
            addRoot : function(id, clazz, config) {
                this.root = new clazz(id, config, this);
                this.addToRenderQueue(this.root);
            },
            
            addWireframe : function(id, clazz, config) {
                //TODO: cleanup wireframes will be replaced
                var o = {
                        id: id,
                        clazz: clazz,
                        config: config
                };
                this._w.put(id, o);
            },
            
            addToRenderQueue : function(wireframe) {
                var func = function() {
                    wireframe.render();
                }
                this.renderQueue.registerFunction(func);
            },
            
            getWireframe : function(id) {
               if(id == this.root.id)
                   return this.root;
               return this.wireframes.get(id);
            },
            
            cleanupWireframes : function() {
                var remove = [];
                this.wireframes.forEach(this, function(key, value){
                    var bodyEl = HippoDom.resolveElement(key);
                    if (bodyEl == null) {
                        remove.push(key);
                    }
                });
                for(var i=0; i<remove.length; i++) {
                    if(this.wireframes.containsKey(remove[i])) {
                        this.cleanup(remove[i]);
                    }
                }
            },
            
            cleanup : function(id) {
                var wireframe = this.wireframes.get(id);
                wireframe.children.forEach(this, function(k, v) {
                    this.cleanup(k);
                });
                wireframe.children.clear();
                if (wireframe.layout != null) {
                    wireframe.layout.destroy();
                    this.wireframes.remove(id);
                }
            },
            
            registerResizeListener : function(el, obj, func, executeNow, calculateSizes) {
                var layoutUnitEl = this.findLayoutUnit(el);
                if (layoutUnitEl) {
                    var layoutUnit = YAHOO.widget.LayoutUnit
                            .getLayoutUnitById(layoutUnitEl.id);
                    if (Lang.isUndefined(layoutUnit.customEvent)
                            || Lang.isNull(layoutUnit.customEvent)) {
                        layoutUnit.customEvent = new YAHOO.util.CustomEvent(
                                "resizeEvent" + layoutUnitEl, this);
                    }
                    layoutUnit.customEvent.subscribe(func, obj);
                    var func = function() {
                        var sizes = null;
                        if(calculateSizes) {
                            sizes = layoutUnit.getSizes();
                            var scrollBottom = layoutUnit.body.scrollHeight - (layoutUnit.body.scrollTop + layoutUnit.body.clientHeight); // height of element scroll
                            var scroll = layoutUnit.body.scrollTop + scrollBottom > 0;
                            sizes['scroll'] = scroll;
                        }
                        layoutUnit.customEvent.fire(sizes);
                    };
                    layoutUnit.on('resize', func);
                    if(executeNow) {
                        func();
                    }
                }
            },

            findLayoutUnit : function(el) {
                while (el != null && el != document.body && el.id != this.root.id) {
                    if (Dom.hasClass(el, 'yui-layout-unit')) {
                        return el;
                    }
                    el = el.parentNode;
                }
                return false;
            }
        };
        
        YAHOO.hippo.BaseWireframe = function(id, config) {
            this.id = id;
            this.config = config;
            this.layout = null;
            this.children = new YAHOO.hippo.HashMap();
            
            this.name = id.indexOf(':') > -1 ? id.substr(id.indexOf(':') + 1) : id;
        };
        
        YAHOO.hippo.BaseWireframe.prototype = {
            render: function() {
                if(this.layout == null) {

                    this.enhanceIds();
                    this.prepareConfig();
                
                    var layout = new YAHOO.widget.Layout(this.id, this.config);
                    this.layout = layout;
                    this.initLayout();
                    
                    try {
                        this.layout.render();
                    } catch(e) {
                        YAHOO.log('An error occured during render of wireframe[' + this.id +']', 'error');
                        YAHOO.log(Lang.dump(this), 'error');
                    }
                }
                this.afterRender();
            },
            
            initLayout : function() {
                var me = this;
                this.layout.on('beforeResize', function() {
                    me.updateDimensions();
                    Dom.setStyle(me.id, 'height', me.config.height + 'px');
                    Dom.setStyle(me.id, 'width',  me.config.width + 'px');
                });
                
                this.layout.on('resize', function() {
                    me.storeDimensions();
                });

            },

            resize : function() {
                if(this.layout != null) {
                    this.layout.resize();
                    this.storeDimensions();
                }
            },

            registerChild  : function(c) {
                this.children.put(c.id, c);  
            },
              
            removeChild : function(c) {
              this.children.remove(c.id);  
            },
            
            prepareConfig : function() {
            },
            
            updateDimensions : function() {
            },
            
            hasPosition : function(pos) {
                if (pos == 'doc') {
                    return true;
                }
                for(var i=0; i<this.config.units.length; i++) {
                    if(this.config.units[i].position == pos) {
                        return true;
                    }
                }
                return false;
            },
            
            loadStoredDimensions : function() {
                var docDim = this.readDimension('doc');
                if(docDim == null || docDim.w == null || docDim.h == null) {
                    return false;
                }
                
                this.config.width = docDim.w;
                this.config.height = docDim.h;
                
                for(var i=0; i<this.config.units.length; i++) {
                    var unit = this.config.units[i];
                    var dim = this.readDimension(unit.position);
                    if(dim) {
                        this.config.units[i].width = dim.w;
                        this.config.units[i].height = dim.h;
                    }
                }
                return true;
            },
            
            storeDimensions : function() {
                YAHOO.log('Store sizes for: ' + this.name);
                var sizes = this.layout.getSizes();
                this.storeDimension(sizes, 'doc');
                this.storeDimension(sizes, 'top');
                this.storeDimension(sizes, 'right');
                this.storeDimension(sizes, 'bottom');
                this.storeDimension(sizes, 'left');
                this.storeDimension(sizes, 'center');
            },
            
            storeDimension : function(sizes, pos) {
                if(this.hasPosition(pos)) {
                    YAHOO.log('Sizes changed for ' + this.name);
                    var date = new Date();
                    date.setDate(date.getDate() + 31);
                    var opts = { expires: date };
                    YAHOO.util.Cookie.setSub('hippocms-layout-sizes', this.name + ':' + pos, sizes[pos].w + ',' + sizes[pos].h, opts);
                }
            },
            
            readDimension : function(pos) {
                var val = YAHOO.util.Cookie.getSub('hippocms-layout-sizes', this.name + ':' + pos);
                if(val == null) {
                    return false;
                }
                var ar = val.split(',');
                return {w:ar[0], h:ar[1]};
            },
            
            afterRender : function() {
                if (Dom.hasClass(this.id, 'smooth_load_workaround')) {
                    Dom.removeClass(this.id, 'smooth_load_workaround');
                }
                
                for (var i = 0; i < this.config.units.length; i++) {
                    var uCfg = this.config.units[i];
                    var un = this.layout.getUnitByPosition(uCfg.position);
                    if(un) {
                        if(uCfg.zindex > 0) {
                            un.setStyle('zIndex', uCfg.zindex);
                        }
                    }
                }
            },
            
            enhanceIds : function() {
                YAHOO.hippo.Dom.enhance(HippoDom.resolveElement(this.id), this.id);
                for ( var i = 0; i < this.config.units.length; i++) {
                    var u = this.config.units[i];
                    var uid = u["id"];
                    var unitEl = HippoDom.resolveElement(uid);
                    HippoDom.enhance(unitEl, uid);
                    this.config.units[i].yId = unitEl.id;
                    
                    uid = u["body"];
                    if (uid != undefined) {
                        unitEl = HippoDom.resolveElement(uid);
                        HippoDom.enhance(unitEl, uid);
                        this.config.units[i].bId = unitEl.id;
                    }
                }
            }

        }; 

        YAHOO.hippo.GridsRootWireframe = function(id, config) {
            YAHOO.hippo.GridsRootWireframe.superclass.constructor.apply(this, arguments); 
            
            var units = [];
            var body = { position: 'center', body: 'bd', grids: true, scroll: false};
            if(config.bodyGutter != null) {
                body.gutter = config.bodyGutter;
            }
            units.push(body);
            
            if(Lang.isNumber(config.headerHeight) && config.headerHeight > 0) {
                var header = {position: 'top', body: 'hd', height: config.headerHeight, scroll: false, grids: true};
                if(config.headerGutter != null) {
                  header.gutter = config.headerGutter;
                }
                units.push(header);
            }
            
            if(Lang.isNumber(config.leftWidth) && config.leftWidth > 0) {
                var left = {position: 'left', body: 'lt', width: config.leftWidth, scroll: false};
                units.push(left);
            }
            
            if(Lang.isNumber(config.rightWidth) && config.rightWidth > 0) {
                var right = {position: 'right', body: 'rt', width: config.rightWidth, scroll: false};
                units.push(right);
            }

            if(Lang.isNumber(config.footerHeight) && config.footerHeight > 0) {
                var footer = {position: 'bottom', body: 'ft', height: config.footerHeight, scroll: false, grids: true};
                if(config.footerGutter != null) {
                  footer.gutter = config.footerGutter;
                }
                units.push(footer);
            }

            this.config.units = units;
            this.margins = null;
        };

        YAHOO.extend(YAHOO.hippo.GridsRootWireframe, YAHOO.hippo.BaseWireframe, {
            render: function() {
                if(this.layout == null) {
                    this.prepareConfig();
                    
                    //for now we don't use the body element as wireframe root in IE
                    /*
                    var layout = YAHOO.env.ua.ie ? 
                        new YAHOO.widget.Layout(this.id, this.config) :
                        new YAHOO.widget.Layout(this.config);
                    */
                    
                    this.layout = new YAHOO.widget.Layout(this.config)
                    this.initLayout();
                }
                this.layout.render();
                
                //hack!
                Dom.setStyle('doc3', 'display', 'none');
                
            },
            
            enhanceIds : function(){
                //dont enhance root ids, yet
            },
            
            prepareConfig : function() {
                this.margins  = new YAHOO.hippo.DomHelper().getMargin(Dom.get(this.id));
                this.config.width = Dom.get(this.id).offsetWidth - this.margins.w; //Width of the outer element
                this.config.height = Dom.getClientHeight() - this.margins.h;
            },

            updateDimensions : function() {
                this.config.height = Dom.getClientHeight()-this.margins.h;
                this.config.width = Dom.getClientWidth()-this.margins.w; //this works better than offsetWidth
            }
        });
        
        YAHOO.hippo.Wireframe = function(id, config) {
            YAHOO.hippo.Wireframe.superclass.constructor.apply(this, arguments);
            this.margins = null;
            this.domHelper = new YAHOO.hippo.DomHelper();
            this.parent = null;
        };

        YAHOO.extend(YAHOO.hippo.Wireframe, YAHOO.hippo.BaseWireframe, {
            
            prepareConfig : function() {
                if(this.config.linkedWithParent) {
                    this.parent = YAHOO.hippo.LayoutManager.getWireframe(this.config.parentId);
                    this.parent.registerChild(this);
                    this.config.parent = this.parent.layout;
                }
                this.initDimensions();
            },
            
            initDimensions : function() {
                if(!this.loadStoredDimensions()) {
                    this.updateDimensions();
                }
            },
            
            updateDimensions : function() {
                var dim = this.getDimensions();
                this.config.height = dim.h;
                this.config.width = dim.w;
            },
            
            getDimensions : function() {
                var dim = {};
                if(this.config.linkedWithParent && this.parent != null) {
                    
                    var margin = {w:0, h:0};
                    var parent = Dom.get(this.id).parentNode;
                    while(parent != null) {
                        var parentMargin = this.domHelper.getMargin(parent);
                        margin.w += parentMargin.w;
                        margin.h += parentMargin.h;
                        
                        if(Dom.hasClass(parent, 'yui-layout-unit')) {
                            //unit found, wrap it up
                            dim.w = this.domHelper.getWidth(parent);
                            dim.h = this.domHelper.getHeight(parent);
                            dim.w -= margin.w;
                            dim.h -= margin.h;
                            
                            if(this.config.parent != null) {
                                var u = this.config.parent.getUnitById(parent.id);
                                if(u != null) {
                                    dim.w -= (u._gutter.left + u._gutter.right);
                                    dim.h -= (u._gutter.top + u._gutter.bottom);
                                }
                            }
                            
                            return dim;
                        }
                        parent = parent.parentNode;
                    }
                }
                //fallback
                var parent = Dom.get(this.id).parentNode;
                dim.w = this.domHelper.getWidth(parent);
                dim.h = this.domHelper.getHeight(parent);
                return dim;
            }
            
        });
        
        YAHOO.hippo.RelativeWireframe = function(id, config) {
            YAHOO.hippo.RelativeWireframe.superclass.constructor.apply(this, arguments);
            
            this.relativeUnits = {};
            for(var i=0; i<config.units.length; i++) {
                var unit = config.units[i];
                var r = {};
                var w = unit.width;
                if(Lang.isUndefined(w)) {
                    r.w = 1;
                }else if(Lang.isString(w) && w.indexOf('%') > -1) {
                    r.w = parseInt(w.substr(0, w.indexOf('%')))/100;
                }
                var h = unit.height;
                if(Lang.isUndefined(h)) {
                    r.h = 1;
                }else if(Lang.isString(h) && h.indexOf('%') > -1) {
                    r.h = parseInt(h.substr(0, h.indexOf('%')))/100;
                }
                this.relativeUnits[unit.position] = r;
            }
        };

        YAHOO.extend(YAHOO.hippo.RelativeWireframe, YAHOO.hippo.Wireframe, {
            
            initDimensions : function() {
                if (!this.loadStoredDimensions()) {
                    var dim = this.getDimensions();
                    
                    for(var i=0; i<this.config.units.length; i++) {
                        var unit = this.config.units[i];
                        if(unit.position != 'center') {
                            var p = this.relativeUnits[unit.position].w;
                            if(!Lang.isUndefined(p) && Lang.isNumber(p)) {
                                //set rel width
                                this.config.units[i].width = parseInt(dim.w*p);
                            }
                            p = this.relativeUnits[unit.position].h;
                            if(!Lang.isUndefined(p) && Lang.isNumber(p)) {
                                //set rel height
                                this.config.units[i].height = parseInt(dim.h*p);
                            }
                        }
                    }
                    
                    this.config.height = dim.h;
                    this.config.width = dim.w;
                }
            },
            
            updateDimensions : function() {
                //recalc dims
                var dim = this.getDimensions();
                this.config.height = dim.h;
                this.config.width = dim.w;
                //this.storeDimensions();
            }
        });
        
        //remove
        YAHOO.hippo.TabsWireframe = function(id, config) {
            YAHOO.hippo.TabsWireframe.superclass.constructor.apply(this, arguments);
        };

        YAHOO.extend(YAHOO.hippo.TabsWireframe, YAHOO.hippo.Wireframe, {
            afterRender : function() {
                var id = this.id.substring(0, this.id.indexOf(':'));
                var x = Dom.get(id + ':tabbed-panel-layout-center');
                //Dom.setStyle(x, 'margin-left', '0px');
                //Dom.setStyle(x, 'display', 'block');
                YAHOO.hippo.TabsWireframe.superclass.prepareConfig.call(this);
            }
        });
        
        YAHOO.hippo.DomHelper = function() {
        };
        
        YAHOO.hippo.DomHelper.prototype = {
            getMargin : function(element) {
                var margins = {w:0, h:0};
                margins.w += this.getBorderWidth(element);
                margins.w += this.getMarginWidth(element);
                margins.w += this.getPaddingWidth(element);

                margins.h += this.getBorderHeight(element);
                margins.h += this.getMarginHeight(element);
                margins.h += this.getPaddingHeight(element);
                return margins;
            },
            
            getWidth : function(el) {
                return this.asInt(el, 'width');
            },
            
            getHeight : function(el) {
                return this.asInt(el, 'height');
            },

            getBorderWidth: function(el) {
                var x = this.asInt(el, 'border-left-width');
                x += this.asInt(el, 'border-right-width');
                return x;
            },

            getBorderHeight: function(el) {
                var y = this.asInt(el, 'border-top-width');
                y += this.asInt(el, 'border-bottom-width');
                return y;
            },

            getMarginWidth: function(el) {
                var x = this.asInt(el, 'margin-left');
                x += this.asInt(el, 'margin-right');
                return x;
            },

            getMarginHeight: function(el) {
                var y = this.asInt(el, 'margin-top');
                y += this.asInt(el, 'margin-bottom');
                return y;
            },

            getPaddingWidth: function(el) {
                var x = this.asInt(el, 'padding-left');
                x += this.asInt(el, 'padding-right');
                return x;
            },

            getPaddingHeight: function(el) {
                var y = this.asInt(el, 'padding-top');
                y += this.asInt(el, 'padding-bottom');
                return y;
            },

            asInt : function(el, style) {
                var x = Dom.getStyle(el, style);
                if(Lang.isString(x) && x.length>2) {
                    x = x.substr(0, x.indexOf('px'));
                    //FF3 on Ubuntu thinks the border is something like 0.81236666 so we round it
                    return Math.round(x);
                }
                return 0;
            }

        };
    })();

    YAHOO.hippo.LayoutManager = new YAHOO.hippo.LayoutManagerImpl();
    YAHOO.register("layoutmanager", YAHOO.hippo.LayoutManager, {
        version: "2.7.0", build: "1799"
    });
}

//PATCH

( function() {
    var Dom = YAHOO.util.Dom, Event = YAHOO.util.Event, Lang = YAHOO.lang;
    
    YAHOO.widget.Layout.prototype.destroy = function() {
        var par = this.get('parent');
        if (par) {
            par.removeListener('resize', this.resize, this, true);
        }
        Event.removeListener(window, 'resize', this.resize, this, true);
        this.unsubscribeAll();
        for (var u in this._units) {
            if (Lang.hasOwnProperty(this._units, u)) {
                if (this._units[u]) {
                    this._units[u].destroy(true);
                }
            }
        }
    
        Event.purgeElement(this.get('element'));
        //this.get('parentNode').removeChild(this.get('element'));
    
        delete YAHOO.widget.Layout._instances[this.get('id')];
        //Brutal Object Destroy
        for (var i in this) {
            if (Lang.hasOwnProperty(this, i)) {
                this[i] = null;
                delete this[i];
            }
        }
    
        if (par) {
            //par.resize();
        }
    }

    YAHOO.widget.LayoutUnit.prototype.destroy = function(force) {
        if (this._resize) {
            this._resize.destroy();
        }
        var par = this.get('parent');
    
        this.setStyle('display', 'none');
        if (this._clip) {
            this._clip.parentNode.removeChild(this._clip);
            this._clip = null;
        }
    
        if (!force) {
            par.removeUnit(this);
        }
        
        if (par) {
            par.removeListener('resize', this.resize, this, true);
        }
        
        this.unsubscribeAll();
        Event.purgeElement(this.get('element'));
        //this.get('parentNode').removeChild(this.get('element'));
    
        delete YAHOO.widget.LayoutUnit._instances[this.get('id')];
        //Brutal Object Destroy
        for (var i in this) {
            if (Lang.hasOwnProperty(this, i)) {
                this[i] = null;
                delete this[i];
            }
        }
        return par;
    }


})();