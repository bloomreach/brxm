const dts = require('dts-bundle');
const path = require('path');

class DtsBundlePlugin {
  apply(compiler) {
    compiler.hooks.done.tap({
      name: 'DtsBundlePlugin',
      stage: Infinity
    }, dts.bundle.bind(null, {
      name: 'ui-extension',
      main: './target/index.d.ts',
      out: 'index.d.ts',
      removeSource: true,
      outputAsModuleFolder: true,
    }));
  }
}

module.exports = {
  mode: 'production',
  entry: './src/index.ts',
  target: 'node',
  context: __dirname,
  resolve: {
    extensions: ['.ts'],
  },
  output: {
    path: path.resolve(__dirname, 'target'),
    libraryTarget: 'commonjs',
    filename: 'index.js',
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
