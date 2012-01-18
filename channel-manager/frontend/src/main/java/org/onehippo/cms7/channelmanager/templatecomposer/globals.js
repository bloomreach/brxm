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

(function() {
    // alternative to typeOf with more capabilities
    Object.toType = (function toType(global) {
        return function(obj) {
            if (obj === global) {
                return "global";
            }
            return ({}).toString.call(obj).match(/\s([a-z|A-Z]+)/)[1].toLowerCase();
        }
    })(this);

    String.prototype.format = function() {
        var formatted = this;
        for (var i = 0; i < arguments.length; i++) {
            var regexp = new RegExp('\\{' + i + '\\}', 'gi');
            formatted = formatted.replace(regexp, arguments[i]);
        }
        return formatted;
    };

    if (typeof JSON === 'undefined') {
        window.JSON = {
            stringify : function() {
                return "";
            },
            parse : function() {
                return null;
            }
        }
    };

    if (typeof window.console === 'undefined') {
        window.console = {
            log : function() {
            },
            dir : function() {
            },
            info : function() {
            },
            warn : function() {
            },
            error : function() {
            },
            group : function() {
            },
            groupEnd : function() {
            }
        };
    }

    HST = {
        COMPONENT       : 'COMPONENT',
        CONTAINER       : 'CONTAINER_COMPONENT',
        CONTAINERITEM   : 'CONTAINER_ITEM_COMPONENT',
        CMSLINK : 'cmslink',

        ATTR : {
            ID : 'uuid',
            TYPE : 'type',
            XTYPE : 'xtype',
            INHERITED : 'inherited',
            URL : 'url',
            REF_NS : 'refNS'
        },

        CLASS : {
            CONTAINER : 'hst-container',
            ITEM : 'hst-container-item',
            EDITLINK : 'hst-cmseditlink'
        },

        DIR : {
            VERTICAL    : 1,
            HORIZONTAL  : 2
        }
    };

})();

