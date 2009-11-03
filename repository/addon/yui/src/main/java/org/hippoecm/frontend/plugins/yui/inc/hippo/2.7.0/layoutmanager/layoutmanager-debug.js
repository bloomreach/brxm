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
        
        YAHOO.widget.Layout.prototype._setupBodyElements =  function() {
            this._doc = Dom.get('layout-doc');
            if (!this._doc) {
                this._doc = document.createElement('div');
                this._doc.id = 'layout-doc';
                if (document.body.firstChild) {
                    document.body.insertBefore(this._doc, document.body.firstChild);
                } else {
                    document.body.appendChild(this._doc);
                }
            }
            this._createUnits();
            this._setBodySize();
            //Skip onwindow resize handling here
            //Event.on(window, 'resize', this.resize, this, true);
            Dom.addClass(this._doc, 'yui-layout-doc');
        };

        
        YAHOO.hippo.LayoutManagerImpl = function() {
            this.init();
        };
        
        YAHOO.hippo.LayoutManagerImpl.prototype = {
            root            : null,
            wireframes      : new YAHOO.hippo.HashMap(),
            _w              : new YAHOO.hippo.HashMap(),
            renderQueue     : new YAHOO.hippo.FunctionQueue('render'),
            throttler       : new Wicket.Throttler(true),
            throttleDelay   : 0,
            resizeEvent     : null,
        
            init : function() {
                //Register window resize event
                Event.on(window, 'resize', this.resize, this, true);
                this.throttleDelay = 0;
                if(YAHOO.env.ua.ie) {
                    this.throttleDelay = 400;
                } else if(YAHOO.env.ua.gecko) {
                    this.throttleDelay = 400;
                }
                
                this.resizeEvent = new YAHOO.util.CustomEvent('rootResizeEvent');
            },
            
            /** 
             * This will only optimize resize performance when
             * layouts do not register a window.resize themselves I guess
             */
            resize : function() {
                if(this.throttleDelay > 0) {
                    var me = this;
                    this.throttler.throttle('resize-root', this.throttleDelay, function() {
                        me._resize();
                    });
                } else {
                    this._resize();
                }
            },

            _resize : function() {
                if(this.root != null) {
                    try {
                        this.root.resize();
                        this.resizeEvent.fire();
                    } catch(e) {
                        YAHOO.log('Error resizing root: ' + e, 'error');
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

                if (YAHOO.env.ua.ie >= 6 && YAHOO.env.ua.ie < 8) {
                    var re = Dom.getElementsByClassName('yui-resize-handle', 'div');
                    for(var i=0; i<re.length; i++) {
                        Dom.setStyle(re[i], 'width', '10px');
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
                this.renderQueue.registerFunction(function() {
                    wireframe.render();
                });
            },
            
            getWireframe : function(id) {
               if(id == this.root.id) {
                   return this.root;
               }
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
                var wireframe = this.wireframes.remove(id);
                if(wireframe.parent != null) {
                    wireframe.parent.removeChild(wireframe);
                }
                wireframe.children.forEach(this, function(k, v) {
                    this.cleanup(k);
                });
                wireframe.children.clear();
                if (wireframe.layout != null) {
                    wireframe.layout.destroy();
                }
            },

            registerRootResizeListener : function(obj, func) {
                this.resizeEvent.subscribe(func, obj, true);
            },

            unregisterRootResizeListener : function(func) {
                this.resizeEvent.unsubscribe(func);                
            },

            registerResizeListener : function(el, obj, func, executeNow, _timeoutLength) {
                var layoutUnit = this.findLayoutUnit(el);
                if(layoutUnit == null) {
                    YAHOO.log('Unable to find ancestor layoutUnit for element[@id=' + el.id + ']', 'error', 'LayoutManager');
                    return;
                }
                var timeoutLength = this.throttleDelay;
                if(!Lang.isUndefined(_timeoutLength) && Lang.isNumber(_timeoutLength)) { //also check for false for backward compatibility
                    timeoutLength = _timeoutLength;
                }
                this.registerEventListener(layoutUnit, layoutUnit, 'resize', obj, func, executeNow, timeoutLength);
            },

            registerRenderListener : function(el, obj, func, executeNow) {
                var layoutUnit = this.findLayoutUnit(el);
                if(layoutUnit == null) {
                    YAHOO.log('Unable to find ancestor layoutUnit for element[@id=' + el.id + ', can not register render event', 'error', 'LayoutManager');
                    return;
                }
                this.registerEventListener(layoutUnit.get('parent'), layoutUnit, 'render', obj, func, executeNow);
            },

            registerEventListener : function(target, unit, evt, obj, func, executeNow, timeoutLength) {
                var oid = Lang.isUndefined(obj.id) ? Dom.generateId() : obj.id;
                
                var myId = '[' + evt + ', ' + oid + ']';
                if(executeNow) {
                    YAHOO.log('ExecuteNow' + myId, 'info', 'LayoutManager');
                    func.apply(obj, [unit.getSizes()]);
                }
                var useTimeout = !Lang.isUndefined(timeoutLength) && Lang.isNumber(timeoutLength) && timeoutLength > 0;
                var eventName = evt + 'CustomEvent';
                var info = { numberOfTimeouts:0, absStart: null, relStart: null };
                var me = this;
                
                if(Lang.isUndefined(target[eventName + 'Subscribers'])) {
                    target[eventName + 'Subscribers'] = new YAHOO.hippo.HashMap();

                    var callback = function() {
                        if(info.numberOfTimeouts == 0) {
                            YAHOO.log('Callback' + myId + ' called, timeout=' + useTimeout, 'info', 'LayoutManager');
                            info.absStart = new Date();
                        } else {
                            var prevTime = new Date().getTime() - info.relStart.getTime();
                            YAHOO.log('Callback' + myId + ' re-called after ' + prevTime + 'ms, timeouts=' + info.numberOfTimeouts, 'info', 'LayoutManager');
                        }
                        var me = this;
                        var execute = function() {
                            var abs = new Date().getTime() - info.absStart.getTime();
                            YAHOO.log('Execute' + myId + ' called after ' + abs + 'ms, timeouts=' + info.numberOfTimeouts + ', timeout=' + useTimeout, 'info', 'LayoutManager');
                            info = { numberOfTimeouts:0, absStart: null, relStart: null };
                            
                            var evtStart = new Date();
                            var values = target[eventName + 'Subscribers'].valueSet();
                            for(var i=0; i<values.length; i++) {
                                var f = values[i];
                                f.apply(obj, [unit.getSizes()]);
                            }

                            YAHOO.log('Execute' + myId + ' handling took ' + (new Date().getTime()-evtStart.getTime()) + 'ms', 'info', 'LayoutManager');
                        }
                        if(useTimeout) {
                            info.relStart = new Date();
                            YAHOO.log('Throttle' + myId + ', timeoutLength=' + timeoutLength, 'info', 'LayoutManager');
                            info.numberOfTimeouts++;

                            timeoutLength = 10;
                            this.throttler.throttle(eventName + oid, timeoutLength, execute);
                        } else {
                            execute();
                        }
                    };
                    YAHOO.log('Register' + myId + ' on unit ' + target.get('id') + ', timeout=' + useTimeout, 'info', 'LayoutManager');
                    target.subscribe(evt, callback, null, this);
                }
                
                obj['SubcribeId' + evt] = oid;
                target[eventName + 'Subscribers'].put(oid, func);
            },
            
            unregisterResizeListener : function (el, obj) {
                var layoutUnit = this.findLayoutUnit(el);
                if(layoutUnit == null) {
                    YAHOO.log('Unable to find ancestor layoutUnit for element[@id=' + el.id + ']', 'error', 'LayoutManager');
                    return false;
                }
                return this.unregisterEventListener('resize', layoutUnit, obj);
            },

            unregisterRenderListener : function (el, obj) {
                var layoutUnit = this.findLayoutUnit(el);
                if(layoutUnit == null) {
                    YAHOO.log('Unable to find ancestor layoutUnit for element[@id=' + el.id + ', can not unregister render event', 'error', 'LayoutManager');
                    return false;
                }
                return this.unregisterEventListener('render', layoutUnit.get('parent'), obj);
            },

            unregisterEventListener : function (evt, target, obj) {
                var oid = obj['SubcribeId' + evt];
                var set = target[evt + 'CustomEventSubscribers'];
                if(set != null && set.containsKey(oid)) {
                    set.remove(oid);
                    return true;
                }
                return false;
            },

            findLayoutUnit : function(el) {
                el = this._findUnitElement(el);
                if(el != null) {
                    return YAHOO.widget.LayoutUnit.getLayoutUnitById(el.id);
                }
                return null;
            },
            
            /**
             * Dom.getAncestorByClassName didn't work
             */
            _findUnitElement : function(el) {
                while (el != null && el != document.body && el.id != this.root.id) {
                    if (Dom.hasClass(el, 'yui-layout-unit')) {
                        return el;
                    }
                    el = el.parentNode;
                }
                return null;
            }
        };
        
        YAHOO.hippo.BaseWireframe = function(id, config) {
            this.id = id;
            this.config = config;
            this.layout = null;
            this.children = new YAHOO.hippo.HashMap();
            this.throttler = new Wicket.Throttler(true);
            
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
                        YAHOO.log('An error occured during render of wireframe[' + this.id +'], dump=' + Lang.dump(this), 'error', 'Wireframe');
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
                
                this.layout.on('resize', this.onLayoutResize, null, this);
            },
            
            onLayoutResize: function() {
                this.storeDimensions();
                
                var values = this.children.valueSet();
                for(var i=0; i<values.length; i++) {
                    values[i].resize();
                }
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
                    YAHOO.log('No stored dimensions found for: ' + this.name, 'info', 'Wireframe');
                    return false;
                }
                
                YAHOO.log('Stored dimensions found for: ' + this.name, 'info', 'Wireframe');
                this.config.width = docDim.w;
                this.config.height = docDim.h;
                var IEIE = 'doc[w=' + docDim.w + ',h=' + docDim.h + '], ';
                
                for(var i=0; i<this.config.units.length; i++) {
                    var unit = this.config.units[i];
                    var dim = this.readDimension(unit.position);
                    if(dim) {
                        this.config.units[i].width = dim.w;
                        this.config.units[i].height = dim.h;
                        IEIE += unit.position + '[w=' + dim.w + ',h=' + dim.h + '], ';
                    }
                }
                //alert(IEIE);
                return true;
            },
            
            storeDimensions : function() {
                YAHOO.log('Store sizes for: ' + this.name, 'info', 'Wireframe');
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
                    YAHOO.log('Sizes changed for ' + this.name, 'info', 'Wireframe');
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
                
                //By default, yui-layout units don;t dynamically keep a maxWidth/minWidth with respect to their neighbors
                //which means a user can render the UI useless. To prevent this we add a check right when the unit's 
                //resize event finishes 
                for(var i=0; i<this.config.units.length; i++) {
                    var x = this.layout.getUnitByPosition(this.config.units[i].position);
                    var me = this;
                    x.on('endResize', function() {
                        var newWidth = this.get('width');
                        var sizes = this.get('parent').getSizes();
                        
                        //if the width of this unit is bigger than the layout width, it will
                        //overlap neighboring units. A 20px margin is used.
                        if((sizes.doc.w-20) < newWidth) {
                            this.set('width', sizes.doc.w-20);
                        } else {
                            //else check if the new width isn't rendering nested layout's units invisible.
                            //if a number less than zero is returned, it resembles the offset of the least 
                            //visible unit. This offset + the new width will make the unit still invisible, so
                            //we add 20 pixels to it to define the new width
                            var diff = me.newWidthIsOk();
                            if(diff < 0) {
                                this.set('width', (newWidth-diff)+20);
                            }
                        }
                    }, x, true);
                }
            },
            
            newWidthIsOk : function() {
                var result = 0;
                for(var i=0; i<this.config.units.length; i++) {
                    var unit = this.layout.getUnitByPosition(this.config.units[i].position);
                    var unitWidth = unit.get('width');
                    if(unitWidth < result) {
                        result = unitWidth;
                    }
                }
                this.children.forEach(this, function(key, wireframe){
                    var diff = wireframe.newWidthIsOk(); 
                    if(diff < result) {
                        result = diff;
                    }
                });
                return result;
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
                        if(unitEl == null) {
                            throw new Error("Could not find element with uid " + uid);
                        }
                        HippoDom.enhance(unitEl, uid);
                        this.config.units[i].bId = unitEl.id;
                    }
                }
            }

        }; 

        YAHOO.hippo.GridsRootWireframe = function(id, config) {
            YAHOO.hippo.GridsRootWireframe.superclass.constructor.apply(this, arguments); 
            
            var units = [];
            var body = { position: 'center', body: 'bd', grids: true, scroll: config.bodyScroll};
            if(config.bodyGutter != null) {
                body.gutter = config.bodyGutter;
            }
            units.push(body);
            
            if(Lang.isNumber(config.headerHeight) && config.headerHeight > 0) {
                var header = {position: 'top', body: 'hd', height: config.headerHeight, scroll: config.headerScroll, resize: config.headerResize, grids: true};
                if(config.headerGutter != null) {
                  header.gutter = config.headerGutter;
                }
                units.push(header);
            }
            
            if(Lang.isNumber(config.leftWidth) && config.leftWidth > 0) {
                var left = {position: 'left', body: 'lt', width: config.leftWidth, scroll: config.leftScroll, resize: config.leftResize };
                if(config.leftGutter != null) {
                    left.gutter = config.leftGutter;
                }
                units.push(left);
            }
            
            if(Lang.isNumber(config.rightWidth) && config.rightWidth > 0) {
                var right = {position: 'right', body: 'rt', width: config.rightWidth, scroll: config.rightScroll, resize: config.rightResize };
                units.push(right);
            }

            if(Lang.isNumber(config.footerHeight) && config.footerHeight > 0) {
                var footer = {position: 'bottom', body: 'ft', height: config.footerHeight, scroll: config.footerScroll, resize: config.footerResize, grids: true};
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
                    this.layout = new YAHOO.widget.Layout(this.config)
                    this.initLayout();
                }
                this.layout.render();
                
                //workaround
                Dom.setStyle('doc3', 'display', 'none');
                
                //HREPTWO-3072 it seems the body with/height is set once by YUI-layout
                //after a resize it's not updated, so we set it to auto instead
                Dom.setStyle(document.body, 'width', 'auto');
                Dom.setStyle(document.body, 'height', 'auto');
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
				
                //HREPTWO-3064 To make older browsers more responsive during resizing
                //we throttle the resize event until it is finished. If we don't set the parent to null
                //YUI layout will connect the resize event of a parent wireframes to it's nested wireframes,
                //rendering the throttle approach useless
                this.config.parent = null;
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
                            
                            if(this.parent != null) {
                                var u = this.parent.layout.getUnitById(parent.id);
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
            
            this.relativeUnits = [];
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
                    this.config.height = dim.h;
                    this.config.width = dim.w;
                    
                    for(var i=0; i<this.config.units.length; i++) {
                        var pos = this.config.units[i].position;
                        if(pos != 'center') {
                            var w = this.relativeUnits[pos].w;
                            var h = this.relativeUnits[pos].h;

                            this.config.units[i].width = parseInt(dim.w*w);
                            this.config.units[i].height = parseInt(dim.h*h);
                        }
                    }
                }
            },
            
            updateDimensions : function() {
                this.initDimensions();

                for(var i=0; i<this.config.units.length; i++) {
                    var pos = this.config.units[i].position;
                    if(pos != 'center') {
                        var x = this.layout.getUnitByPosition(pos);
                        x._configs.width.value = this.config.units[i].width;
                        x._configs.height.value = this.config.units[i].height;
                    }
                }
            },
            
            loadStoredDimensions : function() {
                return false;
            }
        });
        
        //remove
        YAHOO.hippo.TabsWireframe = function(id, config) {
            YAHOO.hippo.TabsWireframe.superclass.constructor.apply(this, arguments);
        };

        YAHOO.extend(YAHOO.hippo.TabsWireframe, YAHOO.hippo.Wireframe, {
            afterRender : function() {
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