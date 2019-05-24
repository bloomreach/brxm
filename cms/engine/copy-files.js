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

const targetDir = 'target/classes/navapp';

// ----------------------------------------------------------------------------
// Files needed for the Navigation Application
// ----------------------------------------------------------------------------

fs.copySync('node_modules/@bloomreach/navapp/dist/navapp/', targetDir);

// TODO (meggermont): explain why
// Move the assets from navapp/navapp/assets to navapp/assets and then remove the empty dir
fs.copySync(targetDir + '/navapp/assets', targetDir  + '/assets', );
fs.rmdirSync(targetDir + '/navapp');



// ----------------------------------------------------------------------------
// Files needed for the navigation communication library used by the CMS
// ----------------------------------------------------------------------------

fs.copyFileSync('node_modules/penpal/dist/penpal.js', targetDir + "/penpal.js");
fs.copySync('node_modules/@bloomreach/navapp-communication/bundles/', targetDir);

