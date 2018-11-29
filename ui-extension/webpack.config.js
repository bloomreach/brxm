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
const merge = require('webpack-merge');
const path = require('path');

class DtsBundlePlugin {
  apply(compiler) {
    compiler.hooks.done.tap({
      name: 'DtsBundlePlugin',
      stage: Infinity
    }, dts.bundle.bind(null, {
      name: 'ui-extension',
      main: './dist/ui-extension.d.ts',
      out: 'ui-extension.d.ts',
      removeSource: true,
      outputAsModuleFolder: true,
    }));
  }
}

const config = {
  mode: 'production',
  entry: './src/ui-extension.ts',
  context: __dirname,
  externals: {
    document: 'document',
    window: 'window',
  },
  resolve: {
    extensions: ['.ts', '.js', '.json'],
  },
  output: {
    path: path.resolve(__dirname, 'dist'),
    library: 'UiExtension',
  },
  module: {
    rules: [
      {
        enforce: 'pre',
        test: /\.ts$/,
        exclude: /node_modules/,
        use: 'tslint-loader?typeCheck=true&emitErrors=true',
      },
      { test: /\.ts$/, use: ['babel-loader', 'ts-loader'] },
      { test: /\.js$/, use: 'babel-loader' },
    ],
  },
  plugins: [
    new DtsBundlePlugin(),
  ],
  watchOptions: {
    ignored: ['**/*.d.ts', /node_modules/],
  }
};

module.exports = [
  merge(config, {
    name: 'es5',
    target: 'node',
    output: {
      filename: 'ui-extension.js',
      libraryTarget: 'umd',
    },
  }),

  merge(config, {
    name: 'bundle',
    target: 'web',
    output: {
      filename: 'ui-extension.min.js',
      libraryTarget: 'window',
    },
    devtool: 'source-map',
  }),
];
