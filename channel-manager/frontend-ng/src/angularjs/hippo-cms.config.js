/*
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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

export function config($mdThemingProvider, $stateProvider, $urlRouterProvider, $translateProvider) {
  'ngInject';

  $urlRouterProvider.otherwise('/');

  $stateProvider.state('hippo-cm', {
    url: '/',
    templateUrl: 'hippo-cm.html',
    resolve: {
      translations: function translations($translate, ConfigService) {
        return $translate.use(ConfigService.locale)
          .catch(() => {
            $translate.use($translate.fallbackLanguage());
          });
      },
    },
    abstract: true,
  });

  $translateProvider.useStaticFilesLoader({
    prefix: 'i18n/',
    suffix: '.json',
  });
  $translateProvider.fallbackLanguage('en');
  $translateProvider.useSanitizeValueStrategy('escaped');

  $mdThemingProvider.definePalette('hippo-blue', {
    50: '#d9edff',
    100: '#b0dafe',
    200: '#7dc2fd',
    300: '#4aa9fc',
    400: '#1c93fb',
    500: '#0086f8',
    600: '#0075dc',
    700: '#0065b1',
    800: '#00539f',
    900: '#004280',
    A100: '#7dc2fd',
    A200: '#1c93fb',
    A400: '#0086f8',
    A700: '#0065b1',
    contrastDefaultColor: 'light',
    contrastDarkColors: ['50', '100', '200', '300', '400', 'A100', 'A200'],
  });

  $mdThemingProvider.definePalette('hippo-orange', {
    50: '#ffe6d5',
    100: '#fcd2b3',
    200: '#fcbc8b',
    300: '#fda663',
    400: '#fd8d36',
    500: '#fc7f1e',
    600: '#e16500',
    700: '#c75500',
    800: '#a84500',
    900: '#8a3600',
    A100: '#fcbc8b',
    A200: '#fd8d36',
    A400: '#fc7f1e',
    A700: '#c75500',
    contrastDefaultColor: 'light',
    contrastDarkColors: ['50', '100', '200', '300', '400', '500', '600', 'A100', 'A200', 'A400'],
  });

  $mdThemingProvider.definePalette('hippo-red', {
    50: '#ffd9d9',
    100: '#ffaeae',
    200: '#ff8e86',
    300: '#ff706f',
    400: '#ff5656',
    500: '#fc331c',
    600: '#ff2500',
    700: '#f11c00',
    800: '#d41700',
    900: '#9b0404',
    A100: '#ff8e86',
    A200: '#ff5656',
    A400: '#fc331c',
    A700: '#f11c00',
    contrastDefaultColor: 'light',
    contrastDarkColors: ['50', '100', '200', '300', '400', 'A100', 'A200'],
  });

  $mdThemingProvider.definePalette('hippo-grey', {
    50: '#ffffff',
    100: '#f4f4f5',
    200: '#e8e8ea',
    300: '#d9d9dd',
    400: '#b8bcbf',
    500: '#92a0a5',
    600: '#697279',
    700: '#39434c',
    800: '#22272f',
    900: '#000000',
    A100: '#e8e8ea',
    A200: '#b8bcbf',
    A400: '#92a0a5',
    A700: '#39434c',
    contrastDefaultColor: 'light',
    contrastDarkColors: ['50', '100', '200', '300', '400', '500', 'A100', 'A200', 'A400'],
  });

  $mdThemingProvider.theme('default')
    .primaryPalette('hippo-blue')
    .accentPalette('hippo-orange')
    .warnPalette('hippo-red')
    .backgroundPalette('hippo-grey');
}

