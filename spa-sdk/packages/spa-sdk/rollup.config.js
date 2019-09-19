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
import dts from 'rollup-plugin-dts';
import { terser } from 'rollup-plugin-terser';
import typescript from 'rollup-plugin-typescript2';

export default [
  {
    input: 'src/index.ts',
    output: [
      {
        exports: 'named',
        file: 'dist/spa-sdk.js',
        format: 'umd',
        name: 'BloomreachSpaSdk',
      },
      {
        file: 'dist/spa-sdk.mjs',
        format: 'esm',
      },
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
    input: 'src/index.ts',
    output: [{
      file: 'dist/spa-sdk.es6.mjs',
      format: 'esm',
    }],
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
    input: 'src/index.ts',
    output: [
      {
        file: 'dist/spa-sdk.d.ts',
        format: 'es',
      },
    ],
    plugins: [ dts() ],
  }
];
