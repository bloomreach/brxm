const gulp = require('gulp');
const webpack = require('webpack');
const WebpackDevServer = require('webpack-dev-server');

const webpackConf = require('../conf/webpack.conf');
const webpackDistConf = require('../conf/webpack-dist.conf');
const webpackDevServerConf = require('../conf/webpack-dev-server.conf.js');

function webpackWrapper(conf, startServer, done) {
  conf.entry.app.unshift(`webpack-dev-server/client?http://localhost:${webpackDevServerConf.port}/`, 'webpack/hot/dev-server');
  const compiler = webpack(conf);

  if (startServer) {
    const server = new WebpackDevServer(compiler, webpackDevServerConf);
    server.listen(webpackDevServerConf.port);
  } else {
    compiler.run(done);
  }
}

gulp.task('webpack:dev', done => {
  webpackWrapper(webpackConf, false, done);
});

gulp.task('webpack:dist', done => {
  webpackWrapper(webpackDistConf, false, done);
});

gulp.task('webpack:serve', done => {
  webpackWrapper(webpackConf, true, done);
});

gulp.task('webpack:distServe', done => {
  webpackWrapper(webpackDistConf, true, done);
});
