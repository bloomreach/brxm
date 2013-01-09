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
 * Class that queues an array of init functions that get executed upon page load
 * or in Wicket's post ajax phase
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo
 * @module functionqueue
 */
( function() {
    var Lang = YAHOO.lang;

    YAHOO.namespace('hippo');

    YAHOO.hippo.FunctionQueue = function(_id) {
        this.id = _id;
        this.queue = [];
        this.uniques = [];
        this.preQueueHandler = null;
        this.postQueueHandler = null;

    };

    YAHOO.hippo.FunctionQueue.prototype = {

        handleQueue : function() {
            YAHOO.log('Handle function  queue[' + this.id + '] with size: '
                    + this.queue.length, 'info', 'FunctionQueue');
            if (Lang.isFunction(this.preQueueHandler)) {
                this.preQueueHandler.apply();
            }
            while (this.queue.length > 0) {
                this.queue.shift().apply();
            }
            this.uniques = [];

            if (Lang.isFunction(this.postQueueHandler)) {
                this.postQueueHandler.apply();
            }
        },

        registerFunction : function(func, uniqueId) {
            var i, len;

            YAHOO.log('Register function[' + uniqueId + '] in queue[' + this.id
                    + '] -> func=' + func, 'info', 'FunctionQueue');

            if (Lang.isFunction(func)) {
                if (!Lang.isUndefined(uniqueId) && !Lang.isNull(uniqueId)) {
                    for (i = 0, len = this.uniques.length; i < len; i++) {
                        if (this.uniques[i] === uniqueId) {
                            return;
                        }
                    }
                    this.uniques.push(uniqueId);
                }
                this.queue.push(func);
            }
        },

        toString : function() {
            return 'Function queue [' + this.id + ']';
        }
    };
}());
YAHOO.register("functionqueue", YAHOO.hippo.FunctionQueue, {
    version: "2.8.1", build: "19"
});
