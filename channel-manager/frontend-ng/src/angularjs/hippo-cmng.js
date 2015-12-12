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

import { channelManagerApi } from './api/api.js';

function config ($stateProvider, $urlRouterProvider, $translateProvider) {
  $urlRouterProvider.otherwise('/');

  $stateProvider.state('main', {
    url: '/',
    templateUrl: 'hippo-cmng.html',
    resolve: {
      translations: function ($translate) {
        // TODO use locale from "config service" rather than hard-coded.
        return $translate.use('en')
          .catch(function () {
            $translate.use($translate.fallbackLanguage());
          });
      }
    }
  });

  // translations
  $translateProvider.useStaticFilesLoader({
    prefix: 'i18n/hippo-cmng.',
    suffix: '.json'
  });
  $translateProvider.fallbackLanguage('en');
  $translateProvider.useSanitizeValueStrategy('escaped');
}

export const hippoCmngModule = angular
  .module('hippo-cmng', [
    'pascalprecht.translate',
    'ui.router',
    'hippo-cmng-templates',
    channelManagerApi.name
  ])
  .config(config);

angular.element(document).ready(function () {
  angular.bootstrap(document.body, [hippoCmngModule.name], {
    strictDi: true
  });
});
