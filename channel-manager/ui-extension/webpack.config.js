/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

const dts = require('dts-bundle');
const path = require('path');

class DtsBundlePlugin {
  apply(compiler) {
    compiler.hooks.done.tap({
      name: 'DtsBundlePlugin',
      stage: Infinity
    }, dts.bundle.bind(null, {
      name: 'ui-extension',
      main: './target/ui-extension.d.ts',
      out: 'ui-extension.d.ts',
      removeSource: true,
      outputAsModuleFolder: true,
    }));
  }
}

module.exports = {
  mode: 'production',
  entry: './src/ui-extension.ts',
  target: 'node',
  context: __dirname,
  resolve: {
    extensions: ['.ts'],
  },
  output: {
    path: path.resolve(__dirname, 'target'),
    libraryTarget: 'commonjs',
    filename: 'ui-extension.js',
  },
  module: {
    rules: [
      { test: /\.ts$/, use: 'ts-loader' },
    ],
  },
  plugins: [
    new DtsBundlePlugin(),
  ],
};
