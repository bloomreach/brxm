/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)

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

'use strict';

var pkg = require('./package.json');

/*
 * This module contains all configuration for the build process.
 */
var buildConfig = {
  /*
   * The `tmp_dir` folder is where we can store temporary files during compilation
   * by for example grunt-usemin. The `dist_dir` is where our src files are output
   * as concatenated, minified and otherwise optimized files.
   */
  dist_dir: 'target/dist',
  src_dir: 'src',
  tmp_dir: 'target/tmp',
  image_dir: 'src/images',
  demo_dir: 'demo',
  docs_dir: 'target/docs',
  components_dir: 'target/bower_components',

  /*
   * This is a collection of patterns.
   * These paths are used in the configuration of
   * build tasks.
   */
  images: 'src/images/**/*.{png,jpg,gif}',
  svg: 'src/images/**/*.svg',
  js: [
    'src/angularjs/**/*.js',
    '!src/angularjs/**/*.spec.js'
  ],
  mainjs: 'src/angularjs/main.js',
  unit: 'src/angularjs/**/*.spec.js',
  tpl: 'src/angularjs/**/*.tpl.html',
  jstplModule: pkg.name + '.templates',
  jstplModuleBase: 'src/angularjs/',
  jstpl: 'target/tmp/angularjs/' + pkg.name + '-templates.js'
};

module.exports = buildConfig;
