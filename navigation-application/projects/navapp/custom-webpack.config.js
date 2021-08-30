/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

const CopyPlugin = require('copy-webpack-plugin');
const WebpackAssetsManifest = require('webpack-assets-manifest');
const path = require('path');

const LOADER_JS_FILE_NAME = 'loader.js';

module.exports = (config) => {
  config.plugins.push(
    new WebpackAssetsManifest({
      output: 'filelist.json',
      customize: (entry) => {
        if (!entry.value.endsWith('.css') && !entry.value.endsWith('.js')) {
          return false;
        }

        if (entry.value.endsWith(LOADER_JS_FILE_NAME)) {
          return false;
        }

        return entry;
      },
    }),
    new CopyPlugin({ patterns: [path.resolve(__dirname, LOADER_JS_FILE_NAME)] })
  );

  return config;
};
