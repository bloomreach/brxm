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

import moment from 'moment-timezone';
import { palettes } from '@bloomreach/frontend-theme';

import isDevMode from './dev-mode';

const FALLBACK_LOCALE = 'en';

function config(
  $compileProvider,
  $httpProvider,
  $mdDateLocaleProvider,
  $mdIconProvider,
  $mdThemingProvider,
  $provide,
  $qProvider,
  $stateProvider,
  $translateSanitizationProvider,
  $translateProvider,
  $urlRouterProvider,
) {
  'ngInject';

  $provide.decorator('$q', ($delegate) => {
    'ngInject';

    if (window.$Promise !== $delegate) {
      window.$Promise = $delegate;
    }

    return $delegate;
  });

  // FIXME This suppresses all uncaught promises
  $qProvider.errorOnUnhandledRejections(false);

  $urlRouterProvider.otherwise('/');

  $stateProvider.state('hippo-cm', {
    url: '/',
    component: 'hippoCm',
    resolve: {
      translations: ($log, $translate, ConfigService) => {
        'ngInject';

        const { locale } = ConfigService;
        let language;
        if (locale) {
          try {
            ({ language } = new Intl.Locale(locale));
          } catch (e) {
            language = FALLBACK_LOCALE;
            $log.warn(`Failed to retrieve language from locale "${locale}", using "${FALLBACK_LOCALE}" instead`);
          }
        }
        $translateProvider
          .useStaticFilesLoader({
            prefix: 'i18n/',
            suffix: `.json?antiCache=${ConfigService.antiCache}`,
          })
          .registerAvailableLanguageKeys([FALLBACK_LOCALE, language], {
            [`${FALLBACK_LOCALE}_*`]: FALLBACK_LOCALE,
            [`${language}_*`]: language,
          })
          .fallbackLanguage(FALLBACK_LOCALE);

        return $translate.use(FALLBACK_LOCALE)
          .then(fallbackData => $translate.use(language).catch(() => {
            $log.warn(`Failed to load "${locale}" translations,  using "${FALLBACK_LOCALE}" instead`);
            return fallbackData;
          }));
      },
      dateLocale: (ConfigService) => {
        'ngInject';

        moment.locale(ConfigService.locale || FALLBACK_LOCALE);
        $mdDateLocaleProvider.months = moment.months(true);
        $mdDateLocaleProvider.shortMonths = moment.monthsShort(true);
        $mdDateLocaleProvider.days = moment.weekdays(true);
        // remove dots from day abbreviations (e.g. nl has this)
        const shortDays = moment.weekdaysShort(true);
        shortDays.forEach((item, index, array) => {
          array[index] = item.replace('.', '');
        });
        $mdDateLocaleProvider.shortDays = shortDays;
      },
    },
  });

  // AngularJs sanitizes all 'external' values itself automatically, so don't
  // let angular-translate sanitize values too to prevent double-escaping.
  $translateSanitizationProvider.addStrategy('none', value => value);
  $translateProvider.useSanitizeValueStrategy('none');

  $mdThemingProvider.definePalette('hippo-blue', {
    ...palettes.blue,
    500: '#0877d1',
    contrastDefaultColor: 'light',
    contrastDarkColors: ['50', '100', '200', '300', '400', 'A100'],
  });

  $mdThemingProvider.definePalette('hippo-orange', {
    ...palettes.orange,
    A700: '#e06717',
    contrastDefaultColor: 'light',
    contrastDarkColors: ['50', '100', '200', '300', '400', 'A100'],
  });

  $mdThemingProvider.definePalette('hippo-red', {
    ...palettes.red,
    contrastDefaultColor: 'light',
    contrastDarkColors: ['50', '100', '200', '300', '400', 'A100'],
  });

  $mdThemingProvider.definePalette('hippo-grey', {
    ...palettes.grey,
    contrastDefaultColor: 'dark',
    contrastLightColors: ['500', '600', '700', '800', '900', 'A200', 'A400', 'A700'],
  });

  $mdThemingProvider.theme('default')
    .primaryPalette('hippo-blue')
    .accentPalette('hippo-orange')
    .warnPalette('hippo-red')
    .backgroundPalette('hippo-grey');

  const HippoGlobal = window.parent.Hippo || {};
  const antiCache = HippoGlobal.antiCache ? `?antiCache=${HippoGlobal.antiCache}` : '';

  $mdDateLocaleProvider
    .formatDate = (date) => {
      const m = moment(date);
      return m.isValid() ? m.format('L') : '';
    };

  $mdDateLocaleProvider
    .parseDate = (dateString) => {
      const m = moment(dateString, 'L', true);
      return m.isValid() ? m.toDate() : new Date(NaN);
    };

  $mdIconProvider.fontSet('materialdesignicons', 'mdi');
  $mdIconProvider.defaultFontSet('mdi');

  $mdIconProvider
    .icon('back', `images/back.svg${antiCache}`)
    .icon('close', `images/close.svg${antiCache}`)
    .icon('document-status-changed', `images/document-status-changed.svg${antiCache}`)
    .icon('document-status-live', `images/document-status-live.svg${antiCache}`)
    .icon('document-status-new', `images/document-status-new.svg${antiCache}`)
    .icon('document', `images/document.svg${antiCache}`)
    .icon('folder-closed', `images/folder-closed.svg${antiCache}`)
    .icon('folder-open', `images/folder-open.svg${antiCache}`)
    .icon('left-side-panel-arrow-left', `images/left-side-panel-arrow-left.svg${antiCache}`)
    .icon('left-side-panel-arrow-right', `images/left-side-panel-arrow-right.svg${antiCache}`)
    .icon('maximize-sidepanel', `images/maximize-sidepanel.svg${antiCache}`)
    .icon('publish', `images/publish.svg${antiCache}`)
    .icon('resize-handle', `images/resize-handle.svg${antiCache}`)
    .icon('switch-to-content-editor', `images/switch-to-content-editor.svg${antiCache}`)
    .icon('toggle-components-overlay', `images/toggle-components-overlay.svg${antiCache}`)
    .icon('toggle-editing-overlay', `images/toggle-editing-overlay.svg${antiCache}`)
    .icon('un-maximize-sidepanel', `images/un-maximize-sidepanel.svg${antiCache}`)
    .icon('xpage', `images/xpage.svg${antiCache}`);

  // only enable Angular debug information during development
  $compileProvider.debugInfoEnabled(isDevMode());

  // report all HTTP requests as 'user activity' to the CMS to prevent active logout
  $httpProvider.interceptors.push((CmsService) => {
    'ngInject';

    return {
      request: (httpConfig) => {
        CmsService.publish('user-activity');
        return httpConfig;
      },
    };
  });
}

export default config;
