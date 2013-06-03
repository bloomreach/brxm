/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function(XinhaTools) {

    /**
     * Return a new function and ensure the provided function is always executed within the provided context
     * with the arguments passed to the wrapping function.
     */
    XinhaTools.proxy = function(func, context) {
        return function() {
            func.apply(context, arguments);
        };
    };

    /**
     * Return the instance of a plugin or null of not found.
     */
    XinhaTools.getPlugin = function(editor, id) {
        return XinhaTools.nullOrValue(editor, ['plugins', id, 'instance']);
    };

    /**
     * Return the object referenced by the path array or null if not found.
     */
    XinhaTools.nullOrValue = function(ref, path) {
        if (ref === null) {
            return null;
        }
        for (var i = 0; i < path.length; i++) {
            if (XinhaTools.isValue(ref[path[i]])) {
                ref = ref[path[i]];
            } else {
                return null;
            }
        }
        return ref;
    };

    /**
     * Check whether the ref value is not undefined and not null
     */
    XinhaTools.isValue = function(ref) {
        return ref !== undefined && ref !== null;
    };

})(window.XinhaTools = window.XinhaTools || {});