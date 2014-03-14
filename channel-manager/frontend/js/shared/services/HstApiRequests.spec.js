/*
* Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

describe('HstApiRequests', function () {
    'use strict';

    var hstApiRequests, prefix, configServiceMock, qMock = null;

    beforeEach(function () {

        prefix = 'hst-api-url-prefix';

        configServiceMock = {
            apiUrlPrefix: prefix
        };

        qMock = {
            when: function(config) {

            }
        };
        spyOn(qMock, 'when');

        module('hippo.channel', function($provide) {
            $provide.value('hippo.channel.ConfigService', configServiceMock);
            $provide.value('$q', qMock);
        });

        inject(['hippo.channel.HstApiRequests', function(HstApiRequests) {
            hstApiRequests = HstApiRequests;
        }]);

    });

    it('should exist', function () {
        expect(hstApiRequests).toBeDefined();
    });

    it('should add FORCE_CLIENT_HOST GET parameter', function () {
        var url = prefix + '/something';
        var config = {
            url: url
        };
        hstApiRequests.request(config);
        expect(config.params.FORCE_CLIENT_HOST).toBe(true);
        expect(config.url).toBe(url);
        expect(qMock.when).toHaveBeenCalledWith(config);
    });

    it('should not  add FORCE_CLIENT_HOST GET parameter', function () {
        var url = '/something-else';
        var config = {
            url: url
        };
        hstApiRequests.request(config);
        expect(config.params).toBeUndefined();
        expect(config.url).toBe(url);
        expect(qMock.when).toHaveBeenCalledWith(config);
    });
});
