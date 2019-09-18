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

import { Configuration } from './api';
import { ComponentFactory, ContentFactory, Content, PageModel, Page } from './page';
import { PageModelUrlBuilder } from './url';

/**
 * Initializes the page model.
 *
 * @param modelUrlBuilder Function generating an API URL based on the current request.
 * @param componentFactory Factory to produce component entities.
 * @param contentFactory Factory to produce content items.
 * @param config Configuration of the SPA integration with brXM.
 */
export async function initialize(
  modelUrlBuilder: PageModelUrlBuilder,
  componentFactory: ComponentFactory,
  contentFactory: ContentFactory,
  config: Configuration,
): Promise<Page> {
  const url = modelUrlBuilder(config.request, config.options);
  const model: PageModel = await config.httpClient({
    url,
    method: 'get',
    headers: config.request.headers,
  });
  const root = componentFactory.create(model.page);
  const content = new Map<string, Content>();

  if (model.content) {
    Object.entries(model.content)
      .forEach(([alias, model]) => content.set(alias, contentFactory.create(model)));
  }

  return new Page(model, root, content);
}
