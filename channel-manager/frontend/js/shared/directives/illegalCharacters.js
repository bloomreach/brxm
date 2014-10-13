/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

    // the directive is not namespaced, because the special notation causes an error with the isolated scope definition.
    // Error: [$compile:iscp] Invalid isolate scope definition for directive 'hippo.channel.menu.illegalCharacters'.

    angular.module('hippo.channel')

        /**
         * @ngdoc directive
         * @name hippo.channel.menu.directive:illegalCharacters
         * @restrict A
         *
         * @description
         * Custom validator for elements that use ng-model.
         *
         * @param {string} hippo.channel.menu.illegalCharacters A string containing the space-separated invalid characters
         */
        .directive('illegalCharacters', [function () {
            return {
                restrict: 'A',
                require: 'ngModel',
                link: function link(scope, elem, attrs, ngModel) {
                    var validator = function (value) {
                        var isValid = true;
                        value = value || '';

                        angular.forEach(attrs.illegalCharacters.split(''), function (character) {
                            if (value.indexOf(character) >= 0) {
                                isValid = false;
                            }
                        });

                        ngModel.$setValidity('illegalCharacters', isValid);

                        return value;
                    };

                    ngModel.$parsers.unshift(validator);
                    ngModel.$formatters.push(validator);
                }
            };
        }]);
})();