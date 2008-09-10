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
 * @requires yahoo, dom, layout, resize, functionqueue, hippodom, json
 * @module layoutmanager
 * @beta
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.LayoutManager) { // Ensure only one layout manager exists
    ( function() {
        var Dom = YAHOO.util.Dom, Lang = YAHOO.lang, HippoDom = YAHOO.hippo.Dom;

        YAHOO.hippo.Wireframe = function(id, parentId) {
            this.id = id;
            this.parentId = parentId;
            this.layout = null;
            this.init = null;
            this.resizeEvent = null;
            this.callbackUrl = null;
        };

        YAHOO.hippo.LayoutManagerImpl = function() {
          this.init();
        };

        YAHOO.hippo.LayoutManagerImpl.prototype = {
            ROOT_ELEMENT_ID :'ROOT_ELEMENT_ID',
            wireframes      : new Array(),
            createQueue     : new YAHOO.hippo.FunctionQueue('create'),
            renderQueue     : new YAHOO.hippo.FunctionQueue('render'),
            throttle        : new Wicket.Throttler(true),
            throttleDelay   : 2000,
            
            init : function() {
              var _this = this;
              Wicket.Ajax.registerPreCallHandler(function(){_this.flushThrottle()});
            },
            
            flushThrottle : function() {
                YAHOO.log('Flush the throttle', 'info', 'LayoutManager');
                for(var id in this.throttle.entries) {
                    var entry = this.throttle.entries[id];
                    //Explicit undefined/null checking for IE
                    if(!Lang.isUndefined(entry) && !Lang.isNull(entry) 
                            && Lang.isFunction(entry.getTimeoutVar)) {
                        window.clearTimeout(entry.getTimeoutVar());
                        this.throttle.execute(id);
                    }
                }
            },

            onLoad : function() {
                YAHOO.log('onLoad', 'info', 'LayoutManager');
                this.cleanupWireframes();
                this.createWireframes();
                this.renderWireframes();
            },

            cleanupWireframes : function() {
                var newlist = [];
                newlist[this.ROOT_ELEMENT_ID] = this.wireframes[this.ROOT_ELEMENT_ID];
                for(var i in this.wireframes) {
                    if(this.wireframes[i].parentId != undefined  && i != this.ROOT_ELEMENT_ID) {
                        var bodyEl = HippoDom.resolveElement(this.wireframes[i].id);
                        if (bodyEl == null) {
                            this.cleanup(this.wireframes[i].id);
                        } else {
                            newlist[i] = this.wireframes[i];
                        }
                    }
                }
                this.wireframes = newlist;
            },
            
            createWireframes : function() {
                YAHOO.log('Create ' + this.createQueue.queue.length + 'wireframes', 'info', 'LayoutManager');
                this.createQueue.handleQueue();
            },

            renderWireframes : function() {
                YAHOO.log('Render ' + this.renderQueue.queue.length + 'wireframes', 'info', 'LayoutManager');                
                this.renderQueue.handleQueue();
            },

            cleanup : function(id) {
                YAHOO.log("Start cleanup of wireframe[" + id + "]", 'info', 'LayoutManager');
                for ( var i in this.wireframes) {
                    if (!Lang.isFunction(i)) {
                        var parId = this.wireframes[i].parentId;
                        if (!Lang.isUndefined(parId) && !Lang.isNull(parId)
                                && parId == id) {
                            this.cleanup(this.wireframes[i].id);
                            break;
                        }
                    }
                }
                var layout = this.wireframes[id].layout;
                if (layout != null) {
                    layout.destroy();
                    this.wireframes[id].layout = null;
                    YAHOO.log('Destroyed layout of wireframe[' + id + ']', 'info', 'LayoutManager');
                }
                YAHOO.log("Cleanup of wireframe[" + id + "] finished", 'info', 'LayoutManager');
            },

            renderWireframe : function(id) {
                YAHOO.log("Render wireframe[" + id + "]", 'info', 'LayoutManager');
                this.wireframes[id].layout.render();

                // TODO: make this configurable
                var el = this.wireframes[id].layout.getUnitByPosition("center")
                        .get("wrap");
                if (Dom.getStyle(el, 'display') == 'none') {
                    // YAHOO.widget.Effects.Appear(el, {ease:
                    // YAHOO.util.Easing.easeIn, seconds: 0.5, delay: false});
                    //YAHOO.widget.Effects.Show(el);
                    Dom.setStyle(el, 'display', 'block');
                }
            },

            registerResizeListener : function(el, obj, func) {
                YAHOO.log('Element[' + el.id + '] tries to register as resizeListener', 'info', 'LayoutManager');
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
                    layoutUnit.on('resize', function() {
                        layoutUnit.customEvent.fire(layoutUnit.getSizes());
                    });
                    layoutUnit.customEvent.fire(layoutUnit.getSizes());
                }
            },

            findLayoutUnit : function(el) {
                while (el != null && el != document.body) {
                    if (Dom.hasClass(el, 'yui-layout-unit')) {
                        return el;
                    }
                    el = el.parentNode;
                }
                return false;
            },

            createWireframe : function(_id, _parentId, linkedWithParent, callbackUrl, config) {
                var _this = this;
                var id = (_id == '') ? this.ROOT_ELEMENT_ID : _id;
                var parentId = (linkedWithParent && _parentId == '') ? this.ROOT_ELEMENT_ID
                        : _parentId;

                if (this.wireframes[id] == null) {
                    this.wireframes[id] = new YAHOO.hippo.Wireframe(id, parentId);
                }
                var wireframe = this.wireframes[id];
                wireframe.callbackUrl = callbackUrl;
                
                for(var i in config) {
                    if(!Lang.isFunction(config[i]) && i != 'units') {
                        str += (' ' + i + '=' + config[i]);
                    }
                }
                YAHOO.log('Create wireframe[' + id + '] - parentId=' + parentId + ' - linkedWithParent=' + linkedWithParent + str, 'info', 'LayoutManager');
                for(var i in config.units) {
                    var j = config.units[i];
                    if(!Lang.isFunction(j)) {
                        var str = '';
                        for(var k in j) {
                            if(!Lang.isFunction(j[k]) && k != 'position') {
                                str += (k + '=' + j[k] + ' ');
                            }
                        }
                        YAHOO.log('Wireframe[' + id + '] unit[' + j.position + ']: ' + str, 'info', 'LayoutManager');                    
                    }
                }
                var update = wireframe.layout != null;
                if (update)
                    this.cleanup(id);

                if (linkedWithParent) {
                    var childInit = function() {
                        YAHOO.log('Linked wireframe[' + id + '] initialize function', 'info', 'LayoutManager');
                        var parentLayout = _this.wireframes[parentId].layout;
                        config.parent = parentLayout;

                        var bodyEl = HippoDom.resolveElement(id);
                        YAHOO.hippo.Dom.enhance(bodyEl, id);

                        var parentLayoutUnitEl = _this.findLayoutUnit(bodyEl);
                        var parentCenterEl = YAHOO.widget.LayoutUnit
                                .getLayoutUnitById(parentLayoutUnitEl.id).get('wrap');
                        var parentWidth = Dom.getStyle(parentCenterEl, 'width')
                        var parentHeight = Dom.getStyle(parentCenterEl,
                                'height')
                                
                        var parentWidth = Dom.getStyle(parentCenterEl, 'width');
                        var parentHeight = Dom.getStyle(parentCenterEl, 'height')
                        
                        var border = _this.getBorder(bodyEl);      
                        if(parentWidth.indexOf('px') > 0) {
                        	parentWidth = parseInt(parentWidth.substr(0, parentWidth.indexOf('px')));
                            parentWidth -= border.x;
                            parentWidth += 'px';
                        }    					
                        if(parentHeight.indexOf('px') > 0) {
                        	parentHeight = parseInt(parentHeight.substr(0, parentHeight.indexOf('px')));
                            parentHeight -= border.y;
                            parentHeight += 'px';
                        }    					

                        Dom.setStyle(bodyEl, 'width', parentWidth);
                        Dom.setStyle(bodyEl, 'height', parentHeight);

                        // TODO: This should be a custom configAttr in the unit
                        // itself, so write a patch
                        for ( var i = 0; i < config.units.length; i++) {
                            var u = config.units[i];
                            var pos = u.position;
                            if (!Lang.isUndefined(u.width)
                                    && !Lang.isNull(u.width)
                                    && !Lang.isNumber(u.width)
                                    && (pos == 'left' || pos == 'right')) {
                                u.width = _this.calculateSizeFromPercentage(
                                        u.width, parentWidth);
                            } else if (!Lang.isUndefined(u.height)
                                    && !Lang.isNull(u.height)
                                    && !Lang.isNumber(u.height)
                                    && (pos == 'top' || pos == 'bottom')) {
                                u.height = _this.calculateSizeFromPercentage(
                                        u.height, parentHeight);
                            }
                        }

                        _this.addLinkedWireframe(id, config, bodyEl);
                    }
                    _this.wireframes[id].init = childInit;
                    if (update) {
                        var func = function() {
                            _this.renderWireframe(_this.ROOT_ELEMENT_ID);
                        };
                        this.renderQueue.registerFunction(func,
                                _this.ROOT_ELEMENT_ID);
                    }
                } else {
                    var initFunc = null;
                    if (id == this.ROOT_ELEMENT_ID) {
                        initFunc = function() {
                            YAHOO.log('Create root wireframe', 'info', 'LayoutManager');
                            _this.addLinkedWireframe(id, config);
                        };
                    } else {
                        initFunc = function() {
                            var bodyEl = HippoDom.resolveElement(id);
                            HippoDom.enhance(bodyEl, id);
                            _this.wireframes[id].layout = new YAHOO.widget.Layout(
                                    id, config);
                        };
                    }
                    this.createQueue.registerFunction(initFunc, id);
                    
                    //Might move these into the init functions
                    this.renderQueue.registerFunction( function() {
                        _this.renderWireframe(id)
                    }, id);
                }
            },

            addLinkedWireframe : function(id, config, bodyEl) {
                var _this = this;
                for ( var i = 0; i < config.units.length; i++) {
                    var u = config.units[i];
                    var uid = u["id"];
                    var unitEl = HippoDom.resolveElement(uid);
                    HippoDom.enhance(unitEl, uid);
                    
                    uid = u["body"];
                    if (uid != undefined) {
                        unitEl = HippoDom.resolveElement(uid);
                        HippoDom.enhance(unitEl, uid);
                    }
                }

                var layout = null;
                if (bodyEl == undefined || bodyEl == null) {
                    layout = new YAHOO.widget.Layout(config);
                } else {
                    layout = new YAHOO.widget.Layout(bodyEl, config);
                }

                layout.on('render', function() {
                    YAHOO.log('Wireframe[' + id + '] onrender', 'info', 'LayoutManager');
                    for(var i in _this.wireframes) {
                        if(_this.wireframes[i].parentId != undefined && _this.wireframes[i].parentId == id) {
                            if (_this.wireframes[i].init != null) {
                                YAHOO.log('Wireframe[' + id + '] childInit', 'info', 'LayoutManager');
                                _this.wireframes[i].init();
                                _this.wireframes[i].init = null;
                            }
                            YAHOO.log('Wireframe[' + id + '] find child and render it', 'info', 'LayoutManager');
                            _this.renderWireframe(_this.wireframes[i].id);
                        }
                    }
                });
                layout.on('beforeResize', function() {
                    var bodyEl = Dom.get(id);
                    if (bodyEl) {
                        var parentLayoutUnitEl = _this.findLayoutUnit(bodyEl);
                        var parentCenterEl = YAHOO.widget.LayoutUnit
                                .getLayoutUnitById(parentLayoutUnitEl.id).get('wrap');
                        if (parentCenterEl) {
                        	 var parentWidth = Dom.getStyle(parentCenterEl, 'width');
                             var parentHeight = Dom.getStyle(parentCenterEl, 'height')
                             var border = _this.getBorder(bodyEl);      
                             if(parentWidth.indexOf('px') > 0) {
                             	parentWidth = parseInt(parentWidth.substr(0, parentWidth.indexOf('px')));
                                 parentWidth -= border.x;
                                 parentWidth += 'px';
                             }    					
                             if(parentHeight.indexOf('px') > 0) {
                             	parentHeight = parseInt(parentHeight.substr(0, parentHeight.indexOf('px')));
                                 parentHeight -= border.y;
                                 parentHeight += 'px';
                             }    					
                             Dom.setStyle(bodyEl, 'height', parentHeight);
                             Dom.setStyle(bodyEl, 'width', parentWidth);
                        }
                    }
                });
                layout.on('resize', function(params) {
                  var wf = _this.wireframes[id];
                  var url = wf.callbackUrl;
                  _this.throttle.throttle(id, _this.throttleDelay, function() {
                    YAHOO.log('Save size-data for wireframe[' + id + ']', 'info', 'LayoutManager');
                    var jsonStr = YAHOO.lang.JSON.stringify(params.sizes);
                    if(wf.sizes != jsonStr) {
                      wf.sizes = jsonStr;
                      url += '&targetId=' + id;
                      url += '&sizes=' + jsonStr;
                      
                      //Don't use Wicket-ajax directly since you want to avoid the pre-call handlers
                      var t = wicketAjaxGetTransport();
                      t.open("GET", url, false);
                      t.send(null);
                    } else {
                      YAHOO.log('Size-data for wireframe[' + id + '] has not changed', 'info', 'LayoutManager');
                    }  
                  });
                });

                this.wireframes[id].layout = layout;
            },

            calculateSizeFromPercentage : function(percentage, parentSize) {
                if (!Lang.isNumber(percentage)) {
                    var idx = percentage.indexOf('%');
                    if (idx > -1) {
                        var abs = parseInt(percentage.substr(0, idx));
                        var parentSize = parseInt(parentSize.substr(0,
                                parentSize.indexOf('px')));
                        var x = parseInt((abs * parentSize) / 100);
                        return x;
                    }
                }
                return size;
            },
            
            getBorderWidth: function(el, type) {
            	var x = Dom.getStyle(el, type);
            	if(Lang.isUndefined(x) || Lang.isNull(x) || x.length<3) 
            		return 0;
    			x = x.substr(0, x.indexOf('px'));
    			//FF3 on Ubuntu thinks the border is something like 0.81236666 so we round it
    			return Math.round(x);
            },
            
            getBorder: function(el) {
            	var obj = { x: 0, y: 0 };
            	
            	var x = this.getBorderWidth(el, 'border-left-width');
            	x += this.getBorderWidth(el, 'border-right-width');
            	obj.x = x;

            	var y = this.getBorderWidth(el, 'border-top-width');
            	y += this.getBorderWidth(el, 'border-bottom-width');
            	obj.y = y;
            	return obj;
            }
        };

    })();

    YAHOO.hippo.LayoutManager = new YAHOO.hippo.LayoutManagerImpl();
    YAHOO.register("layoutmanager", YAHOO.hippo.LayoutManager, {
        version :"2.5.2",
        build :"1076"
    });
}

// PATCH FOR CLEANING UP RESOURCES

( function() {
    var Dom = YAHOO.util.Dom, Event = YAHOO.util.Event, Lang = YAHOO.lang;

    YAHOO.widget.Layout.prototype._setupElements = function() {
        this._doc = this.getElementsByClassName('yui-layout-doc')[0];
        if (!this._doc) {
            this._doc = document.createElement('div');
            this.get('element').appendChild(this._doc);
        }
        this._createUnits();
        this._setBodySize();
        Dom.addClass(this._doc, 'yui-layout-doc');
    };

    YAHOO.widget.Layout.prototype.destroy = function() {
        var par = this.get('parent');
        if (par) {
            par.removeListener('resize', this.resize, this, true);
        }
        Event.removeListener(window, 'resize', this.resize, this, true);

        this.unsubscribeAll();

        this._units = {};
        this._units.center = this._center;
        this._units.top = this._top;
        this._units.bottom = this._bottom;
        this._units.left = this._left;
        this._units.right = this._right;

        for ( var u in this._units) {
            if (Lang.hasOwnProperty(this._units, u)) {
                if (this._units[u]) {
                    this._units[u].destroy(true);
                }
            }
        }

        Event.purgeElement(this.get('element'));
        // this.get('parentNode').removeChild(this.get('element'));

        delete YAHOO.widget.Layout._instances[this.get('id')];
        // Brutal Object Destroy
        for ( var i in this) {
            if (Lang.hasOwnProperty(this, i)) {
                this[i] = null;
                delete this[i];
            }
        }
        if (par) {
            // par.resize();
        }

    };

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

        Event.purgeElement(this.get('element'));
        this.get('parentNode').removeChild(this.get('element'));

        delete YAHOO.widget.LayoutUnit._instances[this.get('id')];
        // Brutal Object Destroy
        for ( var i in this) {
            if (Lang.hasOwnProperty(this, i)) {
                this[i] = null;
                delete this[i];
            }
        }

        return par;
    };
})();
