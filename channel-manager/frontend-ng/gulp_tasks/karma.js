const path = require('path');
const gulp = require('gulp');
const { Server } = require('karma');

function karmaRun(config, singleRun) {
  const configFile = path.join(process.cwd(), 'conf', config);
  return (done) => {
    new Server({
      configFile,
      singleRun,
      autoWatch: !singleRun,
    }, failCount => {
      if (failCount === 0) {
        done();
      } else {
        done(new Error(`Failed ${failCount} tests.`));
      }
    }).start();
  };
}

gulp.task('karma:single-run', karmaRun('karma.conf.js', true));
gulp.task('karma:auto-run', karmaRun('karma.conf.js', false));
