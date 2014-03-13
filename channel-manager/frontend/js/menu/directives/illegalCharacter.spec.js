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

describe('illegal character directive', function () {
    'use strict';
    var $scope, form, element;

    beforeEach(module('hippo.channel.menu'));
    beforeEach(function() {
        module(function($provide) {
            $provide.value('hippo.channel.HstApiRequests', jasmine.createSpy());
        });
    });
    beforeEach(inject(function ($compile, $rootScope) {
        $scope = $rootScope;
        $scope.validation = {
            illegalCharacters: ', 1 # ~ a xyz'
        };
        element = angular.element(
            '<form name="form">' +
                '<input type="text" ng-model="model.validatedField" name="validatedField" data-illegal-characters="validation.illegalCharacters">' +
            '</form>');

        $scope.model = {validatedField: null};
        $compile(element)($scope);
        $scope.$digest();
        form = $scope.form;
    }));

    it('should pass without invalid character', function () {
        form.validatedField.$setViewValue('bcdefghijklmnopqrstuvw');
        expect($scope.model.validatedField).toEqual('bcdefghijklmnopqrstuvw');
        expect(form.validatedField.$valid).toBe(true);
    });

    it('should fail with an invalid character', function () {
        form.validatedField.$setViewValue('a');
        expect($scope.model.validatedField).toEqual('a');
        expect(form.validatedField.$valid).toBe(false);
    });

    it('should fail with multiple invalid characters', function () {
        form.validatedField.$setViewValue(',-1');
        expect($scope.model.validatedField).toEqual(',-1');
        expect(form.validatedField.$valid).toBe(false);
    });

    it('should not fail for empty value', function () {
        form.validatedField.$setViewValue();
        expect($scope.model.validatedField).toEqual('');
        expect(form.validatedField.$valid).toBe(true);
    });
});
