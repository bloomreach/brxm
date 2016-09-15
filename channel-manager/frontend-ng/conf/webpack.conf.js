const pkg = require('../package.json');
const path = require('path');
const webpack = require('webpack');
const conf = require('./gulp.conf');

const HtmlWebpackPlugin = require('html-webpack-plugin');
const autoprefixer = require('autoprefixer');

const srcDir = conf.path.src;

module.exports = {
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
        test: /.json$/,
        loaders: [
          'json',
        ],
      },
      {
        test: /\.(css|scss)$/,
        loaders: [
          'style',
          'css?sourceMap',
          'postcss?sourceMap',
          'sass?sourceMap',
        ],
      },
      {
        test: /\.(eot|svg|ttf|woff|woff2|png)\w*/,
        loader: 'file',
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
      filename: 'vendor.bundle.js',
      minChunks: Infinity,
    }),
    new webpack.optimize.OccurrenceOrderPlugin(),
    new webpack.NoErrorsPlugin(),
    new HtmlWebpackPlugin({
      template: conf.path.src('index.html'),
      inject: true,
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
    path: path.join(process.cwd(), conf.paths.tmp),
    filename: 'index.js',
  },
  entry: {
    vendor: Object.keys(pkg.dependencies),
    index: `./${conf.path.src('index')}`,
  },
  resolve: {
    extensions: ['', '.js', '.scss'],
    root: [srcDir()],
  },
};
