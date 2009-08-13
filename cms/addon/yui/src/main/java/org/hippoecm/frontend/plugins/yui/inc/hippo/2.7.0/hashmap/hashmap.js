/*
 * Copyright 2008 Hippo
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
function _removeAt( index )
{
  var part1 = this.slice( 0, index);
  var part2 = this.slice( index+1 );

  return( part1.concat( part2 ) );
}
Array.prototype.removeAt = _removeAt;

( function() {

    YAHOO.namespace('hippo');

    YAHOO.hippo.HashMap = function() {
        this.keys = [];
        this.values = [];
    };

    YAHOO.hippo.HashMap.prototype = {
        
        put : function(key, value) {
            var index = this._getIndex(key);
            if(index == -1) {
                this.keys.push(key);
                this.values.push(value);
                return null;
            } else {
                var previous = this.values[index];
                this.values[index] = value;
                return previous;
            }
        },
        
        putAll : function(hashMap) {
            var entries = hashMap.entrySet();
            for(var e in  entries) {
                var entry =entries[e];
                this.put(entry.getKey(), entry.getValue());
            }
        },
        
        get : function(key) {
            var index = this._getIndex(key);

            if(index == -1) {
                //TODO: throw error or return null?
                var msg = "No value found for key[" + key + "]";
                YAHOO.log(msg, "error", "HashMap");
                throw new Error(msg)
            }
             return this.values[index];
        },
        
        remove : function(key) {
            var index = this._getIndex(key);
            if(index == -1) {
                //TODO: throw error or return null
                var msg = "Can not remove entry, key[" + key + "] not found";
                YAHOO.log(msg, "error", "HashMap");
                throw new Error(msg)
            }
            var value = this.values[index];
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
            return this.keys.length == 0;
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
            var o = {};
            for(var i=0; i<this.keys.length; i++) {
                o['o' + i] = new YAHOO.hippo.HashMapEntry(this.keys[i], this.values[i]);
            }
            return o;
        },

        entrySetAsArray : function() {
            var ar = [];
            for(var i=0; i<this.keys.length; i++) {
                ar.push(new YAHOO.hippo.HashMapEntry(this.keys[i], this.values[i]));
            }
            return ar;
        },
        
        forEach : function(context, visitor) {
            var ar = this.entrySetAsArray();
            for(var i=0; i<ar.length; i++) {
                var br = visitor.call(context, ar[i].getKey(), ar[i].getValue());
                if(br === true) {
                    break;
                }
            }
        },

        _getIndex : function(key) {
            for (var i=0; i < this.keys.length; i++) {
                if( this.keys[ i ] == key ) {
                    return i;
                }
            }
            return -1;
        },
        
        toString : function() {
            var x = '';
            var entries = this.entrySetAsArray();
            for(var i=0; i<entries.length; i++) {
                if (i>0) x += ', ';
                var entry = entries[i];
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

})();

YAHOO.register("hashmap", YAHOO.hippo.HashMap, {
    version: "2.7.0", build: "1799"
});