/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
(function () {
    'use strict';

    angular.module('hippo.channel')
        .filter('incrementProperty', function () {
            return function (collection, propertyName, propertyValue) {
                var itemsWithProperty = [];
                function findPropertiesAndSubProperties (newCollection) {
                    for (var i = 0; i < newCollection.length; i++) {
                        var propName = newCollection[i][propertyName],
                            match = propName.match(/\((\d+)\)/);
                        if (match) {
                            itemsWithProperty.push(match[1]);
                        } else if(propName.match(propertyValue)) {
                            itemsWithProperty.push('0');
                        }
                    }
                }
                findPropertiesAndSubProperties(collection);

                if(itemsWithProperty.length === 0) {
                    return propertyValue;
                } else {
                    var maxNum = Math.max.apply(null, itemsWithProperty);
                    if(!propertyValue.match(/\((\d+)\)/)) {
                        propertyValue = propertyValue + ' (' + maxNum + ')';
                    }
                    return propertyValue.replace(/\((\d+)\)/, function() {
                        return '(' + (maxNum + 1) + ')';
                    });
                }

                return null;
            };
        });
})();