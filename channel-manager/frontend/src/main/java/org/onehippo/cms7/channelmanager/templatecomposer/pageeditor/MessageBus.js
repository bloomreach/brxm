/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function () {
    "use strict";

    Ext.namespace('Hippo.ChannelManager.TemplateComposer');

    Hippo.ChannelManager.TemplateComposer.createMessageBus = function(name) {
        var subscriptions = {};
        return {
            exception: function(msg) {
                this.publish('exception', msg);
            },

            publish: function(topic) {
                var i, len, subscription;
                if (subscriptions[topic] === undefined) {
                    return true;
                }
                len = subscriptions[topic].length;
                console.log(name + "[" + len + "] " + topic);
                for (i = 0; i < len; i++) {
                    subscription = subscriptions[topic][i];
                    if (subscription.callback.apply(subscription.scope, Array.prototype.slice.call(arguments, 1)) === false) {
                        return false;
                    }
                }
                return true;
            },

            subscribe: function(topic, callback, scope) {
                var scopeParameter = scope || window;
                if (subscriptions[topic] === undefined) {
                    subscriptions[topic] = [];
                }
                subscriptions[topic].push({callback: callback, scope: scopeParameter});
            },

            unsubscribe: function(topic, callback, scope) {
                var scopeParameter, i, len, subscription;
                if (subscriptions[topic] === undefined) {
                    return false;
                }
                scopeParameter = scope || window;
                for (i = 0, len = subscriptions[topic].length; i < len; i++) {
                    subscription = subscriptions[topic][i];
                    if (subscription.callback === callback && subscription.scope === scopeParameter) {
                        subscriptions[topic].splice(i, 1);
                        return true;
                    }
                }
                return false;
            },

            unsubscribeAll: function() {
                subscriptions = {};
            }

        };
    };

}());
