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
 *
 */

const fs = require('file-system');

const sourceDir = 'node_modules/@bloomreach';
const targetDir = 'target/classes/org/hippoecm/frontend';

fs.recurse(sourceDir, ['**/bundles/*.(js|map)', 'navapp/dist/navapp/*.(js|map|css)'], (filepath, relative, filename) => {

  if (filename) {
    const destpath = `${targetDir}/${filename}`;
    fs.copyFile(filepath, destpath,
      {
        done: (err) => {
          if (err) {
            console.error(err);
          } else {
            console.log(`[npm] copied ${filename} to ${destpath}`);
          }
        }
      });
  }
});

fs.copyFile('node_modules/penpal/dist/penpal.js', targetDir + "/penpal.js");
