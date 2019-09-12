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
import { ComponentFactory, ContentFactory, ContentMap, PageModel, Page } from './page';
import { PageModelUrlBuilder } from './url';

/**
 * SPA entry point interacting with the Channel Manager and the Page Model API.
 */
export class Spa {
  /**
   * @param pageModelUrlBuilder Function generating an API URL based on the current request.
   * @param componentFactory Factory to produce component entities.
   * @param contentFactory Factory to produce content entities.
   * @param content Content storage.
   */
  constructor(
    private pageModelUrlBuilder: PageModelUrlBuilder,
    private componentFactory: ComponentFactory,
    private contentFactory: ContentFactory,
    protected content: ContentMap,
  ) {}

  private async fetchModel(config: Configuration): Promise<PageModel> {
    const url = this.pageModelUrlBuilder(config.request, config.options);

    return await config.httpClient({
      url,
      method: 'get',
      headers: config.request.headers,
    });
  }

  private initializeRoot(model: PageModel) {
    return this.componentFactory.create(model.page);
  }

  private initializeContent(model: PageModel) {
    if (!model.content) {
      return;
    }

    Object.entries(model.content)
      .forEach(([alias, model]) => this.content.set(
        alias,
        this.contentFactory.create(model),
      ));
  }

  /**
   * Intitializes the SPA.
   * @param config Configuration of the SPA integration with brXM.
   */
  async initialize(config: Configuration) {
    const model = await this.fetchModel(config);
    const root = this.initializeRoot(model);
    this.initializeContent(model);

    return new Page(model, root, this.content);
  }
}
