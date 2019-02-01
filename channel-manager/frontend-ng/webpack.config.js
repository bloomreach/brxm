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

require('@bloomreach/frontend-build/lib/webpack/rules/files').exclude.push(/items.renderer.html/);

const config = require('@bloomreach/frontend-build/lib/webpack.config');
const {
  dist,
  env,
  opts,
  root,
  src,
} = require('@bloomreach/frontend-build/lib/env');

/* eslint import/no-extraneous-dependencies: "off" */
const merge = require('webpack-merge');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const { ProvidePlugin } = require('webpack');

module.exports = merge(config, {
  entry: {
    'vendor-styles': src('vendors'),
  },

  output: {
    publicPath: '',
  },

  plugins: [
    env !== 'test' && new ProvidePlugin({
      $: 'jquery',
      'window.$': 'jquery',
      'window.jQuery': 'jquery',
      'window.dragula': 'dragula',
    }),

    env !== 'test' && new CopyWebpackPlugin([
      {
        from: root('./node_modules/@bloomreach/dragula/dist/dragula.min.css'),
        to: dist('styles'),
      },
      {
        from: root('./node_modules/@bloomreach/dragula/dist/dragula.min.js'),
        to: dist('scripts'),
      },
    ]),
  ].filter(Boolean),

  ...opts.watch && {
    serve: {
      port: 9090,
    },
  },
});
