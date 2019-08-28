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

/**
 * Main entry point of the spa-sdk library. Implements the public API defined in the
 * api module.
 * @module index
 * @see module:api
 */

import { Configuration, Page } from './api';
import { createPage } from './page';
import { buildPageModelUrl } from './url';

export * from './api';

/**
 * Initializes the page model.
 *
 * @param config configuration of the SPA integration with brXM.
 */
export async function initialize(config: Configuration): Promise<Page> {
  const url = buildPageModelUrl(config.request, config.options);
  const modelData = await config.httpClient({
    url,
    method: 'get',
    headers: config.request.headers,
  });
  return createPage(modelData);
}
