/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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

 const fs = require('fs');

require('@bloomreach/frontend-build/lib/webpack/rules/files').exclude.push(/items.renderer.html/);

const config = require('@bloomreach/frontend-build/lib/webpack.config');
const {
  dist,
  env,
  root,
  src,
} = require('@bloomreach/frontend-build/lib/env');

const uiProjectBasePath = '../ui/dist/ui/';
const uiProjectOutputFolder = './';

/* eslint import/no-extraneous-dependencies: "off" */
const merge = require('webpack-merge');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const { ProvidePlugin } = require('webpack');

function extractAngularResources(pathToIndexHtmlFile, outputBasePath) {
  let content;

  try {
    content = fs.readFileSync(pathToIndexHtmlFile);
  } catch {
    throw new Error(`Unable to read ${pathToIndexHtmlFile}. Make sure ui project is built first.`);
  }

  const resourceRegEx = /[\w]+\.([\w]+\.)?(js|css)/g;
  const resources = content.toString().match(resourceRegEx).map(x => `${uiProjectOutputFolder}${x}`);

  return {
    uiCss: resources.filter(x => x.endsWith('.css')),
    uiJs: resources.filter(x => x.endsWith('.js')),
  };
}

const uiProjectResources = extractAngularResources(uiProjectBasePath + 'index.html');

const webpackConfig = merge.strategy({ plugins: 'replace' })(config, {
  entry: {
    'vendor-styles': src('vendors'),
    iframe: src('app/iframe'),
  },

  output: {
    publicPath: '',
    jsonpFunction: 'hippoCmAngularJSwebpackJsonp',
  },

  optimization: {
    runtimeChunk: {
      name: entrypoint => (entrypoint.name === 'iframe' ? 'iframe' : config.optimization.runtimeChunk.name),
    },
    splitChunks: {
      chunks: chunk => chunk.name !== 'iframe',
      cacheGroups: {
        packages: {
          name: 'packages',
          test: /[\\/]node_modules[\\/]/,
        },
      },
    },
  },

  resolve: {
    alias: {
      'sortablejs': 'sortablejs/Sortable.js',
    },
  },

  module: {
    rules: [
      {
        test: /babel-plugin-transform-async-to-promises/,
        use: {
          loader: 'string-replace-loader',
          options: {
            search: 'Promise\\.(resolve|reject)',
            replace: '$Promise.$1',
            flags: 'g',
          },
        },
      },
    ],
  },

  plugins: [
    ...config.plugins.filter(plugin => !(plugin instanceof HtmlWebpackPlugin)),

    env !== 'test' && new ProvidePlugin({
      $: 'jquery',
      angular: [src('app/angular-no-conflict.js'), 'default'],
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

    env !== 'test' && new HtmlWebpackPlugin({
      template: src('index.ejs'),
      inject: false,
      templateParameters: (compilation, assets, assetTags, options) => ({
        compilation,
        webpackConfig: compilation.options,
        htmlWebpackPlugin: {
          tags: assetTags,
          files: assets,
          options
        },
        ...uiProjectResources,
      }),
      minify: {
        html5: true,
        removeComments: env === 'prod',
        collapseWhitespace: env === 'prod',
        preserveLineBreaks: true,
        decodeEntities: true,
      },
    }),

    new CopyWebpackPlugin([
      { from: uiProjectBasePath, to: uiProjectOutputFolder },
    ]),
  ].filter(Boolean),
});

if (env === 'test') {
  delete webpackConfig.entry;
}

module.exports = webpackConfig;
