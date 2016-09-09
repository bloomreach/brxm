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

const targetDir = 'target/classes/angular/hippo-cm/';
const npmDir = 'node_modules';

const customConfig = {
  env: {
    maven: true,
  },
  distDir: targetDir,
  dependencies: [
    'es6-shim/es6-shim.js',
    'jquery/dist/jquery.js',
    'velocity-animate/velocity.js',
    'dragula/dist/dragula.css',
    'dragula/dist/dragula.js',
    'angular/angular.js',
    'angular-animate/angular-animate.js',
    'angular-aria/angular-aria.js',
    'angular-material/angular-material.js',
    'angular-messages/angular-messages.js',
    'angular-translate/dist/angular-translate.js',
    'angular-translate-loader-static-files/angular-translate-loader-static-files.js',
    'angular-ui-router/release/angular-ui-router.js',
    'angular-ui-tree/dist/angular-ui-tree.min.js',
    're-tree/re-tree.js',
    'ng-device-detector/ng-device-detector.js',
    'ng-focus-if/focusIf.js',
  ],
  copyFiles: [
    {
      src: `${npmDir}/dragula/dist/dragula.min.css`,
      dest: `${targetDir}styles`,
    },
    {
      src: `${npmDir}/dragula/dist/dragula.min.js`,
      dest: `${targetDir}scripts`,
    },
    {
      src: 'src/styles/hippo-iframe.css',
      dest: `${targetDir}styles`,
    },
    {
      src: `${npmDir}/open-sans-fontface/fonts/Regular/*`,
      dest: `${targetDir}fonts/Regular`,
    },
  ],
  systemjsOptions: {
    transpiler: 'babel',
    defaultJSExtensions: true,
    map: {
      'dom-autoscroller': `${npmDir}/dom-autoscroller/index.js`,
      'create-point-cb': `${npmDir}/create-point-cb/index.js`,
      'lodash.debounce': `${npmDir}/lodash.debounce/index.js`,
      'mutation-summary': `${npmDir}/mutation-summary/src/mutation-summary.js`,
    },
  },
};

module.exports = customConfig;
