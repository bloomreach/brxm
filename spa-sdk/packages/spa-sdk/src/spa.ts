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
import { Cms } from './cms';
import { Factory, PageModel, Page } from './page';
import { Events, CmsUpdateEvent } from './events';
import { HttpClient, HttpRequest } from './http';
import { UrlBuilder, UrlBuilderOptions, isMatched } from './url';

/**
 * Configuration options for generating the page model URL.
 */
export interface UrlOptions {
  /**
   * URL mapping for the live page model.
   */
  live: UrlBuilderOptions;

  /**
   * URL mapping for the preview page model.
   */
  preview: UrlBuilderOptions;
}

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
  options: UrlOptions;

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
  private page?: Page;

  /**
   * @param config Configuration of the SPA integration with brXM.
   * @param cms Cms integration instance.
   * @param eventBus Event bus to exchange data between submodules.
   * @param pageFactory Factory to produce page instances.
   * @param urlBuilder API URL builder.
   */
  constructor(
    protected config: Configuration,
    protected cms: Cms,
    protected eventBus: Typed<Events>,
    private pageFactory: Factory<[PageModel], Page>,
    private urlBuilder: UrlBuilder,
  ) {
    this.onCmsUpdate = this.onCmsUpdate.bind(this);
  }

  private async fetchPageModel(url: string) {
    const { remoteAddress: ip } = this.config.request.connection || {};
    const { host, ...headers } = this.config.request.headers || {};
    const response = await this.config.httpClient({
      url,
      headers: {
        ...ip && { 'x-forwarded-for': ip },
        ...headers,
      },
      method: 'GET',
    });

    return response.data;
  }

  private async fetchComponentModel(url: string, payload: object) {
    const data = new URLSearchParams(payload as Record<string, string>);
    const response = await this.config.httpClient({
      url,
      data: data.toString(),
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      method: 'POST',
    });

    return response.data;
  }

  protected async onCmsUpdate(event: CmsUpdateEvent) {
    const root = this.page!.getComponent();
    const component = root.getComponentById(event.id);
    const url = component && component.getUrl();
    if (!url) {
      return;
    }

    const model = await this.fetchComponentModel(url, event.properties);

    this.eventBus.emit('page.update', { page: model });
  }

  /**
   * Intitializes the SPA.
   */
  async initialize(model?: PageModel): Promise<Page> {
    const options = isMatched(this.config.request.path, this.config.options.preview.spaBaseUrl)
      ? this.config.options.preview
      : this.config.options.live;
    this.urlBuilder.initialize(options);
    this.cms.initialize(this.config.window);

    const url = this.urlBuilder.getApiUrl(this.config.request.path);
    this.page = this.pageFactory.create(model || await this.fetchPageModel(url));

    this.eventBus.on('cms.update', this.onCmsUpdate);

    return this.page;
  }

  /**
   * Destroys the integration with the SPA page.
   */
  destroy() {
    this.eventBus.off('cms.update', this.onCmsUpdate);
    delete this.page;
  }
}
