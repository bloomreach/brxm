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
            var url, entries, e, entry;
            if (overrideParameters !== null && overrideParameters !== undefined) {
                this.callbackParameters.putAll(overrideParameters);
            }
            url = this.callbackUrl;
            entries = this.callbackParameters.entrySet();
            for (e in entries) {
                if (entries.hasOwnProperty(e)) {
                    entry = entries[e];
                    url += (url.indexOf('?') > -1) ? '&' : '?';
                    url += (entry.getKey() + '=' + Wicket.Form.encode(entry.getValue()));
                }
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
            if (this.cancelCallback !== true) {
                this.callback.execute(this.getCallbackParameters(dropId));
            }
        }
    };

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
        var targets, i, len, targetEl;

        if (!this.startPos) {
            this.startPos = YAHOO.util.Dom.getXY(this.getEl());
        }
        this.setStartStyle();

        targets = YAHOO.util.DDM.getRelated(this, true);

        for (i = 0, len = targets.length; i < len; i++) {
            targetEl = this.getTargetDomRef(targets[i]);

            if (!this.originalStyles[targetEl.id]) {
                this.originalStyles[targetEl.id] = targetEl.className;
            }
            targetEl.className = targetEl.className + " drag-drop-target";
        }
    },

    getTargetDomRef : function(oDD) {
        var result;
        if (oDD.resource) {
            result = oDD.resource.getEl();
        } else {
            result = oDD.getEl();
        }
        return result;
    },

    endDrag : function(e) {
        this.setEndStyle();
        this.resetTargets();
    },

    resetTargets : function() {
        // reset the target styles
        var targets, i, len, targetEl, oldStyle;

        targets = YAHOO.util.DDM.getRelated(this, true);

        for (i = 0, len = targets.length; i < len; i++) {
            targetEl = this.getTargetDomRef(targets[i]);
            oldStyle = this.originalStyles[targetEl.id];
            if (oldStyle || oldStyle === '') {
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
            var dragEl = this.getDragEl(),
                clickEl = this.getEl();

            dragEl.innerHTML = this.label;
            Dom.setStyle(dragEl, "color", Dom.getStyle(clickEl, "color"));
            Dom.setStyle(clickEl, "opacity", 0.4);
        },

        setEndStyle : function() {
            YAHOO.util.Dom.setStyle(this.getEl(), "opacity", 1);
        },

        /**
         * lookup drop model, getParameters, add to own and return;
         */
        getCallbackParameters : function(dropId) {
            var cp = YAHOO.hippo.DDImage.superclass.getCallbackParameters.call(this, dropId),
                model = YAHOO.hippo.DragDropManager.getModel(dropId);
            if (Lang.isFunction(model.getCallbackParameters)) {
                cp.putAll(model.getCallbackParameters(dropId));
            }
            return cp;
        }

    });

    YAHOO.hippo.DDFallbackModel = function(id, group, config) {
        var Clazz, original, ids, className, i, len, el, newEl, parent, x, ancestorTag;

        function opacity(_el, _val) {
            Dom.setStyle(Dom.getAncestorByTagName(_el, ancestorTag), "opacity", _val);
        }

        function setStartStyle() {
            this.getDragEl().innerHTML = this.label;
            opacity(this.getEl(), 0.4);
        }

        function setEndStyle() {
            opacity(this.getEl(), 1);
        }

        Clazz = config.wrappedModelClass;

        if (YAHOO.env.ua.ie > 0) {
            original = Dom.get(id);

            ids = [];
            className = config.ieFallbackClass;

            Dom.getElementsByClassName(className, null, original, function(el) {
                if (el.id === '') {
                    Dom.generateId(el);
                }
                ids.push(el.id);
            });

            ancestorTag = config.firstAncestorToBlur;

            for (i = 0, len = ids.length; i < len; i++) {
                el = Dom.get(ids[i]);
                newEl = document.createElement("a");
                parent = el.parentNode;
                newEl.appendChild(parent.removeChild(el));
                newEl.setAttribute("href", "#"); // this does the trick
                parent.appendChild(newEl);
                Dom.generateId(newEl);
                x = new Clazz(newEl.id, group, config);

                if (ancestorTag !== '') {
                    x.setStartStyle = setStartStyle;
                    x.setEndStyle = setEndStyle;
                }
            }
        } else {
            x = new Clazz(id, group, config);
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
                            var Dom = YAHOO.util.Dom,
                                dragEl = this.getDragEl(),
                                clickEl = this.getEl();

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
                            var cp = YAHOO.hippo.DDImage.superclass.getCallbackParameters.call(this, dropId),
                                model = YAHOO.hippo.DragDropManager.getModel(dropId);
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

}());
