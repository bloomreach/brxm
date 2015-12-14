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

describe('ConfigService', function () {
  'use strict';

  var configService;

  beforeEach(function () {
    module('hippo-cm-api');
  });

  describe("provides default configuration", function () {
    beforeEach(inject([
      'ConfigService', function (ConfigService) {
        configService = ConfigService;
      }
    ]));

    it('with sensible defaults', function() {
      expect(configService.apiUrlPrefix).toBeDefined();
      expect(configService.locale).toBeDefined();
      expect(configService.antiCache).toBeDefined();

      expect(configService.locale).toEqual('en');
      expect(configService.apiUrlPrefix).toEqual('http://localhost:8080/site/_rp');
    });

  });

  describe('allows custom configuration', function() {
    beforeEach(function() {
      module(function ($provide) {
        $provide.value('IFrameService', {isActive: true, getConfig: function() {
          return {
            locale: 'nl',
            apiUrlPrefix: 'https://127.0.0.1:9080/web/one/two'
          };
        }});
      });
    });

    beforeEach(inject([
      'ConfigService', function (ConfigService) {
        configService = ConfigService;
      }
    ]));

    it('passed in by the IFrameService', function() {
      expect(configService.locale).toEqual('nl');
      expect(configService.apiUrlPrefix).toEqual('https://127.0.0.1:9080/web/one/two');
    });

  });

});
