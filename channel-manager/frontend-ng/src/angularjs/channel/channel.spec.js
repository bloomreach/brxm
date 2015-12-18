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

'use strict';

describe('The hippo-cm.channel module', function () {

  'use strict';

  var configService, $httpBackend, $state, $translate, translationCache;

  var MOCK_TRANSLATIONS = {
    en: {
      HIPPO: 'Hippo'
    },
    nl: {
      HIPPO: 'Nijlpaard'
    }
  };

  beforeEach(function () {
    module('hippo-cm');

    inject(function (ConfigService, _$httpBackend_, _$state_, _$translate_, $translationCache) {
      configService = ConfigService;
      $httpBackend = _$httpBackend_
      $state = _$state_;
      $translate = _$translate_;
      translationCache = $translationCache;
    });

    $httpBackend.whenGET('i18n/hippo-cm.en.json').respond(MOCK_TRANSLATIONS.en);
    $httpBackend.whenGET('i18n/hippo-cm.nl.json').respond(MOCK_TRANSLATIONS.nl);
    $httpBackend.whenGET("i18n/hippo-cm.no-such-locale.json").respond(404);

    $httpBackend.flush();
  });

  //afterEach(function () {
  //
  //});
  //
  it('resolves english translations', function (done) {
    $state.go('hippo-cm.channel').then(function () {
      expect($translate.instant('HIPPO')).toEqual("Hippo");
      done();
    });
    $httpBackend.flush();
  });

  it('resolves translations in the locale specified in the config service', function (done) {
    configService.locale = 'nl';
    $state.go('hippo-cm.channel').then(function () {
      expect($translate.instant('HIPPO')).toEqual("Nijlpaard");
      done();
    });
    $httpBackend.flush();
  });


  it('falls back to english translations', function (done) {
    configService.locale = 'no-such-locale';
    $state.go('hippo-cm.channel').then(function () {
      expect($translate.instant('HIPPO')).toEqual("Hippo");
      done();
    });
    $httpBackend.flush();
  });

  it('falls back to english translations when the translations fail to load', function (done) {
    configService.locale = 'no-such-locale';
    $state.go('hippo-cm.channel').then(function () {
      expect($translate.instant('HIPPO')).toEqual("Hippo");
      done();
    });
    $httpBackend.flush();
  });

});
