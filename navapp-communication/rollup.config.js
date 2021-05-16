/*
 * Copyright 2019-2021 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import babel from '@rollup/plugin-babel';
import dts from 'rollup-plugin-dts';
import json from '@rollup/plugin-json';
import pkg from './package.json';
import { terser } from 'rollup-plugin-terser';
import typescript from '@rollup/plugin-typescript';

export default [
  {
    input: 'src/index.ts',
    output: [
      {
        file: pkg.browser,
        exports: 'named',
        format: 'umd',
        name: 'brNavappCommunication',
        sourcemap: true,
        globals: { 
          penpal: 'Penpal'
        }
      },
    ],
    external: [
      ...Object.keys(pkg.dependencies || {}),
      ...Object.keys(pkg.peerDependencies || {}),
    ],
    plugins: [
      typescript(),
      json({ compact: true }),
      babel({ babelHelpers: 'bundled', extensions: ['.ts'] }),
      terser(),
    ],
  },

  {
    input: 'src/index.ts',
    output: [{
      file: pkg.module,
      format: 'es',
      sourcemap: true,
    }],
    external: [
      ...Object.keys(pkg.dependencies || {}),
      ...Object.keys(pkg.peerDependencies || {}),
    ],
    plugins: [
      typescript(),
      json({ compact: true }),
    ],
  },

  {
    input: 'src/index.ts',
    output: [{
      file: pkg.types,
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
