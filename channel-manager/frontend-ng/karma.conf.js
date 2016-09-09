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

const frontendBuild = require('frontend-build');
const customConfig = require('./build.conf.js');
const cfg = frontendBuild.buildConfig(customConfig);

module.exports = function karmaConfig(config) {
  const options = cfg.karma;

  options.systemjs.includeFiles = [
    `${cfg.npmDir}/angular/angular.js`,
    `${cfg.npmDir}/angular-animate/angular-animate.js`,
    `${cfg.npmDir}/angular-aria/angular-aria.js`,
    `${cfg.npmDir}/angular-material/angular-material.js`,
    `${cfg.npmDir}/angular-messages/angular-messages.js`,
    `${cfg.npmDir}/angular-ui-router/release/angular-ui-router.js`,
    `${cfg.npmDir}/angular-ui-tree/dist/angular-ui-tree.min.js`,
    `${cfg.npmDir}/angular-translate/dist/angular-translate.js`,
    `${cfg.npmDir}/angular-translate-loader-static-files/angular-translate-loader-static-files.js`,
    `${cfg.npmDir}/angular-mocks/angular-mocks.js`,
    `${cfg.npmDir}/jquery/dist/jquery.js`,
    `${cfg.npmDir}/velocity-animate/velocity.js`,
    `${cfg.npmDir}/dragula/dist/dragula.js`,
    `${cfg.npmDir}/re-tree/re-tree.js`,
    `${cfg.npmDir}/ng-device-detector/ng-device-detector.js`,
    `${cfg.npmDir}/ng-focus-if/focusIf.js`,
  ];

  options.systemjs.config.map = cfg.systemjsOptions.map;

  options.files = [
    `${cfg.srcDir}/angularjs/mock.environment.spec.js`,
    cfg.src.fixtures,
    {
      pattern: `${cfg.npmDir}/dragula/dist/dragula.min.+(css|js)`,
      included: false,
    },
    {
      pattern: `${cfg.npmDir}/+(dom-autoscroller|create-point-cb|lodash.debounce)/index.js`,
      included: false,
    },
    {
      pattern: `${cfg.npmDir}/mutation-summary/src/mutation-summary.js`,
      included: false,
    },
  ].concat(options.files);

  options.proxies['/styles/dragula.min.css'] = `/base/${cfg.npmDir}/dragula/dist/dragula.min.css`;
  options.proxies['/scripts/dragula.min.js'] = `/base/${cfg.npmDir}/dragula/dist/dragula.min.js`;

  config.set(options);
};
