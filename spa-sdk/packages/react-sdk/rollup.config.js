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

/* eslint-disable import/no-extraneous-dependencies */
import babel from 'rollup-plugin-babel';
import dts from 'rollup-plugin-dts';
import { terser } from 'rollup-plugin-terser';
import typescript from 'rollup-plugin-typescript2';
import pkg from './package.json';
import minifyOptions from '../../terser.json';

export default [
  {
    input: 'src/index.ts',
    output: [
      {
        exports: 'named',
        file: 'dist/react-sdk.js',
        format: 'umd',
        name: 'BloomreachReactSdk',
        sourcemap: true,
        sourcemapFile: 'dist/react-sdk.js.map',
        globals: {
          react: 'React',
          '@bloomreach/spa-sdk': 'BloomreachSpaSdk',
        },
      },
      {
        file: 'dist/react-sdk.mjs',
        format: 'esm',
        globals: {
          react: 'React',
          '@bloomreach/spa-sdk': 'BloomreachSpaSdk',
        },
      },
    ],
    external: [
      ...Object.keys(pkg.dependencies || {}),
      ...Object.keys(pkg.peerDependencies || {}),
    ],
    plugins: [
      typescript({ cacheRoot: './node_modules/.cache/rpt2' }),
      babel({ extensions: ['.ts'] }),
      terser(minifyOptions),
    ],
  },

  {
    input: 'src/index.ts',
    output: [{
      file: 'dist/react-sdk.es6.mjs',
      format: 'esm',
    }],
    external: [
      ...Object.keys(pkg.dependencies || {}),
      ...Object.keys(pkg.peerDependencies || {}),
    ],
    plugins: [
      typescript({ cacheRoot: './node_modules/.cache/rpt2' }),
      terser(minifyOptions)
    ],
  },

  {
    input: 'src/index.ts',
    output: [
      {
        file: 'dist/react-sdk.d.ts',
        format: 'es',
      },
    ],
    plugins: [ dts() ],
  }
];
