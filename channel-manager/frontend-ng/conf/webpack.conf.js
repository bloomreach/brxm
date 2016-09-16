const webpack = require('webpack');
const conf = require('./gulp.conf');
const path = require('path');

const HtmlWebpackPlugin = require('html-webpack-plugin');
const autoprefixer = require('autoprefixer');

module.exports = {
  module: {
    preLoaders: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        loader: 'eslint',
      },
    ],
    loaders: [
      {
        test: /.json$/,
        loaders: [
          'json',
        ],
      },
      {
        test: /\.scss$/,
        loaders: [
          'style',
          'css?sourceMap',
          'postcss?sourceMap',
          'sass?sourceMap',
        ],
      },
      {
        test: /\.js$/,
        exclude: /node_modules/,
        loaders: [
          'ng-annotate',
          'babel',
        ],
      },
      {
        test: /\.(eot|svg|ttf|woff|woff2|png)\w*/,
        loader: 'file',
      },
      {
        test: /.html$/,
        loaders: [
          'html',
        ],
      },
    ],
  },
  plugins: [
    new webpack.optimize.CommonsChunkPlugin({
      name: 'vendor',
      filename: 'vendor-[hash].js',
      minChunks: Infinity,
    }),
    new webpack.optimize.OccurrenceOrderPlugin(),
    new webpack.NoErrorsPlugin(),
    new HtmlWebpackPlugin({
      template: conf.path.src('index.html'),
      inject: true,
    }),
    new webpack.ProvidePlugin({
      $: 'jquery',
      'window.$': 'jquery',
      'window.jQuery': 'jquery',
      'window.dragula': 'dragula',
    }),
  ],
  postcss: [
    autoprefixer({
      browsers: [
        'last 1 Chrome versions',
        'last 1 Firefox versions',
        'Safari >= 8',
        'Explorer >= 11',
      ],
    }),
  ],
  debug: true,
  devtool: 'cheap-module-eval-source-map',
  output: {
    filename: '[name].[hash].js',
    path: path.join(process.cwd(), conf.paths.dist),
    publicPath: '/',
  },
  entry: {
    vendor: conf.vendors,
    app: `./${conf.path.src('index')}`,
  },
};
