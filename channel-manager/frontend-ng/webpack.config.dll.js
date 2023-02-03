/*
 * Copyright 2019-2023 Bloomreach
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

const config = require('@bloomreach/frontend-build/lib/webpack.config');

module.exports = {
  ...config,
  entry: {
    angularjs: [
      'angular',
      'angular-animate',
      'angular-aria',
      'angular-local-storage',
      'angular-material',
      'angular-messages',
      'angular-mocks',
      'angular-translate',
      'angular-translate-loader-static-files',
      '@uirouter/angularjs',
      'angular-ui-tree',
      'ng-device-detector',
      'ng-focus-if',
    ],
    vendor: [
      '@bloomreach/dragula',
      'jquery',
      'mutation-summary',
      'moment-timezone',
      'rxjs',
    ],
  },
};
