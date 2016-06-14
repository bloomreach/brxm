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
const customConfig = {
  env: {
    maven: true,
  },
  distDir: targetDir,
  copyFiles: [
    {
      src: 'bower_components/dragula.js/dist/dragula.min.css',
      dest: `${targetDir}styles`,
    },
    {
      src: 'bower_components/dragula.js/dist/dragula.min.js',
      dest: `${targetDir}scripts`,
    },
    {
      src: 'src/styles/hippo-iframe.css',
      dest: `${targetDir}styles`,
    },
    {
      src: 'bower_components/open-sans-fontface/fonts/Regular/*',
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

module.exports = customConfig;
