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

import { Typed } from 'emittery';
import { CmsUpdateEvent, Events } from '../events';
import { Factory, PageModel, Page } from '../page';
import { Api } from './api';

/**
 * SPA entry point interacting with the Channel Manager and the Page Model API.
 */
export class Spa {
  private page?: Page;

  /**
   * @param eventBus Event bus to exchange data between submodules.
   * @param pageFactory Factory to produce page instances.
   * @param urlBuilder API URL builder.
   */
  constructor(
    protected eventBus: Typed<Events>,
    private api: Api,
    private pageFactory: Factory<[PageModel], Page>,
  ) {
    this.onCmsUpdate = this.onCmsUpdate.bind(this);
  }

  protected async onCmsUpdate(event: CmsUpdateEvent) {
    const root = this.page!.getComponent();
    const component = root.getComponentById(event.id);
    const url = component && component.getUrl();
    if (!url) {
      return;
    }

    const model = await this.api.getComponent(url, event.properties);

    this.eventBus.emit('page.update', { page: model });
  }

  /**
   * Initializes the SPA.
   * @param path Page path.
   * @param model Preloaded page model.
   */
  async initialize(path: string, model?: PageModel): Promise<Page> {
    this.page = this.pageFactory.create(model || await this.api.getPage(path));

    if (this.page.isPreview()) {
      this.eventBus.on('cms.update', this.onCmsUpdate);
    }

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
