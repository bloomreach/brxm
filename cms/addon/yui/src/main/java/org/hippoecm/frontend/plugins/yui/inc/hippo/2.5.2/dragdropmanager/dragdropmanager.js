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

YAHOO.namespace('hippo');

if (!YAHOO.hippo.DragDropManager) {

    /**
     * @description <p>Todo</p>
     * @namespace YAHOO.hippo
     * @requires dragdropmodel, functionqueue
     * @module drapdropmanager
     */
    YAHOO.hippo.DragDropManager = {
        loader :new YAHOO.hippo.FunctionQueue('DragDropQueue'),

        onLoad : function() {
            this.loader.handleQueue();
        },

        addDraggable : function(id, label, groups, callbackFunc, callbackUrl,
                callbackParameters) {
            var js = YAHOO.lang;
            var func = function() {

                var config = {
                    centerFrame :true,
                    resizeFrame :false
                }
                if (!js.isUndefined(label) && !js.isNull(label)) {
                    config.label = label;
                }

                var drag = null;
                // maybe isArray checks for null/undef but code is obfuscated
                if (!js.isUndefined(groups) && !js.isNull(groups)
                        && js.isArray(groups)) {
                    drag = new YAHOO.hippo.DDModel(id, groups.shift(), config);
                    while (groups.length > 0) {
                        drag.addToGroup(groups.shift());
                    }
                } else {
                    drag = new YAHOO.util.DDModel(id, null, config);
                }
                if (js.isUndefined(callbackParameters)
                        || js.isNull(callbackParameters)) {
                    callbackParameters = new Array();
                }

                drag.onDragDrop = function(ev, dropId) {
                    var targetId = {
                        key :'targetId',
                        value :dropId
                    };
                    callbackParameters.push(targetId);
                    for (i = 0; i < callbackParameters.length; i++) {
                        var paramKey = callbackParameters[i].key, paramValue = Wicket.Form
                                .encode(callbackParameters[i].value);
                        callbackUrl += (callbackUrl.indexOf('?') > -1) ? '&'
                                : '?';
                        callbackUrl += (paramKey + '=' + paramValue);
                    }
                    callbackFunc(callbackUrl);
                }
            };
            this.loader.registerFunction(func);
        },

        addDroppable : function(id, groups) {
            var js = YAHOO.lang;
            var drop = null;
            if (!js.isUndefined(groups) && !js.isNull(groups)
                    && js.isArray(groups)) {
                drop = new YAHOO.util.DDTarget(id, groups.shift());
                while (groups.length > 0) {
                    drop.addToGroup(groups.shift());
                }
            } else {
                drop = new YAHOO.util.DDTarget(id);
            }
        }
    };

    YAHOO.register("dragdropmanager", YAHOO.hippo.DragDropManager, {
        version :"2.5.2",
        build :"1076"
    });
}