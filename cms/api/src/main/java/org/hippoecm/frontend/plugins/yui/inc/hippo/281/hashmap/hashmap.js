/*
 * Copyright 2008-2013 Hippo
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
 * Class that represents a java.util.HashMap
 * </p>
 * @namespace YAHOO.hippo
 * @module hashmap
 */

/**
 * add method removeAt(index) to Array prototype
 */
Array.prototype.removeAt = function(index) {
    var part1 = this.slice(0, index),
        part2 = this.slice(index + 1);

    return part1.concat(part2);
};

(function() {

    YAHOO.namespace('hippo');

    YAHOO.hippo.HashMap = function() {
        this.keys = [];
        this.values = [];
    };

    YAHOO.hippo.HashMap.prototype = {
        
        put : function(key, value) {
            var index, result, previous;
            index = this._getIndex(key);
            if (index === -1) {
                this.keys.push(key);
                this.values.push(value);
                result = null;
            } else {
                previous = this.values[index];
                this.values[index] = value;
                result = previous;
            }
            return result;
        },
        
        putAll : function(hashMap) {
            var entries, e, entry;
            entries = hashMap.entrySet();
            for (e in entries) {
                if (entries.hasOwnProperty(e)) {
                    entry = entries[e];
                    this.put(entry.getKey(), entry.getValue());
                }
            }
        },
        
        get : function(key) {
            var index, msg;

            index = this._getIndex(key);

            if (index === -1) {
                //TODO: throw error or return null?
                msg = "No value found for key[" + key + "]";
                YAHOO.log(msg, "error", "HashMap");
                throw new Error(msg);
            }
             return this.values[index];
        },
        
        remove : function(key) {
            var index, msg, value;
            index = this._getIndex(key);
            if (index === -1) {
                //TODO: throw error or return null
                msg = "Can not remove entry, key[" + key + "] not found";
                YAHOO.log(msg, "error", "HashMap");
                throw new Error(msg);
            }
            value = this.values[index];
            this.keys = this.keys.removeAt(index);
            this.values = this.values.removeAt(index);
            
            return value;
        },

        containsKey : function(key) {
            return this._getIndex(key) > -1;
        },
        
        size : function() {
            return this.keys.length;
        },
        
        isEmpty : function() {
            return this.keys.length === 0;
        },
        
        clear : function() {
            this.keys = [];
            this.values = [];
        },

        keySet : function() {
            return this.keys;
        },
        
        valueSet : function() {
            return this.values;
        },
        
        entrySet : function() {
            var o, i, len;
            o = {};
            for (i = 0, len = this.keys.length; i < len; i++) {
                o['o' + i] = new YAHOO.hippo.HashMapEntry(this.keys[i], this.values[i]);
            }
            return o;
        },

        entrySetAsArray : function() {
            var ar, i, len;
            ar = [];
            for (i = 0, len = this.keys.length; i < len; i++) {
                ar.push(new YAHOO.hippo.HashMapEntry(this.keys[i], this.values[i]));
            }
            return ar;
        },
        
        forEach : function(context, visitor) {
            var ar, i, len, br;
            ar = this.entrySetAsArray();
            for (i = 0, len = ar.length; i < len; i++) {
                br = visitor.call(context, ar[i].getKey(), ar[i].getValue());
                if (br === true) {
                    break;
                }
            }
        },

        _getIndex : function(key) {
            var i, len;
            for (i = 0, len = this.keys.length; i < len; i++) {
                if (this.keys[i] === key) {
                    return i;
                }
            }
            return -1;
        },
        
        toString : function() {
            var x, entries, i, len, entry;
            x = '';
            entries = this.entrySetAsArray();
            for (i = 0, len = entries.length; i < len; i++) {
                if (i > 0) {
                    x += ', ';
                }
                entry = entries[i];
                x +='{' + entry.getKey() + '=' + entry.getValue()+ '}'; 
            }
            return 'HashMap[' + x + ']';
        }
    };
    
    YAHOO.hippo.HashMapEntry = function(key, value) {
        this._key = key;
        this._value = value;
    };
    
    YAHOO.hippo.HashMapEntry.prototype = {
        getKey : function() {
            return this._key;
        },
        
        getValue : function() {
            return this._value;
        }
    };

}());

YAHOO.register("hashmap", YAHOO.hippo.HashMap, {
    version: "2.8.1", build: "19"
});