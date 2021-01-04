/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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

import { inject, injectable, optional } from 'inversify';
import { ApiService, Api } from './api';
import { CmsUpdateEvent, EventBusService as CmsEventBusService, EventBus as CmsEventBus } from '../cms';
import { EventBusService, EventBus, PageFactory, PageModel, Page } from '../page';
import { Logger } from '../logger';

export const SpaService = Symbol.for('SpaService');

/**
 * SPA entry point interacting with the Channel Manager and the Page Model API.
 */
@injectable()
export class Spa {
  private page?: Page;

  /**
   * @param eventBus Event bus to exchange data between submodules.
   * @param api Api client.
   * @param pageFactory Factory to produce page instances.
   */
  constructor(
    @inject(ApiService) private api: Api,
    @inject(PageFactory) private pageFactory: PageFactory,
    @inject(CmsEventBusService) @optional() private cmsEventBus?: CmsEventBus,
    @inject(EventBusService) @optional() private eventBus?: EventBus,
    @inject(Logger) @optional() private logger?: Logger,
  ) {
    this.onCmsUpdate = this.onCmsUpdate.bind(this);
  }

  protected async onCmsUpdate(event: CmsUpdateEvent) {
    this.logger?.debug('Recieved CMS update event.');
    this.logger?.debug('Event:', event);

    const root = this.page!.getComponent();
    const component = root.getComponentById(event.id);
    const url = component?.getUrl();
    if (!url) {
      this.logger?.debug('Skipping the update event.');

      return;
    }

    this.logger?.debug('Trying to request the component model.');
    const model = await this.api.getComponent(url, event.properties);
    this.logger?.debug('Model:', model);

    this.eventBus?.emit('page.update', { page: model });
  }

  /**
   * Initializes the SPA.
   * @param model A preloaded page model or URL to a page model.
   */
  initialize(model: PageModel | string): Page | Promise<Page> {
    if (typeof model === 'string') {
      this.logger?.debug('Trying to request the page model.');

      return this.api.getPage(model)
        .then(this.hydrate.bind(this));
    }

    this.logger?.debug('Received dehydrated model.');

    return this.hydrate(model);
  }

  private hydrate(model: PageModel) {
    this.logger?.debug('Model:', model);
    this.logger?.debug('Hydrating.');

    this.page = this.pageFactory(model);

    if (this.page.isPreview()) {
      this.cmsEventBus?.on('cms.update', this.onCmsUpdate);
    }

    return this.page;
  }

  /**
   * Destroys the integration with the SPA page.
   */
  destroy() {
    this.cmsEventBus?.off('cms.update', this.onCmsUpdate);
    this.eventBus?.clearListeners();
    delete this.page;

    this.logger?.debug('Destroyed page.');
  }
}
