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

const hippoBuild = require('hippo-build');
const customConfig = require('./build.conf.js');
const cfg = hippoBuild.buildConfig(customConfig);

module.exports = function karmaConfig(config) {
  const options = cfg.karma;

  options.systemjs.includeFiles = [
    `${cfg.bowerDir}/angular/angular.js`,
    `${cfg.bowerDir}/angular-animate/angular-animate.js`,
    `${cfg.bowerDir}/angular-aria/angular-aria.js`,
    `${cfg.bowerDir}/angular-material/angular-material.js`,
    `${cfg.bowerDir}/angular-ui-router/release/angular-ui-router.js`,
    `${cfg.bowerDir}/angular-translate/angular-translate.js`,
    `${cfg.bowerDir}/angular-translate-loader-static-files/angular-translate-loader-static-files.js`,
    `${cfg.bowerDir}/angular-mocks/angular-mocks.js`,
    `${cfg.bowerDir}/jquery/dist/jquery.js`,
    `${cfg.bowerDir}/velocity/velocity.js`,
    `${cfg.bowerDir}/dragula.js/dist/dragula.js`,
  ];

  options.files = [
    cfg.src.templates,
    cfg.src.scripts,
    `${cfg.srcDir}/angularjs/mock.environment.spec.js`,
    cfg.src.unitTests,
    cfg.src.fixtures,
    {
      pattern: `${cfg.bowerDir}/dragula.js/dist/dragula.min.+(css|js)`,
      included: false,
    },
  ];

  options.proxies['/styles/dragula.min.css'] = `/base/${cfg.bowerDir}/dragula.js/dist/dragula.min.css`;
  options.proxies['/scripts/dragula.min.js'] = `/base/${cfg.bowerDir}/dragula.js/dist/dragula.min.js`;

  config.set(options);
};
