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
 * @module dragdropmodel
 * @class a YAHOO.util.DDProxy extension
 * @requires dragdrop, hashmap
 * @extends YAHOO.util.DDProxy
 * @constructor
 * @param {String}
 *            id the id of the linked element
 */
YAHOO.namespace("hippo");

( function() {
    var Dom = YAHOO.util.Dom, Lang = YAHOO.lang;

    YAHOO.hippo.CallbackHelper = function(url, func, parameters) {
        this.init(url, func);
    };

    YAHOO.hippo.CallbackHelper.prototype = {
        callbackUrl :null,

        callbackFunction : function() {
        },

        callbackParameters :new YAHOO.hippo.HashMap(),

        init : function(url, func) {
            this.callbackUrl = url.replace(/\&amp;/g, '&');
            this.callbackFunction = func;
        },

        execute : function(overrideParameters) {
            if (overrideParameters != null) {
                this.callbackParameters.putAll(overrideParameters);
            }
            var url = this.callbackUrl;
            var entries = this.callbackParameters.entrySet();
            for ( var e in entries) {
                var entry = entries[e];
                url += (url.indexOf('?') > -1) ? '&' : '?';
                url += (entry.getKey() + '=' + Wicket.Form.encode(entry.getValue()));
            }
            this.callbackFunction(url);
        }
    };

    YAHOO.hippo.DDSharedBehavior = function() {
    };

    YAHOO.hippo.DDSharedBehavior.prototype = {
        initCommon : function(id, config) {
            this.label = Lang.isString(config.label) ? config.label : "";
            if (Lang.isArray(config.groups)) {
                while (config.groups.length > 0) {
                    this.addToGroup(config.groups.shift());
                }
            }
            this.cancelCallback = config.cancelCallback ? true : false;
            this.callback = new YAHOO.hippo.CallbackHelper(config.callbackUrl, config.callbackFunction,
                    config.callbackParameters);
        },

        getCallbackParameters : function(dropId) {
            var params = new YAHOO.hippo.HashMap();
            params.put('targetId', dropId);
            return params;
        },

        onDragDropAction : function(dropId) {
            if (this.cancelCallback)
                return;
            this.callback.execute(this.getCallbackParameters(dropId));
        }
    }

    YAHOO.hippo.DDBaseDragModel = function(id, sGroup, config) {
        YAHOO.hippo.DDBaseDragModel.superclass.constructor.apply(this, arguments);
        this.initPlayer(id, sGroup, config);
        this.initCommon(id, config);
    };

    YAHOO.extend(YAHOO.hippo.DDBaseDragModel, YAHOO.util.DDProxy, {

        TYPE :"DDBaseDragModel",

        initPlayer : function(id, sGroup, config) {
            if (!id) {
                return;
            }
            YAHOO.util.DDM.mode = YAHOO.util.DDM.POINT;

            this.initStyle();

            // specify that this is not currently a drop target
        this.isTarget = false;
        this.originalStyles = [];
        this.type = YAHOO.hippo.DDBaseDragModel.TYPE;
        this.startPos = YAHOO.util.Dom.getXY(this.getEl());

        this.useShim = true; // iframe magic
    },

    initStyle : function() {
        var el = this.getDragEl();
        YAHOO.util.Dom.setStyle(el, "borderColor", "transparent");
    },

    setStartStyle : function() {
    },

    setEndStyle : function() {
    },

    startDrag : function(x, y) {
        if (!this.startPos) {
            this.startPos = YAHOO.util.Dom.getXY(this.getEl());
        }
        YAHOO.log('Start drag[' + this.id + '] startpos: ' + this.startPos, "info", "DDBaseDragModel");
        this.setStartStyle();

        var targets = YAHOO.util.DDM.getRelated(this, true);

        for ( var i = 0; i < targets.length; i++) {
            var targetEl = this.getTargetDomRef(targets[i]);

            if (!this.originalStyles[targetEl.id]) {
                this.originalStyles[targetEl.id] = targetEl.className;
            }
            targetEl.className = targetEl.className + " drag-drop-target";
        }
    },

    getTargetDomRef : function(oDD) {
        if (oDD.resource) {
            return oDD.resource.getEl();
        } else {
            return oDD.getEl();
        }
    },

    endDrag : function(e) {
        YAHOO.log('End drag[' + this.id + ']', "info", "DDBaseDragModel");
        this.setEndStyle();
        this.resetTargets();
    },

    resetTargets : function() {
        // reset the target styles

        var targets = YAHOO.util.DDM.getRelated(this, true);
        for ( var i = 0; i < targets.length; i++) {
            var targetEl = this.getTargetDomRef(targets[i]);
            var oldStyle = this.originalStyles[targetEl.id];
            if (oldStyle || oldStyle == '') {
                targetEl.className = oldStyle;
            }
        }
    },

    onDragDrop : function(e, id) {
        this.onDragDropAction(id);
    }

    });

    Lang.augment(YAHOO.hippo.DDBaseDragModel, YAHOO.hippo.DDSharedBehavior);

    YAHOO.hippo.DDModel = function(id, sGroup, config) {
        YAHOO.hippo.DDModel.superclass.constructor.apply(this, arguments);
    };

    YAHOO.extend(YAHOO.hippo.DDModel, YAHOO.hippo.DDBaseDragModel, {

        TYPE :"DDModel",

        initStyle : function() {
            YAHOO.hippo.DDModel.superclass.initStyle.call(this);
            var el = this.getDragEl();
            YAHOO.util.Dom.setStyle(el, "opacity", 0.76);
        },

        setStartStyle : function() {
            var dragEl = this.getDragEl();
            dragEl.innerHTML = this.label;
            Dom.setStyle(dragEl, "color", Dom.getStyle(clickEl, "color"));

            var clickEl = this.getEl();
            Dom.setStyle(clickEl, "opacity", 0.4);
        },

        setEndStyle : function() {
            YAHOO.util.Dom.setStyle(this.getEl(), "opacity", 1);
        },

        /**
         * lookup drop model, getParameters, add to own and return;
         */
        getCallbackParameters : function(dropId) {
            var cp = YAHOO.hippo.DDImage.superclass.getCallbackParameters.call(this, dropId);
            var model = YAHOO.hippo.DragDropManager.getModel(dropId);
            if (Lang.isFunction(model.getCallbackParameters)) {
                cp.putAll(model.getCallbackParameters(dropId));
            }
            return cp;
        }

    });

    YAHOO.hippo.DDFallbackModel = function(id, group, config) {

        var clazz = config.wrappedModelClass;
        if (YAHOO.env.ua.ie > 0) {
            var original = Dom.get(id);

            var ids = [];
            var className = config.ieFallbackClass;
            var dds = Dom.getElementsByClassName(className, null, original, function(el) {
                if (el.id == '') {
                    Dom.generateId(el);
                }
                ids.push(el.id);
            });

            for ( var i = 0; i < ids.length; i++) {
                var el = Dom.get(ids[i]);
                var newEl = document.createElement("a");
                var parent = el.parentNode;
                newEl.appendChild(parent.removeChild(el));
                newEl.setAttribute("href", "#"); // this does the trick
                parent.appendChild(newEl);
                Dom.generateId(newEl);
                var x = new clazz(newEl.id, group, config);

                var ancestorTag = config.firstAncestorToBlur;
                if (ancestorTag != '') {
                    var opacity = function(_el, _val) {
                        Dom.setStyle(Dom.getAncestorByTagName(_el, ancestorTag), "opacity", _val);
                    }
                    x.setStartStyle = function() {
                        this.getDragEl().innerHTML = this.label;
                        opacity(this.getEl(), 0.4);
                    };

                    x.setEndStyle = function() {
                        opacity(this.getEl(), 1);
                    }
                }
            }
        } else {
            var x = new clazz(id, group, config)
        }
    };

    YAHOO.hippo.DDImage = function(id, sGroup, config) {
        YAHOO.hippo.DDImage.superclass.constructor.apply(this, arguments);
    };

    YAHOO
            .extend(
                    YAHOO.hippo.DDImage,
                    YAHOO.hippo.DDBaseDragModel,
                    {
                        TYPE :"DDImage",

                        initPlayer : function(id, sGroup, config) {
                            YAHOO.hippo.DDImage.superclass.initPlayer.call(this, id, sGroup, config);

                            this.currentGroup = config.currentGroup;
                        },

                        setStartStyle : function() {
                            var Dom = YAHOO.util.Dom;
                            var dragEl = this.getDragEl();
                            var clickEl = this.getEl();

                            dragEl.innerHTML = this.label;
                            dragEl.innerHTML = '<div style="padding-left:15px;padding-top:5px;width:120px;"><div><img src="' + clickEl.src + '" /></div></div>';

                            Dom.setStyle(dragEl, "color", Dom.getStyle(clickEl, "color"));
                            Dom.setStyle(clickEl, "opacity", 0.4);
                        },

                        setEndStyle : function() {
                            YAHOO.util.Dom.setStyle(this.getEl(), "opacity", 1);
                        },

                        /**
                         * lookup drop model, getParameters, add to own and
                         * return;
                         */
                        getCallbackParameters : function(dropId) {
                            var cp = YAHOO.hippo.DDImage.superclass.getCallbackParameters.call(this, dropId);
                            var model = YAHOO.hippo.DragDropManager.getModel(dropId);
                            if (Lang.isFunction(model.getCallbackParameters)) {
                                cp.putAll(model.getCallbackParameters(dropId));
                            }
                            return cp;
                        }

                    });

    YAHOO.hippo.DDBaseDropModel = function(id, sGroup, config) {
        YAHOO.hippo.DDBaseDropModel.superclass.constructor.apply(this, arguments);
        this.initCommon(id, config);
    };

    YAHOO.extend(YAHOO.hippo.DDBaseDropModel, YAHOO.util.DDTarget, {
        TYPE :"DDBaseDropModel"
    });

    Lang.augment(YAHOO.hippo.DDBaseDropModel, YAHOO.hippo.DDSharedBehavior);

})();
