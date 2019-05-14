// This file is required by karma.conf.js and loads recursively all the .spec and framework files

// We find all the tests.
const context = (require as any).context('./', true, /\.spec\.ts$/);
// And load the modules.
context.keys().map(context);
