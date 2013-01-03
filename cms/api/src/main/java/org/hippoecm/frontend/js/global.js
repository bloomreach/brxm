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

    if (window.console === undefined) {
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

    String.prototype.format = function() {
        var formatted, regexp, i, len;
        formatted = this;
        for (i = 0, len = arguments.length; i < len; i++) {
            regexp = new RegExp('\\{' + i + '\\}', 'gi');
            formatted = formatted.replace(regexp, arguments[i]);
        }
        return formatted;
    };

}());
