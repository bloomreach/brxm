/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
(function($) {
    "use strict";

    $.namespace('Hippo.ChannelManager.TemplateComposer.IFrame');

    var Main = (function() {
        var subscriptions = {};
        return {
            exception: function(msg) {
                sendMessage({msg: msg}, "iframeexception");
            },

            publish: function(topic) {
               var i, len, subscription;
               if (typeof subscriptions[topic] === 'undefined') {
                    return true;
               }
               console.log('publishing to topic '+topic + ', ' + subscriptions[topic].length + ' subscriptions');

               for (i = 0, len = subscriptions[topic].length; i < len; i++) {
                   subscription = subscriptions[topic][i];
                   if (subscription.callback.apply(subscription.scope, Array.prototype.slice.call(arguments, 1)) === false) {
                       return false;
                   }
               }
               return true;
            },

            subscribe: function(topic, callback, scope) {
               var scopeParameter = scope || window;
               if (typeof subscriptions[topic] === 'undefined') {
                   subscriptions[topic] = [];
               }
               subscriptions[topic].push({callback: callback, scope: scopeParameter});
            },

            unSubscribe: function(topic, callback, scope) {
               var scopeParameter, i, len, subscription;
               if (typeof subscriptions[topic] === 'undefined') {
                   return false;
               }
               scopeParameter = scope || window;
               for (i=0, len = subscriptions[topic].length; i < len; i++) {
                   subscription = subscriptions[topic][i];
                   if (subscription.callback === callback && subscription.scope === scopeParameter) {
                       subscriptions[topic].splice(i, 1);
                       return true;
                   }
               }
               return false;
            }
        };
    }());

    onhostmessage(function(msg) {
        Main.publish('initialize', msg.data);
    }, this, false, 'init');

    Hippo.ChannelManager.TemplateComposer.IFrame.Main = Main;

}(jQuery));