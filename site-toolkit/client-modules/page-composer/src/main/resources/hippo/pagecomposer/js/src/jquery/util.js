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
    init: function() {
    },

    inside : function (source, el, thresHigh, thresLow, min){
        thresHigh = thresHigh || 0.5;
        thresLow = thresLow || 0.2;
        min = min || 4;

        var srcOffset = source.offset();
        var srcWidth  = source.width();
        var srcHeight = source.height();

        var elWidth = srcWidth;
        var elHeight = srcHeight;

        if(elWidth > elHeight) {
            elWidth -= (srcWidth * thresLow);
            elHeight -= (srcHeight * thresHigh);
            elHeight = elHeight > min ? min : elHeight;
        } else if(elHeight > elWidth) {
            elWidth -= (srcWidth * thresHigh);
            elHeight -= (srcHeight * thresLow);
            elWidth = elWidth > min ? min : elWidth;
        } else {
            elWidth -= (srcWidth * thresHigh);
            elHeight -= (srcHeight * thresHigh);
        }

        el.width(elWidth);
        el.height(elHeight);
        var elLeft = srcOffset.left + ((srcWidth-elWidth)/2);
        var elTop = srcOffset.top + ((srcHeight-elHeight)/2);
        el.offset({
            left: elLeft,
            top : elTop
        });
    },

    beneath : function(source, el, thresHigh, thresLow, min) {
        thresHigh = thresHigh || 0.5;
        thresLow = thresLow || 0.15;
        min = min || 4;

        var srcWidth  = source.outerWidth();
        var srcHeight = source.outerHeight();

        var elWidth = srcWidth - (srcWidth * thresLow);
        var elHeight = srcHeight - (srcHeight * thresHigh);
        elHeight = elHeight > min ? min : elHeight;

        el.width(elWidth);
        el.height(elHeight);

        var elLeft = 0, elTop = 0;
        var srcPosition = source.position();
        //TODO: add better test for relative
        if(srcPosition.left == 0) {
            var parent = source.parent();
            var pPos = parent.position();
            var x = 0, y = 0;
            var prev = source.prev();
            while(prev.length > 0) {
                y += prev.height();
                prev = prev.prev();
            }
            elLeft = pPos.left;
            elTop = pPos.top + y + elHeight;
        } else {
            elLeft = srcPosition.left + ((srcWidth-elWidth)/2);
            elTop = srcPosition.top + srcHeight + 1;
        }

        el.offset({
            left: elLeft,
            top : elTop
        });
    },

    above: function(source, el, thresHigh, thresLow, min) {
        thresHigh = thresHigh || 0.5;
        thresLow = thresLow || 0.15;
        min = min || 4;

        var srcWidth  = source.outerWidth();
        var srcHeight = source.outerHeight();

        var elWidth = srcWidth - (srcWidth * thresLow);
        var elHeight = srcHeight - (srcHeight * thresHigh);
        elHeight = elHeight > min ? min : elHeight;

        el.width(elWidth);
        el.height(elHeight);

        var elLeft = 0, elTop = 0;
        var srcPosition = source.position();
        elLeft = srcPosition.left + ((srcWidth-elWidth)/2);
        elTop = srcPosition.top - 1;

        el.offset({
            left: elLeft,
            top : elTop
        });
    },

    between : function(prev, next, el, thresHigh, thresLow, min) {
        thresHigh = thresHigh || 0.5;
        thresLow = thresLow || 0.15;
        min = min || 4;

        var srcWidth  = prev.outerWidth();
        var srcHeight = prev.outerHeight();

        var elWidth = srcWidth - (srcWidth * thresLow);
        var elHeight = srcHeight - (srcHeight * thresHigh);
        elHeight = elHeight > min ? min : elHeight;

        el.width(elWidth);
        el.height(elHeight);

        var elLeft = 0, elTop = 0;
        var prevPosition = prev.position();
        var nextPosition = next.position();
        elLeft = prevPosition.left + ((srcWidth-elWidth)/2);
        
        var half = (nextPosition.top - (prevPosition.top + srcHeight))/2; 
        elTop = nextPosition.top  - half;

        el.offset({
            left: elLeft,
            top : elTop
        });
    }
});