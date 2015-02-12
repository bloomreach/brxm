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

describe('the showFocus directive', function () {
    'use strict';

    var wrapper, element, $scope, $timeout;

    beforeEach(module('hippo.channel'));

    // TODO: we should refactor the tests so we don't need this beforeEach block
    beforeEach(function() {
        module(function($provide) {
            $provide.value('hippo.channel.HstApiRequests', jasmine.createSpy());
        });
    });

    beforeEach(inject(function ($compile, $rootScope, _$timeout_) {
        $scope = $rootScope;
        $timeout = _$timeout_;
        wrapper = angular.element('<div data-ng-show="showWrapper">');
        element = angular.element('<input data-show-focus="focusInput">');
        $compile(element)($scope);
        $scope.$digest();
    }));

    afterEach(function () {
        element.remove();
        wrapper.remove();
    });

    it('should focus an element within an ng-show', function () {
        element.appendTo(wrapper);
        wrapper.appendTo(document.body);
        $scope.showWrapper = true;
        expect(wrapper).toBeVisible();

        $scope.focusInput = true;
        $timeout.flush();
        expect(element).toBeFocused();
    });

});