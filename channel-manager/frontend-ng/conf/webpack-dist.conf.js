const webpack = require('webpack');
const conf = require('./gulp.conf');
const path = require('path');

const HtmlWebpackPlugin = require('html-webpack-plugin');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const pkg = require('../package.json');
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
        test: /\.(css|scss)$/,
        loaders: ExtractTextPlugin.extract({
          fallbackLoader: 'style',
          loader: 'css?minimize!postcss!sass',
        }),
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
    new webpack.optimize.OccurrenceOrderPlugin(),
    new webpack.NoErrorsPlugin(),
    new HtmlWebpackPlugin({
      template: conf.path.src('index.html'),
      inject: true,
    }),
    new webpack.optimize.UglifyJsPlugin({
      compress: { unused: true, dead_code: true },
    }),
    new ExtractTextPlugin('index-[contenthash].css'),
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
  output: {
    path: path.join(process.cwd(), conf.paths.dist),
    filename: '[name]-[hash].js',
    publicPath: '/cms/angular/hippo-cm',
  },
  debug: true,
  devtool: 'cheap-module-eval-source-map',
  entry: {
    vendor: Object.keys(pkg.dependencies),
    app: `./${conf.path.src('index')}`,
  },
};
