
// set test environment, currently used to trigger 'istanbul' plugin in .babelrc
process.env.ENV = process.env.NODE_ENV = 'test';

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
          'null',
        ],
      },
      {
        test: /\.js$/,
        exclude: [/(node_modules)/],
        loaders: [
          'ng-annotate',
          'babel',
        ],
      },
      {
        test: /\.html$/,
        exclude: /items.renderer.html/,
        loaders: [
          'html',
        ],
      },
    ],
  },
  devtool: 'inline-source-map',
};
