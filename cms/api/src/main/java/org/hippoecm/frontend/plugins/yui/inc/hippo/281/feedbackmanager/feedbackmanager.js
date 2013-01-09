/*
 * Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
 * Provides a singleton manager for feedback panels
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom, event, container, hashmap, hippoajax
 * @module feedbackmanager
 * @beta
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.FeedbackManager) {
    (function () {
        var Dom = YAHOO.util.Dom;

        YAHOO.hippo.FeedbackPanel = function (id, config) {
            this.id = id;
            this.config = config;
            this.module = null;

            YAHOO.hippo.HippoAjax.registerDestroyFunction(Dom.get(this.id), this.cleanup, this);
        };

        YAHOO.hippo.FeedbackPanel.prototype = {
            show : function () {
                if (this.module === null || this.module === undefined) {
                    this.module = new YAHOO.widget.Module(this.id, this.config);
                }
                var element = Dom.get(this.id);
                this.module.render(element.parentNode);
                YAHOO.util.Event.addListener(this.id, "click", this.hide, this, true);
                this.module.show();
            },

            hide : function () {
                if (this.module !== null && this.module !== undefined) {
                    this.module.hide();
                }
            },

            cleanup : function () {
                if (this.module !== null && this.module !== undefined) {
                    this.module.destroy();
                    this.module = null;
                }
            }
        };

        YAHOO.hippo.FeedbackManagerImpl = function () {
        };

        YAHOO.hippo.FeedbackManagerImpl.prototype = {
            instances : new YAHOO.hippo.HashMap(),

            create : function (id, config) {
                this._cleanup();

                if (this.instances.containsKey(id)) {
                    YAHOO.log("Feedback panel [" + id + "] was already registered", "warn", "FeedbackManager");
                    return;
                }

                YAHOO.log("Creating feedback panel [" + id + "]", "info", "FeedbackManager");
                this.instances.put(id, new YAHOO.hippo.FeedbackPanel(id, config));
            },

            get : function (id) {
                this._cleanup();
                return this.instances.get(id);
            },

            delayedHide : function (id, delay) {
                var panel = this.instances.get(id);
                panel.show();
                YAHOO.lang.later(delay, panel, 'hide');
            },

            _cleanup : function () {
                var ids, toRemove, i, len, id, domId, panel;
                ids = this.instances.keySet();
                toRemove = [];
                for (i = 0, len = ids.length; i < len; i++) {
                    id = ids[i];
                    domId = Dom.get(id);
                    if (domId === null || domId === undefined) {
                        toRemove.push(id);
                    }
                }
                for (i = 0, len = toRemove.length; i < len; i++) {
                    panel = this.instances.remove(toRemove[i]);
                    panel.cleanup();
                }
            }
        };

    }());

    YAHOO.hippo.FeedbackManager = new YAHOO.hippo.FeedbackManagerImpl();
    YAHOO.register("feedbackmanager", YAHOO.hippo.FeedbackManager, {
        version : "2.8.1", build : "19"
    });
}