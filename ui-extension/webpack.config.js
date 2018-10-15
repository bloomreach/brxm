const dts = require('dts-bundle');
const path = require('path');

class DtsBundlePlugin {
  apply(compiler) {
    compiler.hooks.done.tap({
      name: 'DtsBundlePlugin',
      stage: Infinity
    }, dts.bundle.bind(null, {
      name: 'ui-extension',
      main: './target/ui-extension.d.ts',
      out: 'ui-extension.d.ts',
      removeSource: true,
      outputAsModuleFolder: true,
    }));
  }
}

module.exports = {
  mode: 'production',
  entry: './src/ui-extension.ts',
  target: 'node',
  context: __dirname,
  resolve: {
    extensions: ['.ts'],
  },
  output: {
    path: path.resolve(__dirname, 'target'),
    libraryTarget: 'commonjs',
    filename: 'ui-extension.js',
  },
  module: {
    rules: [
      { test: /\.ts$/, use: 'ts-loader' },
    ],
  },
  plugins: [
    new DtsBundlePlugin(),
  ],
};
