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
import { Cms, Window } from './cms';
import { ComponentFactory, ContentFactory, Content, PageModel, PageImpl, Page } from './page';
import { Events, CmsUpdateEvent } from './events';
import { HttpClient, HttpRequest } from './http';
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

  /**
   * The window reference for the CMS integration.
   * By default the global window object will be used.
   */
  window?: Window;
}

/**
 * SPA entry point interacting with the Channel Manager and the Page Model API.
 */
export class Spa {
  private pages = new Map<Page, Configuration>();

  /**
   * @param pageModelUrlBuilder Function generating an API URL based on the current request.
   * @param componentFactory Factory to produce component entities.
   * @param contentFactory Factory to produce content entities.
   * @param eventBus Event bus to exchange data between submodules.
   * @param cms Cms integration instance.
   */
  constructor(
    private pageModelUrlBuilder: PageModelUrlBuilder,
    private componentFactory: ComponentFactory,
    private contentFactory: ContentFactory,
    protected eventBus: Typed<Events>,
    protected cms: Cms,
  ) {
    this.onCmsUpdate = this.onCmsUpdate.bind(this);
  }

  private async fetchPageModel(config: Configuration) {
    const url = this.pageModelUrlBuilder(config.request, config.options);
    const response = await config.httpClient({
      url,
      headers: config.request.headers,
      method: 'GET',
    });

    return response.data;
  }

  private async fetchComponentModel(config: Configuration, page: Page, id: string, properties: object) {
    const root = page.getComponent();
    const component = root.getComponentById(id);
    const url = component && component.getUrl();
    if (!url) {
      return;
    }

    const response = await config.httpClient({
      url,
      data: properties,
      method: 'POST',
    });

    return response.data;
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
    return new PageImpl(
      model,
      this.initializeRoot(model),
      this.initializeContent(model),
      this.eventBus,
    );
  }

  protected async onCmsUpdate(event: CmsUpdateEvent) {
    this.pages.forEach(async (config, page) => {
      const model = await this.fetchComponentModel(config, page, event.id, event.properties);
      if (!model) {
        return;
      }

      this.eventBus.emit('page.update', { page: this.initializePage(model) });
    });
  }

  /**
   * Intitializes the SPA.
   * @param config Configuration of the SPA integration with brXM.
   */
  async initialize(config: Configuration): Promise<Page> {
    this.cms.initialize(config.window);

    const model = await this.fetchPageModel(config);
    const page = this.initializePage(model);

    if (!this.pages.size) {
      this.eventBus.on('cms.update', this.onCmsUpdate);
    }

    this.pages.set(page, config);

    return page;
  }

  /**
   * Destroys the integration with the SPA page.
   * @param page Page instance to destroy.
   */
  destroy(page: Page) {
    this.pages.delete(page);

    if (!this.pages.size) {
      this.eventBus.off('cms.update', this.onCmsUpdate);
    }
  }
}
