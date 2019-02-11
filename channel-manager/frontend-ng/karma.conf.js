/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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

const fbConfig = require('@bloomreach/frontend-build/lib/karma.conf');

module.exports = (config) => {
  fbConfig(config);

  config.set({
    port: 10002,
    files: [
      ...config.files,
      'node_modules/@bloomreach/dragula/dist/dragula.min.js',
      'node_modules/@bloomreach/dragula/dist/dragula.min.css',
      'node_modules/jquery/dist/jquery.js',
    ],
    proxies: {
      '/styles/dragula.min.css': '/base/node_modules/@bloomreach/dragula/dist/dragula.min.css',
      '/scripts/dragula.min.js': '/base/node_modules/@bloomreach/dragula/dist/dragula.min.js',
    },
    reporters: [...config.reporters, 'junit'],
    junitReporter: {
      outputDir: 'target/surefire-reports',
    },
  });
};
