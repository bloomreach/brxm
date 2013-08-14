/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
(function(window, document) {
    "use strict";

    /**
     * Shows a query parameter with the given name and string value in the URL. When the value is empty, the parameter
     * will not be shown.
     *
     * @param name the name of the parameter
     * @param value the string value of the parameter (use an empty string to remove the parameter from the url).
     */
    Hippo.showParameterInUrl = function(name, value) {
        if (window.history && window.history.pushState) {
            var nameValuePair = name + '=' + value,
                urlParameters = document.location.toString().split('?'),
                urlBase = urlParameters.shift(),
                hasValue = value.length > 0,
                url, queryString, parameters, isChanged, i;

            if (urlParameters.length === 0) {
                if (hasValue) {
                    url = urlBase + '?' + nameValuePair;
                } else {
                    url = urlBase;
                }
            } else {
                queryString = urlParameters.join('?'); // join remaining parts, so parameters containing ? are handled correctly
                parameters = queryString.split(/[&;]/g);
                isChanged = false;
                for (i = parameters.length - 1; i >= 0; i--) {
                    if (parameters[i].indexOf(name + '=') === 0) {
                        if (hasValue) {
                            parameters[i] = nameValuePair;
                        } else {
                            parameters.splice(i, 1);
                        }
                        isChanged = true;
                    }
                }
                url = urlBase;
                if (parameters.length > 0) {
                    url += '?' + parameters.join('&');
                }
                if (!isChanged && hasValue) {
                    url += '&' + nameValuePair;
                }
            }
            window.history.pushState(null, null, url);
        }
    };
}(window, document));
