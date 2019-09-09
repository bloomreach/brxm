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
import { terser } from 'rollup-plugin-terser';

export default [
  {
    input: 'src/index.js',
    output: [
      {
        exports: 'named',
        file: 'dist/bloomreach-experience-react-sdk.js',
        format: 'umd',
        name: 'BloomreachReactSdk',
        sourcemap: true,
        sourcemapFile: 'dist/bloomreach-experience-react-sdk.js.map',
        globals: {
          react: 'React',
        },
      },
    ],
    plugins: [
      babel({ extensions: ['.js'] }),
      terser({
        ecma: 5,
        mangle: false,
        compress: false,
        output: {
          beautify: true,
          comments: false,
        },
      }),
    ],
  },
];
