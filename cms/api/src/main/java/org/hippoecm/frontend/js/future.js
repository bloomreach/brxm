/*
 *  Copyright 2012 Hippo.
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
"use strict";

if (typeof Hippo === 'undefined') {
    Hippo = {};
}

if (typeof Hippo.Future === 'undefined') {
    Hippo.Future = function(func) {
        this.success = false;
        this.completed = false;
        this.value = undefined;

        this.successHandlers = [];
        this.failureHandlers = [];

        var self = this;
        func.call(this, function(value) {
            self.onSuccess.call(self, value)
        }, function(value) {
            self.onFailure.call(self, value)
        });
    };

    Hippo.Future.prototype = {

        when: function(cb) {
            if (!this.completed) {
                this.successHandlers.push(cb);
            } else if (this.success) {
                cb.call(this, this.value);
            }
            return this;
        },

        otherwise: function(cb) {
            if (!this.completed) {
                this.failureHandlers.push(cb);
            } else if (!this.success) {
                cb.call(this);
            }
            return this;
        },

        get: function() {
            if (!this.completed) {
                throw "Future has not completed yet";
            }
            return this.value;
        },

        onSuccess: function(value) {
            if (this.completed) {
                return;
            }
            this.value = value;
            this.success = true;
            this.completed = true;
            for (var i = 0; i < this.successHandlers.length; i++) {
                this.successHandlers[i].call(this, value);
            }
            this.cleanup();
        },

        onFailure: function(value) {
            if (this.completed) {
                return;
            }
            this.value = value;
            this.success = false;
            this.completed = true;
            for (var i = 0; i < this.failureHandlers.length; i++) {
                this.failureHandlers[i].call(this, value);
            }
            this.cleanup();
        },

        cleanup: function() {
            delete this.successHandlers;
            delete this.failureHandlers;
        }

    };

    Hippo.Future.join = function() {
        var futures;
        if (arguments.length == 1) {
            futures = arguments[0];
        } else {
            futures = Array.prototype.slice.call(arguments);
        }

        var value = null;
        var join = new Hippo.Future(function(onSuccess, onFailure) {
            var completed = false;
            var togo = futures.length;
            var failureHandler = function() {
                togo--;
                if (completed) {
                    return;
                }
                completed = true;
                onFailure.call(this);
            };
            for (var i = 0; i < futures.length; i++) {
                futures[i].when(function(result) {
                    togo--;
                    if (!completed && togo === 0) {
                        completed = true;
                        onSuccess.call(this, value);
                    }
                }).otherwise(failureHandler);
            }
        });
        join.set = function(val) {
            value = val;
        };
        return join;
    };
}