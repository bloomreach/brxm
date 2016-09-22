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
      {
        test: /\.js$/,
        exclude: /(node_modules|.*\.spec\.js)/,
        loader: 'isparta',
      },
    ],
  },
  devtool: 'inline-source-map',
};
