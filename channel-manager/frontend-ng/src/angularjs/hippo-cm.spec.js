/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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

import angular from 'angular';
import 'angular-mocks';

describe('The hippo-cm module', () => {
  let configService;
  let $httpBackend;
  let $rootScope;
  let $state;
  let $translate;

  const MOCK_TRANSLATIONS = {
    en: {
      HIPPO: 'Hippo',
    },
    nl: {
      HIPPO: 'Nijlpaard',
    },
  };

  beforeEach(() => {
    angular.mock.module('hippo-cm', ($stateProvider, $translateProvider) => {
      $stateProvider.state('hippo-cm.dummy-child-state', {});
      $translateProvider.translations('en', MOCK_TRANSLATIONS.en);
      $translateProvider.translations('nl', MOCK_TRANSLATIONS.nl);
    });

    inject((ConfigService, _$httpBackend_, _$rootScope_, _$state_, _$translate_) => {
      configService = ConfigService;
      $httpBackend = _$httpBackend_;
      $rootScope = _$rootScope_;
      $state = _$state_;
      $translate = _$translate_;
    });
  });

  it('uses English translations by default', () => {
    $state.go('hippo-cm.dummy-child-state');
    $rootScope.$apply();

    expect($translate.instant('HIPPO')).toEqual('Hippo');
  });

  it('uses the locale specified in the config service', () => {
    configService.locale = 'nl';

    $state.go('hippo-cm.dummy-child-state');
    $rootScope.$apply();

    expect($translate.instant('HIPPO')).toEqual('Nijlpaard');
  });

  it('falls back to English translations for unknown locales', () => {
    configService.locale = 'no-such-locale';
    $httpBackend.whenGET('i18n/no-such-locale.json?antiCache=123').respond(404);

    $state.go('hippo-cm.dummy-child-state');
    $rootScope.$apply();

    expect($translate.instant('HIPPO')).toEqual('Hippo');
  });
});
