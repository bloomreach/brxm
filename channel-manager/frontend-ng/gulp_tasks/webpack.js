const gulp = require('gulp');
const webpack = require('webpack');
const WebpackDevServer = require('webpack-dev-server');

const webpackConf = require('../conf/webpack.conf');
const webpackDistConf = require('../conf/webpack-dist.conf');
const webpackDevServerConf = require('../conf/webpack-dev-server.conf.js');

function webpackWrapper(conf) {
  const compiler = webpack(conf);
  const server = new WebpackDevServer(compiler, webpackDevServerConf);
  server.listen(webpackConf.devServer.port);
}

gulp.task('webpack', () => {
  webpackWrapper(webpackConf);
});

gulp.task('webpack:dist', () => {
  webpackWrapper(webpackDistConf);
});

