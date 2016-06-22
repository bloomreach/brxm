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
const npmSources = [
  'es6-shim/es6-shim.js',
  'jquery/dist/jquery.js',
  'velocity-animate/velocity.js',
  'dragula/dist/dragula.js',
  'dragula/dist/dragula.css',
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
];

const customConfig = {
  env: {
    maven: true,
  },
  distDir: targetDir,
  copyFiles: [
    {
      src: 'node_modules/dragula/dist/dragula.min.css',
      dest: `${targetDir}styles`,
    },
    {
      src: 'node_modules/dragula/dist/dragula.min.js',
      dest: `${targetDir}scripts`,
    },
    {
      src: 'src/styles/hippo-iframe.css',
      dest: `${targetDir}styles`,
    },
    {
      src: 'node_modules/open-sans-fontface/fonts/Regular/*',
      dest: `${targetDir}fonts/Regular`,
    },
  ],
  systemjsOptions: {
    transpiler: 'babel',
    defaultJSExtensions: true,
    map: {
      'dom-autoscroller': 'node_modules/dom-autoscroller/index.js',
      'pointer-point': 'node_modules/pointer-point/index.js',
      'more-events': 'node_modules/more-events/index.js',
    },
  },
};

npmSources.forEach((src) => {
  const last = src.lastIndexOf('/');
  const path = last > -1 ? src.substring(0, last) : '';
  customConfig.copyFiles.push({
    src: `node_modules/${src}`,
    dest: `${targetDir}node_modules/${path}`,
  });
});

module.exports = customConfig;
