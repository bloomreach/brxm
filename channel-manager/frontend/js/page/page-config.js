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
(function () {
  "use strict";

  angular.module('hippo.channel.page')

    .config([
      '$stateProvider',
      '$translateProvider',
      '$tooltipProvider',
      function ($stateProvider, $translateProvider, $tooltipProvider) {

        // routing
        $stateProvider
          .state('settings', {
            url: '/settings',
            controller: 'hippo.channel.page.SettingsCtrl',
            templateUrl: 'states/settings/settings.html'
          });

        // translations
        $translateProvider.useStaticFilesLoader({
          prefix: 'i18n/',
          suffix: '.json'
        });

        // tooltips
        $tooltipProvider.options({
          animation: false
        });
        $tooltipProvider.setTriggers({
          'show': 'hide'
        });
      }
    ]
  );

}());
