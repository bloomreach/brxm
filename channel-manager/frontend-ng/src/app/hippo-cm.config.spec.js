/*
 * Copyright 2015-2020 Hippo B.V. (http://www.onehippo.com)
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
import moment from 'moment-timezone';

describe('The hippo-cm module config', () => {
  let configService;
  let $http;
  let $httpBackend;
  let $injector;
  let $rootScope;
  let $state;
  let $translate;
  let $window;

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

    inject((ConfigService, _$http_, _$httpBackend_, _$injector_, _$rootScope_, _$state_, _$translate_, _$window_) => {
      configService = ConfigService;
      $http = _$http_;
      $httpBackend = _$httpBackend_;
      $injector = _$injector_;
      $rootScope = _$rootScope_;
      $state = _$state_;
      $translate = _$translate_;
      $window = _$window_;
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
    configService.locale = 'unknown';
    $httpBackend.expectGET('i18n/unknown.json?antiCache=123').respond(404);

    $state.go('hippo-cm.dummy-child-state');
    $httpBackend.flush();

    expect($translate.instant('HIPPO')).toEqual('Hippo');
  });

  it('reports user activity to the CMS when the backend is called', () => {
    spyOn($window.APP_TO_CMS, 'publish');
    $httpBackend.expectGET('/some-url', {
      Accept: 'application/json, text/plain, */*',
    }).respond(200);
    $http.get('/some-url');
    $httpBackend.flush();
    expect($window.APP_TO_CMS.publish).toHaveBeenCalledWith('user-activity');
  });

  it('uses the user locale for moment.js', () => {
    configService.locale = 'nl';
    $state.go('hippo-cm.dummy-child-state');
    $rootScope.$apply();

    expect(moment.locale()).toEqual('nl');
  });

  describe('Angular Material Date Locale Provider', () => {
    it('uses an empty string for invalid dates', () => {
      const $mdDateLocale = $injector.get('$mdDateLocale');
      const invalidDate = new Date('2015-13-25T35:78:89.000Z');
      expect($mdDateLocale.formatDate(invalidDate)).toEqual('');
    });

    describe('with a Dutch user locale', () => {
      let $mdDateLocale;

      beforeEach(() => {
        configService.locale = 'nl';
        $state.go('hippo-cm.dummy-child-state');
        $rootScope.$apply();

        // fetch $mdDateLocale after setting the locale so it uses the tweaks in the $mdDateLocaleProvider
        $mdDateLocale = $injector.get('$mdDateLocale');
      });

      it('uses locale-specific month names', () => {
        expect($mdDateLocale.months).toEqual([
          'januari',
          'februari',
          'maart',
          'april',
          'mei',
          'juni',
          'juli',
          'augustus',
          'september',
          'oktober',
          'november',
          'december',
        ]);
      });

      it('uses locale-specific short month names', () => {
        expect($mdDateLocale.shortMonths).toEqual([
          'jan.',
          'feb.',
          'mrt.',
          'apr.',
          'mei',
          'jun.',
          'jul.',
          'aug.',
          'sep.',
          'okt.',
          'nov.',
          'dec.',
        ]);
      });

      it('uses locale-specific day names', () => {
        expect($mdDateLocale.days).toEqual([
          'maandag',
          'dinsdag',
          'woensdag',
          'donderdag',
          'vrijdag',
          'zaterdag',
          'zondag',
        ]);
      });

      it('uses locale-specific short day names', () => {
        expect($mdDateLocale.shortDays).toEqual([
          'ma',
          'di',
          'wo',
          'do',
          'vr',
          'za',
          'zo',
        ]);
      });

      it('formats dates in the user locale', () => {
        const christmas = new Date('2015-12-25T06:53:00.000Z');
        expect($mdDateLocale.formatDate(christmas)).toEqual('25-12-2015');
      });
    });

    describe('without a locale', () => {
      let $mdDateLocale;

      beforeEach(() => {
        configService.locale = null;
        $state.go('hippo-cm.dummy-child-state');
        $rootScope.$apply();

        // fetch $mdDateLocale after setting the locale so it uses the tweaks in the $mdDateLocaleProvider
        $mdDateLocale = $injector.get('$mdDateLocale');
      });

      it('falls back to English', () => {
        expect(moment.locale()).toEqual('en');
      });

      it('formats dates in the English locale', () => {
        const christmas = new Date('2015-12-25T06:53:00.000Z');
        expect($mdDateLocale.formatDate(christmas)).toEqual('12/25/2015');
      });
    });
  });
});
