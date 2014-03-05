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
    // Error: [$compile:iscp] Invalid isolate scope definition for directive 'hippo.channelManager.menuManager.illegalCharacters'.

    angular.module('hippo.channelManager.menuManager')

        /**
         * @ngdoc directive
         * @name hippo.channelManager.menuManager.directive:illegalCharacters
         * @restrict A
         *
         * @description
         * Custom validator for elements that use ng-model.
         * Modified version of: http://ericpanorel.net/2013/10/05/angularjs-password-match-form-validation/
         *
         * @param {string} hippo.channelManager.menuManager.illegalCharacters A string containing the space-separated invalid characters
         */
        .directive('illegalCharacters', [function () {
            return {
                restrict: 'A',
                require: '?ngModel',
                scope: {
                    characters: '=illegalCharacters'
                },
                link: function link(scope, elem, attrs, ngModel) {
                    var directiveName = 'illegalCharacters';
                    var validatorName = 'illegalCharacters';

                    // do nothing if ng-model is not defined, or no illegal characters specified
                    if (!ngModel || !attrs[directiveName]) {
                        return;
                    }

                    var validator = function (value) {
                        var valid = true;
                        value = value || '';

                        angular.forEach(scope.characters.split(' '), function (character) {
                            if (value.indexOf(character) >= 0) {
                                valid = false;
                            }
                        });

                        ngModel.$setValidity(validatorName, valid);

                        return value;
                    };

                    ngModel.$parsers.unshift(validator);
                    ngModel.$formatters.push(validator);

                    scope.$watch(attrs[directiveName], function () {
                        validator(ngModel.$viewValue);
                    });
                }
            };
        }]);
})();