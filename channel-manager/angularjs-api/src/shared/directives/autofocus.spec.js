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

describe('the autofocus directive', function () {
    'use strict';

    var element;

    beforeEach(module('hippo.channel'));

    // TODO: we should refactor the tests so we don't need this beforeEach block
    beforeEach(function() {
        module(function($provide) {
            $provide.value('hippo.channel.HstApiRequests', jasmine.createSpy());
        });
    });

    beforeEach(inject(function (_$compile_, _$rootScope_, _$timeout_) {
        element = angular.element('<input data-hippo-channel-auto-focus>');
        _$compile_(element)(_$rootScope_);
        _$rootScope_.$digest();
        element.appendTo(document.body);
        _$timeout_.flush();
    }));

    afterEach(function () {
        element.remove();
    });

    it('should focus an element', function () {
        expect(element).toBeFocused();
    });

});
