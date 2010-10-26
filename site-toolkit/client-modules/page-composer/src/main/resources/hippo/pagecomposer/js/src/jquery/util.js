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
    }
});