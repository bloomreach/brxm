/*
 *  Copyright 2010 Hippo.
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

jQuery.noConflict();
(function($) {

    $.namespace('Hippo.Util');

    Hippo.Util.Map = Class.extend({
        init : function() {
            this.keys = [];
            this.values = {};
        },

        put : function(key, value) {
            this.keys.push(key);
            this.values[key] = value;
        },

        get : function(key) {
            if (this.containsKey(key)) {
                return this.values[key];
            }
            return null;
        },

        containsKey : function(key) {
            return $.inArray(key, this.keys) > -1;
        },

        remove : function(key) {
            var idx = $.inArray(key, this.keys);
            if (idx > -1) {
                this.keys.removeByIndex(idx);
                var v = this.values[key];
                delete this.values[key];
                return v;
            } else {
                throw new Error('Remove failed: No entry found for key ' + key)
            }
        },

        each: function(f, scope) {
            scope = scope || this;
            var len = this.keys.length;
            for (var i = 0; i < len; ++i) {
                var key = this.keys[i];
                f.apply(scope, [key, this.values[key]]);
            }
        },

        keySet : function() {
            var keys = [this.keys.length], len = this.keys.length;
            for(var i=0; i<len; ++i) {
                keys[i] = this.keys[i];
            }
            return keys;
        },

        size : function() {
            return this.keys.length;
        },

        clear : function() {
            this.keys = [];
            this.values = {};
        }

    });

    Hippo.Util.OrderedMap = Hippo.Util.Map.extend({
        put : function(key, value, index) {
            if (typeof index === 'undefined') {
                this._super(key, value);
            } else {
                this.keys.splice(index, 0, key);
                this.values[key] = value;
            }
        },

        updateOrder : function (order) {
            var old = this.keys;
            this.keys = order;
            return order != old;
        }
    });

    Hippo.Util.Draw = Class.extend({
        VERTICAL : 'v',
        HORIZONTAL : 'h',

        init: function(cfg) {
            this.thresHigh = cfg.tresholdHigh || 0.8;
            this.thresLow = cfg.tresholdLow || 0.2;
            this.min = cfg.min || 0;
            this.beforeOffset = cfg.beforeOffset || 2;
            this.afterOffset = cfg.afterOffset || 2;

            this.cache = {};
        },

        _draw : function(type, source, el, pos, thresHigh, thresLow, min, orientation) {
            var key = type + source[0].id;
            var o = this.cache[key];
            if(typeof o === 'undefined') {
                thresHigh = thresHigh || this.thresHigh;
                thresLow = thresLow || this.thresLow;
                min = min || this.min;

                var src = {
                    offset : source.offset(),
                    width  : source.outerWidth(),
                    height : source.outerHeight()
                };
                var ind = {
                    width  : src.width,
                    height : src.height,
                    orientation: orientation || src.height > src.width ? this.HORIZONTAL : this.VERTICAL,
                    left: src.offset.left,
                    top: src.offset.top
                };

                if(ind.orientation == this.VERTICAL) {
                    ind.width -= (ind.width  * thresLow);
                    ind.height = min > 0 ? min : ind.height - (ind.height * thresHigh);
                } else {
                    ind.height -= (ind.height * thresLow);
                    ind.width = min > 0 ? min : ind.width - (ind.width * thresHigh);
                }
                o = {
                    src : src,
                    ind : ind
                };
                pos.call(this, o);
                this.cache[key] = o;
            }

            el.width(o.ind.width);
            el.height(o.ind.height);
            el.offset({
                left: o.ind.left,
                top : o.ind.top
            });

        },

        inside : function (source, el, thresHigh, thresLow, min, orientation){
            this._draw('inside', source, el, function(data) {
                if(data.ind.orientation == this.VERTICAL) {
                    data.ind.left += (data.src.width-data.ind.width) / 2;
                    data.ind.top  += (data.src.height-data.ind.height) / 2;
                }
            }, thresHigh, thresLow, min, orientation);
        },

        after : function(source, el, thresHigh, thresLow, min, orientation) {
            this._draw('after', source, el, function(data) {
                if(data.ind.orientation == this.VERTICAL) {
                    data.ind.left += (data.src.width-data.ind.width) / 2;
                    data.ind.top += data.src.height + this.afterOffset;
                }
            }, thresHigh, thresLow, min, orientation);
        },

        before : function(source, el, thresHigh, thresLow, min, orientation) {
            this._draw('before', source, el, function(data) {
                if(data.ind.orientation == this.VERTICAL) {
                    data.ind.left += (data.src.width - data.ind.width) / 2;
                    data.ind.top -= this.beforeOffset;
                }
            }, thresHigh, thresLow, min, orientation);
        },

        between : function(prev, next, el, thresHigh, thresLow, min, orientation) {
            //take prev as source
            this._draw('between', prev, el, function(data) {
                //TODO: maybe use offset instead
                var nextPosition = next.position();

                if(data.ind.orientation == this.VERTICAL) {
                    data.ind.left += (data.src.width - data.ind.width) / 2;
                    var bottom = data.ind.top + data.src.height;
                    data.ind.top = bottom + ((nextPosition.top - bottom) / 2) - (data.ind.height/2);
                }

            }, thresHigh, thresLow, min, orientation);
        },

        reset : function() {
            this.cache = {};
        }
    });

})(jQuery);


//Copied from http://ejohn.org/blog/javascript-array-remove/
//Adds removeByIndex to array
Array.prototype.removeByIndex= function(from, to) {
    var rest = this.slice((to || from) + 1 || this.length);
    this.length = from < 0 ? this.length + from : from;
    return this.push.apply(this, rest);
};

