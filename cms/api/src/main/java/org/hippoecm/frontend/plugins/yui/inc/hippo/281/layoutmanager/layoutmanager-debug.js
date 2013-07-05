/*
 * Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
                Wicket.Event.subscribe('/ajax/call/success', Wicket.bind(this.render, this));
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
                if (this.root !== null) {
                    try {
                        this.root.resize();
                        this.resizeEvent.fire();
                    } catch(e) {
                        YAHOO.log('Error resizing root: ' + e, 'error');
                    }
                }
            },

            render : function() {
                this.cleanupWireframes();
            },

            addRoot : function(id, Clazz, config) {
                this.root = new Clazz(id, config, this);
                this.root.render();
            },
            
            addWireframe : function(id, clazz, config) {
                var object = new clazz(id, config, this);
                this.wireframes.put(id, object);
                object.render();
            },

            handleExpandCollapse : function(element) {
                var unit, layout, layoutId, wireframe, position, unitCfg;
                while(true) {
                    unit = this.findLayoutUnit(element);
                    if(unit === null) {
                        return;
                    }
                    layout = unit.get('parent');
                    layoutId = layout.get('id');
                    wireframe = null;
                    if(this.wireframes.containsKey(layoutId)) {
                        wireframe = this.wireframes.get(layoutId);
                    } else if (this.root.layout.get('id') === layoutId) {
                        wireframe = this.root;
                    }
                    if(wireframe === null) {
                        return;
                    }
                    position = unit.get('position');
                    unitCfg = wireframe.getUnitConfigByPosition(position);
                    if (unitCfg !== null && unitCfg.expandCollapseEnabled) {
                        //do callback
                        wireframe.config.callbackFunction({position: position});
                        return;
                    }
                    element = Dom.get(wireframe.id);
                }
            },

            expandUnit : function(id, position) {
                if(this.wireframes.containsKey(id)) {
                    this.wireframes.get(id).expandUnit(position);
                }
            },

            collapseUnit : function(id, position) {
                if(this.wireframes.containsKey(id)) {
                    this.wireframes.get(id).collapseUnit(position);
                }
            },

            getWireframe : function(id) {
               if (this.root !== null && id === this.root.id) {
                   return this.root;
               }
               return this.wireframes.get(id);
            },

            cleanupWireframes : function() {
                var remove, i;
                remove = [];
                this.wireframes.forEach(this, function(key, value){
                    var bodyEl = HippoDom.resolveElement(key);
                    if (bodyEl === null) {
                        remove.push(key);
                    }
                });
                for (i = 0; i < remove.length; i++) {
                    if (this.wireframes.containsKey(remove[i])) {
                        this.cleanup(remove[i]);
                    }
                }
            },
            
            cleanup : function(id) {
                var wireframe = this.wireframes.remove(id);
                if(wireframe.parent !== null) {
                    wireframe.parent.removeChild(wireframe);
                }
                wireframe.children.forEach(this, function(k, v) {
                    this.cleanup(k);
                });
                wireframe.children.clear();
                if (wireframe.layout !== null) {
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
                var layoutUnit, timeoutLength;
                layoutUnit = this.findLayoutUnit(el);
                if(layoutUnit === null) {
                    YAHOO.log('Unable to find ancestor layoutUnit for element[@id=' + el.id + ']', 'error', 'LayoutManager');
                    return;
                }
                timeoutLength = this.throttleDelay;
                if(!Lang.isUndefined(_timeoutLength) && Lang.isNumber(_timeoutLength)) { //also check for false for backward compatibility
                    timeoutLength = _timeoutLength;
                }
                this.registerEventListener(layoutUnit, layoutUnit, 'resize', obj, func, executeNow, timeoutLength);
            },

            registerRenderListener : function(el, obj, func, executeNow) {
                var layoutUnit = this.findLayoutUnit(el);
                if(layoutUnit === null) {
                    YAHOO.log('Unable to find ancestor layoutUnit for element[@id=' + el.id + ', can not register render event', 'error', 'LayoutManager');
                    return;
                }
                this.registerEventListener(layoutUnit.get('parent'), layoutUnit, 'render', obj, func, executeNow);
            },

            /**
             * Because we use the Wicket.Throttler we loose our context, so never store your function like 'this.update'
             * but rather fallback to the super-ugly "var me = this; var func = function() {me.update()};" construct..
             *
             * @param target
             * @param unit
             * @param evt
             * @param obj
             * @param func
             * @param executeNow
             * @param timeoutLength
             */
            registerEventListener : function(target, unit, evt, obj, func, executeNow, timeoutLength) {
                var oid, myId, useTimeout, eventName, info, self, callback;

                oid = Lang.isUndefined(obj.id) ? Dom.generateId() : obj.id;
                
                myId = '[' + evt + ', ' + oid + ']';
                if(executeNow) {
                    YAHOO.log('ExecuteNow' + myId, 'info', 'LayoutManager');
                    func.apply(obj, [unit.getSizes()]);
                }
                useTimeout = !Lang.isUndefined(timeoutLength) && Lang.isNumber(timeoutLength) && timeoutLength > 0;
                eventName = evt + 'CustomEvent';
                info = { numberOfTimeouts:0, absStart: null, relStart: null };
                self = this;
                
                if(Lang.isUndefined(target[eventName + 'Subscribers'])) {
                    target[eventName + 'Subscribers'] = new YAHOO.hippo.HashMap();

                    callback = function() {
                        var prevTime, execute;

                        if(info.numberOfTimeouts === 0) {
                            YAHOO.log('Callback' + myId + ' called, timeout=' + useTimeout, 'info', 'LayoutManager');
                            info.absStart = new Date();
                        } else {
                            prevTime = new Date().getTime() - info.relStart.getTime();
                            YAHOO.log('Callback' + myId + ' re-called after ' + prevTime + 'ms, timeouts=' + info.numberOfTimeouts, 'info', 'LayoutManager');
                        }
                        execute = function() {
                            var abs, evtStart, values, i, f;

                            abs = new Date().getTime() - info.absStart.getTime();
                            YAHOO.log('Execute' + myId + ' called after ' + abs + 'ms, timeouts=' + info.numberOfTimeouts + ', timeout=' + useTimeout, 'info', 'LayoutManager');
                            info = { numberOfTimeouts:0, absStart: null, relStart: null };
                            
                            evtStart = new Date();
                            values = target[eventName + 'Subscribers'].valueSet();
                            for (i = 0; i < values.length; i++) {
                                f = values[i];
                                f.apply(obj, [unit.getSizes()]);
                            }

                            YAHOO.log('Execute' + myId + ' handling took ' + (new Date().getTime()-evtStart.getTime()) + 'ms', 'info', 'LayoutManager');
                        };
                        if (useTimeout) {
                            info.relStart = new Date();
                            YAHOO.log('Throttle' + myId + ', timeoutLength=' + timeoutLength, 'info', 'LayoutManager');
                            info.numberOfTimeouts++;

                            timeoutLength = 10;
                            this.throttler.throttle(eventName + oid, timeoutLength, execute);
                        } else {
                            execute.apply(self);
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
                if (layoutUnit === null) {
                    YAHOO.log('Unable to find ancestor layoutUnit for element[@id=' + el.id + ']', 'error', 'LayoutManager');
                    return false;
                }
                return this.unregisterEventListener('resize', layoutUnit, obj);
            },

            unregisterRenderListener : function (el, obj) {
                var layoutUnit = this.findLayoutUnit(el);
                if (layoutUnit === null) {
                    YAHOO.log('Unable to find ancestor layoutUnit for element[@id=' + el.id + ', can not unregister render event', 'error', 'LayoutManager');
                    return false;
                }
                return this.unregisterEventListener('render', layoutUnit.get('parent'), obj);
            },

            unregisterEventListener : function (evt, target, obj) {
                var oid = obj['SubcribeId' + evt],
                    set = target[evt + 'CustomEventSubscribers'];
                if (set !== null && set.containsKey(oid)) {
                    set.remove(oid);
                    return true;
                }
                return false;
            },

            findLayoutUnit : function(el) {
                el = this._findUnitElement(el);
                if (el !== null) {
                    return YAHOO.widget.LayoutUnit.getLayoutUnitById(el.id);
                }
                return null;
            },
            
            /**
             * Dom.getAncestorByClassName didn't work
             */
            _findUnitElement : function(el) {
                while (el !== null && el !== document.body && (this.root === null || el.id !== this.root.id)) {
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
            this.unitExpanded = null;
            this.layoutInitialized = false;

            this.name = id.indexOf(':') > -1 ? id.substr(id.indexOf(':') + 1) : id;
            
            this.DIM_COOKIE = 'hippocms7-layout-sizes';
        };
        
        YAHOO.hippo.BaseWireframe.prototype = {
            render: function() {
                if (this.layout === null) {

                    this.enhanceIds();
                    this.prepareConfig();
                
                    this.layout = new YAHOO.widget.Layout(this.id, this.config);
                    this.initLayout();
                    
                    try {
                        this.layout.render();
                    } catch(e) {
                        YAHOO.log('An error occured during render of wireframe[' + this.id +'], dump=' + Lang.dump(this), 'error', 'Wireframe');
                    }
                    this.layoutInitialized = true;
                }
                this.afterRender();
            },
            
            initLayout : function() {
                var me = this;
                this.layout.on('beforeResize', function() {
                    me.loadDimensions();
                    Dom.setStyle(me.id, 'height', me.config.height + 'px');
                    Dom.setStyle(me.id, 'width',  me.config.width + 'px');
                });
                
                this.layout.on('resize', this.onLayoutResize, null, this);
            },
            
            onLayoutResize: function() {
                var values, i;
                if (this.layoutInitialized) {
                    try {
                        this.storeDimensions();
                    } catch(e) {
                    }
                }
                values = this.children.valueSet();
                for (i = 0; i < values.length; i++) {
                    values[i].resize();
                }
            },

            resize : function() {
                if(this.layout !== null) {
                    this.layout.resize();
                }
                if(this.unitExpanded) {
                    this.expandUnit(this.unitExpanded);
                }
            },

            registerChild  : function(c) {
                this.children.put(c.id, c);  
            },
              
            removeChild : function(c) {
              this.children.remove(c.id);  
            },
            
            prepareConfig : function() {
                var i, len, unit;
                for (i = 0, len = this.config.units.length; i < len; i++) {
                    unit = this.config.units[i];
                    if (unit.body === null) {
                        delete unit.body;
                    }
                }
            },
            
            loadDimensions : function() {
            },
            
            storeDimensions : function() {
                var i, len;

                if (this.unitExpanded !== null) {
                    //Don't store expanded dimensions
                    return;
                }
                YAHOO.log('Store dimensions for: ' + this.name, 'info', 'Wireframe');

                for (i = 0, len = this.config.units.length; i < len; ++i) {
                    if (this.config.units[i].position !== 'center') {
                        this.storeDimension(this.config.units[i]);
                    }
                }
            },
            
            storeDimension : function(unitConfig) {
                var pos, sizes, width, height, date, opts;

                pos = unitConfig.position;
                sizes  = this.layout.getSizes();
                width  = sizes[pos].w;
                height = sizes[pos].h;
                if(unitConfig.resize) {
                    date = new Date();
                    date.setDate(date.getDate() + 31);
                    opts = { expires: date };
                    YAHOO.util.Cookie.setSub(this.DIM_COOKIE, this.name + ':' + pos, width + ',' + height, opts);
                    YAHOO.log('Stored dimension for ' + this.name + ':' + pos, 'info', 'Wireframe');
                }
            },
            
            readDimension : function(pos) {
                var val, ar;

                val = YAHOO.util.Cookie.getSub(this.DIM_COOKIE, this.name + ':' + pos);
                if (val === null) {
                    return false;
                }
                ar = val.split(',');
                return {w:ar[0], h:ar[1]};
            },
            
            afterRender : function() {
                var i, len, unitConfig, unit;

                if (Dom.hasClass(this.id, 'smooth_load_workaround')) {
                    Dom.removeClass(this.id, 'smooth_load_workaround');
                }
                
                for (i = 0, len = this.config.units.length; i < len; i++) {
                    unitConfig = this.config.units[i];
                    unit = this.layout.getUnitByPosition(unitConfig.position);
                    if(unit) {
                        if(unitConfig.zindex > 0) {
                            unit.setStyle('zIndex', unitConfig.zindex);
                        }

                        //By default, yui-layout units don't dynamically keep a maxWidth/minWidth with respect to their neighbors
                        //which means a user can render the UI useless. To prevent this we add a check right when the unit's
                        //resize event finishes
                        unit.on('endResize', this.onEndResizeUnit, unit, this);

                        if(unitConfig.expanded === true) {
                            this.expandUnit(unitConfig.position);
                        }
                    }
                }
            },

            onEndResizeUnit : function(o, unit) {
                var sizes, minWidth, newWidth, offset, diff;

                //if the width of this unit is bigger than the layout width, it will
                //overlap neighboring units. A 20px margin is used.
                //Added check for minWidth as well
                sizes = unit.get('parent').getSizes();
                minWidth = unit.get('minWidth');
                newWidth = unit.get('width');
                offset = minWidth !== null ? minWidth : 20;

                if ((sizes.doc.w - offset) < newWidth) {
                    unit.set('width', sizes.doc.w - offset);
                } else {
                    //else check if the new width isn't rendering nested layout's units invisible.
                    //if a number less than zero is returned, it resembles the offset of the least
                    //visible unit. This offset + the new width will make the unit still invisible, so
                    //we add 20 pixels to it to define the new width
                    diff = this.newWidthIsOk();
                    if (diff < 0) {
                        unit.set('width', (newWidth - diff) + 20);
                    }
                }

            },

            newWidthIsOk : function() {
                var result, i, len, unit, unitWidth;

                result = 0;
                for (i = 0, len = this.config.units.length; i < len; i++) {
                    unit = this.layout.getUnitByPosition(this.config.units[i].position);
                    unitWidth = unit.get('width');
                    if (unitWidth < result) {
                        result = unitWidth;
                    }
                }
                this.children.forEach(this, function(key, wireframe) {
                    var diff = wireframe.newWidthIsOk();
                    if (diff < result) {
                        result = diff;
                    }
                });
                return result;
            },
            
            enhanceIds : function() {
                var i, len, u, uid, unitEl;

                YAHOO.hippo.Dom.enhance(HippoDom.resolveElement(this.id), this.id);
                for (i = 0, len = this.config.units.length; i < len; i++) {
                    u = this.config.units[i];
                    uid = u.id;
                    unitEl = HippoDom.resolveElement(uid);
                    HippoDom.enhance(unitEl, uid);
                    this.config.units[i].yId = unitEl.id;
                    
                    uid = u.body;
                    if (uid  !== null && uid !== undefined) {
                        unitEl = HippoDom.resolveElement(uid);
                        if(unitEl === null) {
                            throw new Error("Could not find element with uid " + uid);
                        }
                        HippoDom.enhance(unitEl, uid);
                        this.config.units[i].bId = unitEl.id;
                    }
                }
            },

            expandUnit : function(position) {
                var unit = this.layout.getUnitByPosition(position);
                if (unit !== null) {
                    this.unitExpanded = position;
                    unit.set('width', this.layout.getSizes().doc.w);
                }
            },

            collapseUnit : function(position) {
                var unit, config;

                unit = this.layout.getUnitByPosition(position);
                if (unit !== null) {
                    config = this.getUnitConfigByPosition(position);
                    if(config !== null) {
                        this.unitExpanded = null;
                        unit.set('width', Number(config.width));
                        this.children.forEach(this, function(k, v) {
                            v.checkSizes();
                        });
                    }
                }
            },

            checkSizes : function() {
                var i, len, unit;

                for (i = 0, len = this.config.units.length; i < len; i++) {
                    unit = this.layout.getUnitByPosition(this.config.units[i].position);
                    this.onEndResizeUnit(null, unit);
                }
                this.children.forEach(this, function(k, v) {
                    v.checkSizes();
                });
            },

            getUnitConfigByPosition : function(position) {
                var i, len;
                for (i = 0, len = this.config.units.length; i < len; ++i) {
                    if (this.config.units[i].position === position) {
                        return this.config.units[i];
                    }
                }
                return null;
            }

        };

        YAHOO.hippo.GridsRootWireframe = function(id, config) {
            var units, body, header, left, right, footer;

            YAHOO.hippo.GridsRootWireframe.superclass.constructor.apply(this, arguments); 
            
            units = [];
            body = { position: 'center', body: 'bd', grids: true, scroll: config.bodyScroll};
            if (config.bodyGutter !== null) {
                body.gutter = config.bodyGutter;
            }
            units.push(body);

            if (Lang.isNumber(config.headerHeight) && config.headerHeight > 0) {
                header = {position: 'top', body: 'hd', height: config.headerHeight, scroll: config.headerScroll, resize: config.headerResize, grids: true};
                if (config.headerGutter !== null) {
                    header.gutter = config.headerGutter;
                }
                units.push(header);
            }

            if (Lang.isNumber(config.leftWidth) && config.leftWidth > 0) {
                left = {position: 'left', body: 'lt', width: config.leftWidth, scroll: config.leftScroll, resize: config.leftResize };
                if (config.leftGutter !== null) {
                    left.gutter = config.leftGutter;
                }
                units.push(left);
            }
            
            if(Lang.isNumber(config.rightWidth) && config.rightWidth > 0) {
                right = {position: 'right', body: 'rt', width: config.rightWidth, scroll: config.rightScroll, resize: config.rightResize };
                units.push(right);
            }

            if(Lang.isNumber(config.footerHeight) && config.footerHeight > 0) {
                footer = {position: 'bottom', body: 'ft', height: config.footerHeight, scroll: config.footerScroll, resize: config.footerResize, grids: true};
                if (config.footerGutter !== null) {
                    footer.gutter = config.footerGutter;
                }
                units.push(footer);
            }

            this.config.units = units;
            this.margins = null;
        };

        YAHOO.extend(YAHOO.hippo.GridsRootWireframe, YAHOO.hippo.BaseWireframe, {
            render: function() {
                if (this.layout === null) {
                    this.prepareConfig();
                    this.layout = new YAHOO.widget.Layout(this.config);
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

            loadDimensions : function() {
                this.config.height = Dom.getClientHeight()-this.margins.h;
                this.config.width = Dom.getClientWidth()-this.margins.w; //this works better than offsetWidth
            }

        });

        /**
         * Wireframe that functions as a root wireframe in a portlet environment
         */
        YAHOO.hippo.PortletWireframe = function(id, config) {
            var units, body, header, left, right, footer;

            YAHOO.hippo.PortletWireframe.superclass.constructor.apply(this, arguments); 
            
            units = [];
            body = { position: 'center', body: 'bd', grids: true, scroll: config.bodyScroll};
            if (config.bodyGutter !== null) {
                body.gutter = config.bodyGutter;
            }
            units.push(body);
            
            if(Lang.isNumber(config.headerHeight) && config.headerHeight > 0) {
                header = {position: 'top', body: 'hd', height: config.headerHeight, scroll: config.headerScroll, resize: config.headerResize, grids: true};
                if (config.headerGutter !== null) {
                  header.gutter = config.headerGutter;
                }
                units.push(header);
            }
            
            if(Lang.isNumber(config.leftWidth) && config.leftWidth > 0) {
                left = {position: 'left', body: 'lt', width: config.leftWidth, scroll: config.leftScroll, resize: config.leftResize };
                if(config.leftGutter !== null) {
                    left.gutter = config.leftGutter;
                }
                units.push(left);
            }
            
            if(Lang.isNumber(config.rightWidth) && config.rightWidth > 0) {
                right = {position: 'right', body: 'rt', width: config.rightWidth, scroll: config.rightScroll, resize: config.rightResize };
                units.push(right);
            }

            if(Lang.isNumber(config.footerHeight) && config.footerHeight > 0) {
                footer = {position: 'bottom', body: 'ft', height: config.footerHeight, scroll: config.footerScroll, resize: config.footerResize, grids: true};
                if (config.footerGutter !== null) {
                  footer.gutter = config.footerGutter;
                }
                units.push(footer);
            }

            this.config.units = units;
            this.margins = null;
        };

        YAHOO.extend(YAHOO.hippo.PortletWireframe, YAHOO.hippo.BaseWireframe, {
            render: function() {
                if (this.layout === null) {
                    this.prepareConfig();
                    this.layout = new YAHOO.widget.Layout(this.id, this.config);
                    this.initLayout();
                }
                this.layout.render();
            },

            onLayoutResize : function() {
                YAHOO.hippo.PortletWireframe.superclass.onLayoutResize.call(this);
                
                //HREPTWO-3072 it seems the body with/height is set once by YUI-layout
                //after a resize it's not updated, so we set it to auto instead
                var el = Dom.get(this.id);
                Dom.setStyle(el, 'width', 'auto');
                Dom.setStyle(el, 'height', 'auto');
            },
            
            prepareConfig : function() {
                var el = Dom.get(this.id);
                this.margins  = new YAHOO.hippo.DomHelper().getMargin(el);
                this.config.width = el.clientWidth - this.margins.w; //Width of the outer element
                this.config.height = el.clientHeight - this.margins.h;
            },

            loadDimensions : function() {
                var el = Dom.get(this.id);
                this.config.height = el.clientHeight;
                this.config.width = el.clientWidth;
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
                YAHOO.hippo.Wireframe.superclass.prepareConfig.call(this);

                if (this.config.linkedWithParent) {
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
                var i, len, unit, dim;
                for (i = 0, len = this.config.units.length; i < len; i++) {
                    unit = this.config.units[i];
                    if (unit.resize) {
                        dim = this.readDimension(unit.position);
                        if(dim) {
                            this.config.units[i].width = dim.w;
                            this.config.units[i].height = dim.h;
                        }
                    }
                }
            },
            
            loadDimensions : function() {
                var dim = this.getDimensions();
                this.config.height = dim.h;
                this.config.width = dim.w;

                YAHOO.log('Load dimensions for: ' + this.name, 'info', 'Wireframe');
            },
            
            getDimensions : function() {
                var dim, margin, myParent, parentMargin, u;

                dim = {};
                if (this.config.linkedWithParent && this.parent !== null) {

                    margin = {w:0, h:0};
                    myParent = Dom.get(this.id).parentNode;
                    while (myParent !== null) {
                        parentMargin = this.domHelper.getMargin(myParent);
                        margin.w += parentMargin.w;
                        margin.h += parentMargin.h;
                        
                        if (Dom.hasClass(myParent, 'yui-layout-unit')) {
                            //unit found, wrap it up
                            dim.w = this.domHelper.getWidth(myParent);
                            dim.h = this.domHelper.getHeight(myParent);
                            dim.w -= margin.w;
                            dim.h -= margin.h;

                            if (this.parent !== null) {
                                u = this.parent.layout.getUnitById(myParent.id);
                                if (u !== null) {
                                    dim.w -= (u._gutter.left + u._gutter.right);
                                    dim.h -= (u._gutter.top + u._gutter.bottom);
                                }
                            }
                            return dim;
                        }
                        myParent = myParent.parentNode;
                    }
                }
                //fallback
                myParent = Dom.get(this.id).parentNode;
                dim.w = this.domHelper.getWidth(myParent);
                dim.h = this.domHelper.getHeight(myParent);
                return dim;
            }
            
        });
        
        YAHOO.hippo.RelativeWireframe = function(id, config) {
            var i, len, unit, r, w, h;

            YAHOO.hippo.RelativeWireframe.superclass.constructor.apply(this, arguments);
            
            this.relativeUnits = [];
            for (i = 0, len = config.units.length; i < len; i++) {
                unit = config.units[i];
                r = {};
                w = unit.width;
                if (Lang.isUndefined(w)) {
                    r.w = 1;
                } else if (Lang.isString(w) && w.indexOf('%') > -1) {
                    r.w = parseInt(w.substr(0, w.indexOf('%')), 10) / 100;
                }
                h = unit.height;
                if (Lang.isUndefined(h)) {
                    r.h = 1;
                } else if(Lang.isString(h) && h.indexOf('%') > -1) {
                    r.h = parseInt(h.substr(0, h.indexOf('%')), 10) / 100;
                }
                this.relativeUnits[unit.position] = r;
            }
        };

        YAHOO.extend(YAHOO.hippo.RelativeWireframe, YAHOO.hippo.Wireframe, {
            
            initDimensions : function() {
                var dim, i, len, pos, w;

                dim = this.getDimensions();
                this.config.height = dim.h;
                this.config.width = dim.w;

                for (i = 0, len = this.config.units.length; i < len; i++) {
                    pos = this.config.units[i].position;
                    if (pos !== 'center' && pos !== 'top' && pos !== 'bottom') {
                        w = this.relativeUnits[pos].w;
                        this.config.units[i].width = parseInt(dim.w * w, 10);

                        //var h = this.relativeUnits[pos].h;
                        //this.config.units[i].height = parseInt(dim.h*h);
                    }
                }
            },
            
            loadDimensions : function() {
                var i, len, pos, x;

                this.initDimensions();

                for (i = 0, len = this.config.units.length; i < len; i++) {
                    pos = this.config.units[i].position;
                    if (pos !== 'center' && pos !== 'top' && pos !== 'bottom') {
                        x = this.layout.getUnitByPosition(pos);
                        x._configs.width.value = this.config.units[i].width;
                    }
                }
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
    }());

    YAHOO.hippo.LayoutManager = new YAHOO.hippo.LayoutManagerImpl();
    YAHOO.register("layoutmanager", YAHOO.hippo.LayoutManager, {
        version: "2.8.1", build: "19"
    });
}

//PATCH

( function() {
    var Event = YAHOO.util.Event, Lang = YAHOO.lang;

    YAHOO.widget.Layout.prototype.destroy = function() {
        var par, u, i;

        par = this.get('parent');
        if (par) {
            par.removeListener('resize', this.resize, this, true);
        }
        Event.removeListener(window, 'resize', this.resize, this, true);
        this.unsubscribeAll();
        for (u in this._units) {
            if (this._units.hasOwnProperty(u)) {
                if (this._units[u]) {
                    this._units[u].destroy(true);
                }
            }
        }

        Event.purgeElement(this.get('element'));

        delete YAHOO.widget.Layout._instances[this.get('id')];
        //Brutal Object Destroy
        for (i in this) {
            if (this.hasOwnProperty(i)) {
                this[i] = null;
                delete this[i];
            }
        }
    };

    YAHOO.widget.LayoutUnit.prototype.destroy = function(force) {
        var par, wrap, body, i;

        if (this._resize) {
            this._resize.destroy();
        }
        par = this.get('parent');
        
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

        wrap = this.get('wrap');
        if (wrap) {
            delete YAHOO.widget.LayoutUnit._instances[wrap.id];
        }
        body = this.get('body');
        if (body) {
            delete YAHOO.widget.LayoutUnit._instances[body];
        }

        Event.purgeElement(this.get('element'));

        delete YAHOO.widget.LayoutUnit._instances[this.get('id')];
        //Brutal Object Destroy
        for (i in this) {
            if (Lang.hasOwnProperty(this, i)) {
                this[i] = null;
                delete this[i];
            }
        }
        return par;
    };

}());
