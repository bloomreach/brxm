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
describe('filter', function () {
    'use strict';

    beforeEach(module('hippo.channel'));

    it('should should leave capitals or lowercase characters unchanged', inject(function (hippoChannelAlphanumericFilter) {
        expect(hippoChannelAlphanumericFilter('a')).toEqual('a');
        expect(hippoChannelAlphanumericFilter('B')).toEqual('B');
    }));

    it('should convert spaces to dashes', inject(function (hippoChannelAlphanumericFilter) {
        expect(hippoChannelAlphanumericFilter(' ')).toEqual('-');
    }));

    it('should convert an escaped non-alphanumerical character to a dash', inject(function (hippoChannelAlphanumericFilter) {
        expect(hippoChannelAlphanumericFilter('\'')).toEqual('-');
    }));

    it('should convert a set of non-alphanumerical characters to dashes', inject(function (hippoChannelAlphanumericFilter) {
        expect(hippoChannelAlphanumericFilter('`~@#$%^&*()_+-[]{};"')).toEqual('--------------------');
    }));

    it('should convert special characters to dashes', inject(function (hippoChannelAlphanumericFilter) {
        expect(hippoChannelAlphanumericFilter('é')).toEqual('-');
        expect(hippoChannelAlphanumericFilter('á')).toEqual('-');
        expect(hippoChannelAlphanumericFilter('í')).toEqual('-');
        expect(hippoChannelAlphanumericFilter('ó')).toEqual('-');
        expect(hippoChannelAlphanumericFilter('ú')).toEqual('-');
        expect(hippoChannelAlphanumericFilter('ü')).toEqual('-');
        expect(hippoChannelAlphanumericFilter('ñ')).toEqual('-');
    }));

});
