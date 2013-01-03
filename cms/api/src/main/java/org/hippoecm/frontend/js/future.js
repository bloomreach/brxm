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
(function() {
    "use strict";

    if (Hippo === undefined) {
        Hippo = {};
    }

    if (Hippo.Future !== undefined) {
        return;
    }

    var Future = function(func) {
        this.success = false;
        this.completed = false;
        this.value = undefined;

        this.successHandlers = [];
        this.failureHandlers = [];

        var self = this;
        func.call(this, function(value) {
            self.onSuccess.call(self, value);
        }, function(value) {
            self.onFailure.call(self, value);
        });
    };

    Future.prototype = {

        /**
         * Register a callback to be invoked when the future completes successfully.
         * If the future has already completed and was successful, the callback is invoked immediately.
         * Returns this, so this method can be used in a chain.
         *
         * @param cb
         * @return {*}
         */
        when: function(cb) {
            if (!cb) {
                throw new TypeError('callback is undefined');
            }
            if (!this.completed) {
                this.successHandlers.push(cb);
            } else if (this.success) {
                cb.call(this, this.value);
            }
            return this;
        },

        /**
         * Register a callback to be invoked when the future does NOT complete successfully.
         * If the future has already completed and was not successful, the callback is invoked immediately.
         * Returns this, so this method can be used in a chain.
         *
         * @param cb
         * @return {*}
         */
        otherwise: function(cb) {
            if (!this.completed) {
                this.failureHandlers.push(cb);
            } else if (!this.success) {
                cb.call(this);
            }
            return this;
        },

        /**
         * Transform the result of the future.  A transformer can manipulate the value that will be received
         * by callbacks.  A new future will be returned.
         * <p>
         *     Example:
         * <code>
         * future.transform(function(value) {
         *   return 'Hello ' + value;
         * }).when(function(result) {
         *   console.log(result);
         * });
         * </code>
         *     will log 'Hello Alice' to the console if this future completes with value 'Alice'.
         * <p>
         *     If the transformer throws an exception, the failure callbacks will be invoked.
         *
         * @param transformer
         * @return {Hippo.Future}
         */
        transform: function(transformer) {
            return new Hippo.Future(function(onSuccess, onFail) {
                this.when(function(value) {
                    var transformed;
                    try {
                        transformed = transformer(value);
                    } catch (e) {
                        onFail();
                        return;
                    }
                    onSuccess(transformed);
                }).otherwise(onFail);
            }.bind(this));
        },

        /**
         * Chain a future that depends on this future.  The future that is returned by this method will
         * complete when the future, created by the factory, completes.  The factory creates a future with
         * the result of this future.
         * <p>
         *     Example:
         * <code>
         * future.next(function(value) {
         *   return new Future(function(onSuccess, onFail) {
         *     onSuccess('Hello ' + value);
         *   });
         * }).when(function(result) {
         *   console.log(result);
         * });
         * </code>
         *     will log 'Hello Alice' to the console when this future completes with value 'Alice'.
         *
         * @param futureFactory
         * @return {Hippo.Future}
         */
        chain: function(futureFactory) {
            return new Hippo.Future(function(onSuccess, onFail) {
                this.when(function(value) {
                    var future = futureFactory(value);
                    future.when(onSuccess).otherwise(onFail);
                }).otherwise(onFail);
            }.bind(this));
        },

        /**
         * Chain a future when this future fails.  This allows recovery with futures to be transparent.
         * <p>
         *     Example:
         * <code>
         * future.retry(function() {
         *     return Future.constant('Hello Bob');
         * }).when(function(result) {
         *     console.log(result);
         * });
         * </code>
         *     will log 'Hello Bob' to the console when this future fails.
         *
         * @param futureFactory
         * @return {Hippo.Future}
         */
        retry: function(futureFactory) {
            return new Hippo.Future(function(onSuccess, onFail) {
                this.when(onSuccess)
                    .otherwise(function() {
                        var future = futureFactory();
                        future.when(onSuccess).otherwise(onFail);
                    });
            }.bind(this));
        },

        /**
         * Returns the value of the future.  Can only be invoked after the future completed successfully.
         *
         * @return {*}
         */
        get: function() {
            if (!this.completed) {
                throw "Future has not completed yet";
            }
            if (!this.success) {
                throw "Future completed unsuccessfully";
            }
            return this.value;
        },

        onSuccess: function(value) {
            var i, len;
            if (this.completed) {
                return;
            }
            this.value = value;
            this.success = true;
            this.completed = true;
            for (i = 0, len = this.successHandlers.length; i < len; i++) {
                this.successHandlers[i].call(this, value);
            }
            this.cleanup();
        },

        onFailure: function(value) {
            var i, len;
            if (this.completed) {
                return;
            }
            this.value = value;
            this.success = false;
            this.completed = true;
            for (i = 0, len = this.failureHandlers.length; i < len; i++) {
                this.failureHandlers[i].call(this, value);
            }
            this.cleanup();
        },

        cleanup : function() {
            delete this.successHandlers;
            delete this.failureHandlers;
        }

    };

    /**
     * Future constructor.  Only exposes public methods.
     *
     * @param func
     * @constructor
     */
    Hippo.Future = function(func) {
        var _future = new Future(func),
            publicMembers = [ 'when', 'otherwise', 'transform', 'chain', 'retry', 'get' ],
            i, len;
        // use a for-loop to be compatible with IE8
        for (i = 0, len = publicMembers.length; i < len; i++) {
            this[publicMembers[i]] = _future[publicMembers[i]].bind(_future);
        }
    };

    /**
     * FAIL constant
     *
     * @type {Hippo.Future}
     */
    Hippo.Future.FAIL = new Hippo.Future(function (onSuccess, onFail) {
        onFail();
    });

    /**
     * Constant future whose value is already known.
     *
     * @param value
     * @return {Hippo.Future}
     */
    Hippo.Future.constant = function(value) {
        return new Hippo.Future(function (onSuccess, onFail) {
            onSuccess(value);
        });
    };

    /**
     * Create a future whose success depends on a number of other futures.  Only if those other futures
     * all complete successfully will the returned future also complete, successfully.  If any of the supplied
     * futures fails, the returned future also fails.
     *
     * @return {Hippo.Future}
     */
    Hippo.Future.join = function() {
        var futures, value, join;

        if (arguments.length === 1) {
            futures = arguments[0];
        } else {
            futures = Array.prototype.slice.call(arguments);
        }

        value = null;
        join = new Hippo.Future(function (onSuccess, onFailure) {
            var togo, completed, successHandler, failureHandler, i, len;

            togo = futures.length;

            // early exit if there are no actual futures to wait for
            if (togo === 0) {
                onSuccess.call(this, value);
                return;
            }

            completed = false;
            successHandler = function() {
                togo--;
                if (!completed && togo === 0) {
                    completed = true;
                    onSuccess.call(this, value);
                }
            };
            failureHandler = function() {
                togo--;
                if (completed) {
                    return;
                }
                completed = true;
                onFailure.call(this);
            };

            for (i = 0, len = futures.length; i < len; i++) {
                futures[i].when(successHandler).otherwise(failureHandler);
            }
        });
        join.set = function(val) {
            value = val;
        };
        return join;
    };

    /**
     * Create a future that will create an associative map of the results.  Based on a map of (key => future) pairs,
     * it will complete with a map of values (key => value) when all futures have completed.
     * <p>
     * Note that this future will always complete successfully, even if the supplied futures fail.  The associated
     * values will be undefined.
     *
     * @param map
     * @return {Hippo.Future}
     */
    Hippo.Future.map = function(map) {
        return new Hippo.Future(function(onSuccess, onFail) {
            var value = {}, keys = Object.keys(map), togo = keys.length, completed = false;

            if (togo === 0) {
                onSuccess.call(this, value);
                return;
            }

            function done() {
                togo--;
                if (!completed && togo === 0) {
                    completed = true;
                    onSuccess.call(this, value);
                }
            }

            keys.forEach(function(key) {
                var future = map[key];
                future.when(function (val) {
                    value[key] = val;
                    done();
                }).otherwise(function () {
                    value[key] = undefined;
                    done();
                });
            });
        });
    };


}());