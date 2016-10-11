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
const path = require('path');

const targetDir = path.resolve('./target/classes/angular/hippo-cm/');
const npmDir = path.resolve('./node_modules');

const customConfig = {
  dist: targetDir,
  publicPath: '/cms/angular/hippo-cm/',
  htmlExcludes: /items.renderer.html/,
  vendorExcludes: [
    'open-sans-fontface',
  ],
  copyFiles: [
    {
      from: `${npmDir}/dragula/dist/dragula.min.css`,
      to: `${targetDir}/styles`,
    },
    {
      from: `${npmDir}/dragula/dist/dragula.min.js`,
      to: `${targetDir}/scripts`,
    },
  ],
  provide: {
    $: 'jquery',
    'window.$': 'jquery',
    'window.jQuery': 'jquery',
    'window.dragula': 'dragula',
  },
  serverPort: 9090,
  karma: {
    files: [
      'node_modules/dragula/dist/dragula.min.js',
      'node_modules/dragula/dist/dragula.min.css',
      'node_modules/jquery/dist/jquery.js',
    ],
    proxies: {
      '/styles/dragula.min.css': '/base/node_modules/dragula/dist/dragula.min.css',
      '/scripts/dragula.min.js': '/base/node_modules/dragula/dist/dragula.min.js',
    },
  },
};

module.exports = customConfig;
