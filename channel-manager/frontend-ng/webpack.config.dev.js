
const path = require('path');
const webpack = require('webpack');
const autoprefixer = require('autoprefixer');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');

const npmDir = path.resolve(__dirname, 'node_modules');
const srcDir = path.resolve(__dirname, 'src');
const distDir = path.resolve(__dirname, 'dist');
const publicDir = path.resolve(distDir, 'public');

module.exports = {
  entry: {
    index: './src/index.js',
    vendor: [
      'velocity-animate',
      'dragula',
      'angular',
      'angular-animate',
      'angular-aria',
      'angular-material',
      'angular-messages',
      'angular-translate',
      'angular-translate-loader-static-files',
      'angular-ui-router',
      'angular-ui-tree',
      're-tree',
      'ng-device-detector',
      'ng-focus-if',
    ],
  },
  debug: true,
  devtool: 'cheap-module-eval-source-map',
  devServer: {},
  module: {
    preLoaders: [
      {
        test: /\.js?$/,
        loader: 'eslint',
        exclude: /node_modules/,
      },
    ],
    loaders: [
      {
        test: /\.scss$/,
        loaders: ['style', 'css?sourceMap', 'postcss?sourceMap', `sass?sourceMap&includePaths[]=${srcDir}`],
      },
      {
        test: /\.js$/,
        loaders: ['ng-annotate', 'nginject?deprecate', 'babel?{"presets":["es2015"]}'],
        exclude: /(node_modules)/,
      },
      {
        test: /\.(eot|svg|ttf|woff|woff2|png)\w*/,
        loader: 'file',
      },
      {
        test: /\.html$/,
        loader: 'ngtemplate!html',
        exclude: /(index.html)/,
      },
    ],
  },
  output: {
    path: distDir,
    filename: '[name].bundle.js',
    chunkFilename: '[id].bundle.js',
    publicPath: '/cms/angular/hippo-cm',
  },
  postcss: [
    autoprefixer({
      browsers: ['last 5 versions'],
    }),
  ],
  plugins: [
    new webpack.ProvidePlugin({
      $: 'jquery',
      'window.$': 'jquery',
      'window.jQuery': 'jquery',
      'window.dragula': 'dragula',
    }),
    new webpack.optimize.CommonsChunkPlugin('vendor', 'vendor.bundle.js', Infinity),
    new HtmlWebpackPlugin({
      pushState: true,
      filename: 'index.html',
      inject: 'body',
      template: 'src/index.html',
      hash: false,
    }),
    new CopyWebpackPlugin([{
      from: path.resolve(srcDir, 'i18n'),
      to: path.resolve(publicDir, 'i18n'),
    }, {
      from: path.resolve(npmDir, 'dragula', 'dist', 'dragula.min.js'),
      to: path.resolve(publicDir, 'scripts', 'dragula.min.js'),
    }, {
      from: path.resolve(npmDir, 'dragula', 'dist', 'dragula.min.css'),
      to: path.resolve(publicDir, 'styles', 'dragula.min.css'),
    }, {
      from: path.resolve(srcDir, 'styles', 'hippo-iframe.css'),
      to: path.resolve(publicDir, 'styles', 'hippo-iframe.css'),
    }, {
      from: path.resolve(srcDir, 'images'),
      to: path.resolve(publicDir, 'images'),
    }]),
  ],
  resolve: {
    extensions: ['', '.js', '.scss'],
    root: [srcDir],
  },
};
