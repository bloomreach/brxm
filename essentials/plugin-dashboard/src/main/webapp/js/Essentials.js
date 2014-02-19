'use strict';
(function (Essentials, undefined) {
    // {"values":{"entry":[{"key":"foo2","value":"bar"},{"key":"foo","value":"bar"}]}}

    //############################################
    // UTILS
    //############################################

    var Map = function () {
        var ref = this.items = [];
        this.put = function (key, value) {
            //ref.push({"key":key,"value":value});
            ref.push({"key":key,"value":value});
            return this;
        };

    };
    var Query = function(){
        var ref = this;
        this.forQuery = function (query) {
            ref.query = query;
            return this;
        };
        this.forPageSize = function (pageSize) {
            ref.pageSize = pageSize;
            return this;
        };
        this.forPage = function (page) {
            ref.page = page;
            return this;
        };
        this.ofType = function (type) {
            ref.type = type;
            return this;
        };

    };


    /**
     * Creates a payload objects if not created already and adds key/value to dictionary (map)
     * @param key
     * @param value
     * @param payload optional, may be null
     * @returns
     */
    Essentials.addPayloadData = function (key, value, payload) {
        if (payload === undefined || payload == null) {
            payload = {"values":{}};

        }
        payload.values[key] = value;
        return payload;
    };

    Essentials.queryBuilder = function (q) {
        var query = new Query();
        query.forQuery(q);
        return query;
    };

    Essentials.mapBuilder = function () {
        return new Map();
    };


    /**
     * Create a collection for easier lookup. e.g.:
     * <code>if(x in collection(1,2,3,4){doSomething()};</code>
     * <code>if(x in collection([1,2,3,4]){doSomething()};</code>
     * @param optional optional parameter  (type of array)
     * @returns {{}}
     */
    Essentials.collection = function (optional) {
        var obj = {}, arr = arguments;
        if (optional instanceof Array) {
            arr = optional;
        }
        for (var i = 0; i < arr.length; i++) {
            obj[arr[i]] = true;
        }
        return obj;
    };


    /**
     * given two arrays, get same values
     * @param array1
     * @param array2
     * @returns {Array}
     */
    Essentials.intersect = function (array1, array2) {
        var result = [];
        var c = Essentials.collection(array2);
        for (var i = 0; i < array1.length; i++) {
            if (array1[i] in c) {
                result.push(array1[i]);
            }
        }
        return result;
    };

    /**
     *  given two arrays, get elements that are in second one but not in first one
     * @param array1
     * @param array2
     * @returns {Array}
     */
    Essentials.complement = function (array1, array2) {
        var result = [];
        var c = Essentials.collection(array2);
        for (var i = 0; i < array1.length; i++) {
            if (!(array1[i] in c)) {
                result.push(array1[i]);
            }
        }
        return result;
    };

    Essentials.isEmpty = function (str) {
        return typeof str === "undefined" || str == null || str.trim().length == 0;
    };

    Essentials.asArray = function (obj) {
        var array = [];
        if (typeof obj === "undefined" || obj == null) {
            return array;
        }
        if (typeof obj === "object") {
            array.push(obj);
            return array;
        }
        return array;
    };


})(window.Essentials = window.Essentials || {}, undefined);