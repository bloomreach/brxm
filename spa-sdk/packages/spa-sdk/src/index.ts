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

import { Configuration } from './api';
import { buildPageModelUrl } from './url';
import {
  ComponentFactory,
  Component,
  ContainerItemModel,
  ContainerItem,
  ContainerModel,
  Container,
  PageModel,
  Page,
  TYPE_COMPONENT,
  TYPE_COMPONENT_CONTAINER_ITEM,
  TYPE_COMPONENT_CONTAINER,
} from './page';

export * from './api';

/**
 * Initializes the page model.
 *
 * @param config configuration of the SPA integration with brXM.
 */
export async function initialize(config: Configuration): Promise<Page> {
  const url = buildPageModelUrl(config.request, config.options);
  const factory = new ComponentFactory()
    .register(TYPE_COMPONENT, (model, children) => new Component(model, children))
    .register<ContainerModel, ContainerItem>(
      TYPE_COMPONENT_CONTAINER,
      (model, children) => new Container(model, children),
    )
    .register<ContainerItemModel>(TYPE_COMPONENT_CONTAINER_ITEM, model => new ContainerItem(model));

  const pageModel = await config.httpClient({
    url,
    method: 'get',
    headers: config.request.headers,
  }) as PageModel;
  const root = factory.create(pageModel.page);

  return new Page(pageModel, root);
}
