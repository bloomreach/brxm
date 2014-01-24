'use strict';
(function (Essentials, undefined) {
    // {"values":{"entry":[{"key":"foo2","value":"bar"},{"key":"foo","value":"bar"}]}}

    //############################################
    // UTILS
    //############################################

    Essentials.emptyPayload = function () {
        var payload = {};
        payload.values = {};
        payload.values.entry = [];
        return {"payload": payload};
    };

    Essentials.addPayloadData = function (key, value, payload) {
        if (payload === undefined || payload == null) {
            payload = Essentials.emptyPayload();
        }
        payload['payload'].values.entry.push({"key": key, "value": value});
        return payload;
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


})(window.Essentials = window.Essentials || {}, undefined);