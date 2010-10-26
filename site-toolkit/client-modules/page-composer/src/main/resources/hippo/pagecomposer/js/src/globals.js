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
    if (typeof window.console === 'undefined') {
        window.console = {
            log : function() {
            },
            dir : function() {
            },
            warn : function() {
            },
            error : function() {
            }
        };
    }

    //Copied from http://ejohn.org/blog/javascript-array-remove/
    //Adds removeByIndex to array
    Array.prototype.removeByIndex= function(from, to) {
        var rest = this.slice((to || from) + 1 || this.length);
        this.length = from < 0 ? this.length + from : from;
        return this.push.apply(this, rest);
    };


    HST = {
        COMPONENT       : 'COMPONENT',
        CONTAINER       : 'CONTAINER_COMPONENT',
        CONTAINERITEM   : 'CONTAINER_ITEM_COMPONENT'
    };

})();
