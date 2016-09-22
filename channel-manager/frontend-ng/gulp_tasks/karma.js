const path = require('path');

const gulp = require('gulp');
const karma = require('karma');

function karmaFinishHandler(done) {
  return failCount => {
    if (failCount) {
      done(new Error(`Failed ${failCount} tests.`));
    } else {
      done();
    }
  };
}

function karmaSingleRun(done) {
  const configFile = path.join(process.cwd(), 'conf', 'karma.conf.js');
  const karmaServer = new karma.Server({ configFile }, karmaFinishHandler(done));
  karmaServer.start();
}

function karmaAutoRun(done) {
  const configFile = path.join(process.cwd(), 'conf', 'karma-auto.conf.js');
  const karmaServer = new karma.Server({ configFile }, karmaFinishHandler(done));
  karmaServer.start();
}

gulp.task('karma:single-run', karmaSingleRun);
gulp.task('karma:auto-run', karmaAutoRun);
