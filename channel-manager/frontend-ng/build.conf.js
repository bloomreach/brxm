/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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
const path = require('path');

const targetDir = path.resolve('./target/classes/angular/hippo-cm/');
const npmDir = path.resolve('./node_modules');

const customConfig = {
  typeScript: false,
  dist: targetDir,
  publicPath: '',
  htmlExcludes: /items.renderer.html/,
  vendorExcludes: [
    'open-sans-fontface',
  ],
  copyFiles: [
    {
      from: `${npmDir}/@bloomreach/dragula/dist/dragula.min.css`,
      to: `${targetDir}/styles`,
    },
    {
      from: `${npmDir}/@bloomreach/dragula/dist/dragula.min.js`,
      to: `${targetDir}/scripts`,
    },
  ],
  provide: {
    $: 'jquery',
    'window.$': 'jquery',
    'window.jQuery': 'jquery',
    'window.dragula': 'dragula',
  },
  hmr: true,
  serverPort: 9090,
  karma: {
    port: 10002,
    files: [
      'node_modules/@bloomreach/dragula/dist/dragula.min.js',
      'node_modules/@bloomreach/dragula/dist/dragula.min.css',
      'node_modules/jquery/dist/jquery.js',
    ],
    proxies: {
      '/styles/dragula.min.css': '/base/node_modules/@bloomreach/dragula/dist/dragula.min.css',
      '/scripts/dragula.min.js': '/base/node_modules/@bloomreach/dragula/dist/dragula.min.js',
    },
  },

  dlls: {
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
    ],
  },
};

module.exports = customConfig;
