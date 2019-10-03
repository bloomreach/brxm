/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const fs = require('fs');
const path = require('path');
const dotenv = require('dotenv');

const dotenvPath = path.resolve(process.cwd(), '.env');
if (fs.existsSync(dotenvPath)) {
  const { error } = dotenv.confg({ path: dotenvPath });
  if (error) {
    throw error;
  }
}

module.exports = {
  assetPrefix: process.env.PUBLIC_URL || '/',
  publicRuntimeConfig: {
    brUrlLive: process.env.BR_URL_LIVE,
    spaBasePathLive: process.env.SPA_BASE_PATH_LIVE,
    brUrlPreview: process.env.BR_URL_PREVIEW,
    spaBasePathPreview: process.env.SPA_BASE_PATH_PREVIEW,
  },
};
