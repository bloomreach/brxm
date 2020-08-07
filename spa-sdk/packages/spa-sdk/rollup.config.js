/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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
import dts from 'rollup-plugin-dts';
import { terser } from 'rollup-plugin-terser';
import typescript from 'rollup-plugin-typescript2';
import pkg from './package.json';
import terserOptions from './terser.json';

export default [
  {
    input: 'src/index.ts',
    output: [
      {
        exports: 'named',
        file: 'dist/index.js',
        format: 'umd',
        name: 'BloomreachSpaSdk',
        sourcemap: true,
        sourcemapExcludeSources: true,
      },
      {
        file: 'dist/index.mjs',
        format: 'esm',
      },
    ],
    external: [
      ...Object.keys(pkg.dependencies || {}),
      ...Object.keys(pkg.peerDependencies || {}),
    ],
    plugins: [
      typescript({
        cacheRoot: './node_modules/.cache/rpt2',
        useTsconfigDeclarationDir: true,
      }),
      babel({ extensions: ['.ts'] }),
      terser(terserOptions)
    ],
  },

  {
    input: 'src/index.ts',
    output: [{
      file: 'dist/index.es6.js',
      format: 'esm',
    }],
    external: [
      ...Object.keys(pkg.dependencies || {}),
      ...Object.keys(pkg.peerDependencies || {}),
    ],
    plugins: [
      typescript({
        cacheRoot: './node_modules/.cache/rpt2',
        tsconfigOverride: {
          compilerOptions: {
            declaration: false,
          }
        },
      }),
      terser(terserOptions)
    ],
  },

  {
    input: 'src/index.ts',
    output: [{
      file: 'dist/index.d.ts',
      format: 'es',
    }],
    external: [
      ...Object.keys(pkg.dependencies || {}),
      ...Object.keys(pkg.peerDependencies || {}),
    ],
    plugins: [
      dts(),
    ],
  },
];
