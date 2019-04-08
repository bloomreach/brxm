/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
      babel({ extensions: ['.ts'] }),
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
