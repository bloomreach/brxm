const gulp = require('gulp');
const webpack = require('webpack');
const WebpackDevServer = require('webpack-dev-server');

const webpackConf = require('../conf/webpack.conf');
const webpackDistConf = require('../conf/webpack-dist.conf');
const webpackDevServerConf = require('../conf/webpack-dev-server.conf.js');

function webpackWrapper(conf) {
  conf.entry.app.unshift(`webpack-dev-server/client?http://localhost:${webpackDevServerConf.port}/`, 'webpack/hot/dev-server');
  const compiler = webpack(conf);
  const server = new WebpackDevServer(compiler, webpackDevServerConf);
  server.listen(webpackDevServerConf.port);
}

gulp.task('webpack', () => {
  webpackWrapper(webpackConf);
});

gulp.task('webpack:dist', () => {
  webpackWrapper(webpackDistConf);
});

