import babel from 'rollup-plugin-babel';
import commonjs from 'rollup-plugin-commonjs';
import { dts } from 'rollup-plugin-dts';
import resolve from 'rollup-plugin-node-resolve';
import { terser } from 'rollup-plugin-terser';
import typescript from 'rollup-plugin-typescript2';
import pkg from './package.json';

export default [
  {
    input: 'src/ui-extension.ts',
    output: [
      {
        exports: 'named',
        file: 'dist/ui-extension.min.js',
        format: 'iife',
        name: 'UiExtension',
        sourcemap: true,
        sourcemapFile: 'dist/ui-extension.min.js.map',
      },
    ],
    plugins: [
      resolve(),
      typescript({ cacheRoot: './node_modules/.cache/rpt2' }),
      commonjs(),
      babel({ extensions: ['.js', '.ts'] }),
      terser(),
    ],
  },

  {
    input: 'src/ui-extension.ts',
    output: [
      {
        exports: 'named',
        file: 'dist/ui-extension.js',
        format: 'umd',
        globals: {
          penpal: 'Penpal',
          emittery: 'Emittery',
        },
        name: 'UiExtension',
      },
      {
        file: 'dist/ui-extension.mjs',
        format: 'esm',
      },
    ],
    external: [
      ...Object.keys(pkg.dependencies || {}),
      ...Object.keys(pkg.peerDependencies || {}),
    ],
    plugins: [
      typescript({ cacheRoot: './node_modules/.cache/rpt2' }),
      babel({ extensions: ['.js', '.ts'] }),
      terser({
        ecma: 5,
        mangle: false,
        compress: false,
        output: {
          beautify: true,
          comments: false,
        },
        sourcemap: false,
      })
    ],
  },

  {
    input: 'src/ui-extension.ts',
    output: [{
      file: 'dist/ui-extension.es6.mjs',
      format: 'esm',
    }],
    external: [
      ...Object.keys(pkg.dependencies || {}),
      ...Object.keys(pkg.peerDependencies || {}),
    ],
    plugins: [
      typescript({ cacheRoot: './node_modules/.cache/rpt2' }),
      terser({
        ecma: 5,
        mangle: false,
        compress: false,
        output: {
          beautify: true,
          comments: false,
        },
        sourcemap: false,
      })
    ],
  },

  {
    input: 'src/ui-extension.ts',
    output: [
      {
        file: 'dist/ui-extension.d.ts',
        format: 'es',
      },
    ],
    plugins: [ dts() ],
  }
];
