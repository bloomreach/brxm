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

import { Typed } from 'emittery';
import { Events, CmsUpdateEvent } from './events';
import { HttpClient, HttpRequest } from './http';
import { ComponentFactory, ContentFactory, Content, PageModel, Page } from './page';
import { PageModelUrlBuilder, PageModelUrlOptions } from './url';

/**
 * Configuration of the SPA SDK.
 */
export interface Configuration {
  /**
   * HTTP client that will be used to fetch the page model.
   */
  httpClient: HttpClient<PageModel>;

  /**
   * Current user's request.
   */
  request: HttpRequest;

  /**
   * Options for generating the page model API URL.
   */
  options: PageModelUrlOptions;
}

/**
 * SPA entry point interacting with the Channel Manager and the Page Model API.
 */
export class Spa {
  /**
   * @param pageModelUrlBuilder Function generating an API URL based on the current request.
   * @param componentFactory Factory to produce component entities.
   * @param contentFactory Factory to produce content entities.
   * @param eventBus Event bus to exchange data between submodules.
   */
  constructor(
    private pageModelUrlBuilder: PageModelUrlBuilder,
    private componentFactory: ComponentFactory,
    private contentFactory: ContentFactory,
    protected eventBus: Typed<Events>,
  ) {}

  private async fetchPageModel(config: Configuration) {
    const url = this.pageModelUrlBuilder(config.request, config.options);

    return await config.httpClient({
      url,
      method: 'get',
      headers: config.request.headers,
    });
  }

  private async fetchComponentModel(config: Configuration, page: Page, id: string, properties: object) {
    const root = page.getComponent();
    const component = root.getComponentById(id);
    const url = component && component.getModelUrl();
    if (!url) {
      return;
    }

    return await config.httpClient({
      url,
      data: properties,
      method: 'post',
    });
  }

  private initializeRoot(model: PageModel) {
    return this.componentFactory.create(model.page);
  }

  private initializeContent(model: PageModel) {
    return new Map<string, Content>(
      Object.entries(model.content || {})
        .map(([alias, model]) => [
          alias,
          this.contentFactory.create(model),
        ]),
    );
  }

  private initializePage(model: PageModel) {
    return new Page(
      model,
      this.initializeRoot(model),
      this.initializeContent(model),
    );
  }

  protected async onCmsUpdate(config: Configuration, page: Page, event: CmsUpdateEvent) {
    const model = await this.fetchComponentModel(config, page, event.id, event.properties);
    if (!model) {
      return;
    }

    this.eventBus.emit('page.update', { page: this.initializePage(model) });
  }

  /**
   * Intitializes the SPA.
   * @param config Configuration of the SPA integration with brXM.
   */
  async initialize(config: Configuration) {
    const model = await this.fetchPageModel(config);
    const page = this.initializePage(model);

    this.eventBus.on('cms.update', this.onCmsUpdate.bind(this, config, page));

    return page;
  }
}
