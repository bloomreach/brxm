/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

const fs = require('fs');
const path = require('path');

const FILES_ENCODING = 'utf8';
const VERSION_REGEX = /const version \= \'[\d.-]+\'\;/;

function createVersionDeclarationString(version) {
  return `const version = '${version}';`;
}

const packageJsonPath = process.argv[2];

if (!packageJsonPath) {
  throw new Error('The path to package.json must be set "node set-version.js path/to/package.json"');
}

const { version } = JSON.parse(fs.readFileSync(packageJsonPath, { encoding: FILES_ENCODING }));

const getVersionTsPath = path.join(path.dirname(packageJsonPath), 'src', 'lib', 'get-version.ts');
const getVersionTsContent = fs.readFileSync(getVersionTsPath, { encoding: FILES_ENCODING });

if (!getVersionTsContent.match(VERSION_REGEX)) {
  throw new Error(`The version declaration is not found in "${getVersionTsPath}". ` +
    `There must be version declaration matching to the regex ${VERSION_REGEX}.`);
}

const updatedGetVersionTsContent = getVersionTsContent.replace(VERSION_REGEX, createVersionDeclarationString(version));

fs.writeFileSync(getVersionTsPath, updatedGetVersionTsContent);
