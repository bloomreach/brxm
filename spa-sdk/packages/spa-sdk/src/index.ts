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

import { initialize as initializePageModel } from './initialize';
import { buildPageModelUrl } from './url';
import {
  ComponentFactory,
  Component,
  ContainerItemModel,
  ContainerItem,
  ContainerModel,
  Container,
  ContentModel,
  Content,
  TYPE_COMPONENT,
  TYPE_COMPONENT_CONTAINER_ITEM,
  TYPE_COMPONENT_CONTAINER,
} from './page';

const contentFactory = (model: ContentModel) => new Content(model);
const componentFactory = new ComponentFactory()
  .register(TYPE_COMPONENT, (model, children) => new Component(model, children))
  .register<ContainerModel, ContainerItem>(
    TYPE_COMPONENT_CONTAINER,
    (model, children) => new Container(model, children),
  )
  .register<ContainerItemModel>(TYPE_COMPONENT_CONTAINER_ITEM, model => new ContainerItem(model));

/**
 * Initializes the page model.
 *
 * @param config Configuration of the SPA integration with brXM.
 */
export const initialize = initializePageModel.bind(null, buildPageModelUrl, componentFactory, contentFactory);
export * from './api';
export {
  Component,
  ContainerItem,
  Container,
  Page,
  TYPE_COMPONENT,
  TYPE_COMPONENT_CONTAINER_ITEM,
  TYPE_COMPONENT_CONTAINER,
  TYPE_CONTAINER_BOX,
  TYPE_CONTAINER_INLINE,
  TYPE_CONTAINER_NO_MARKUP,
  TYPE_CONTAINER_ORDERED_LIST,
  TYPE_CONTAINER_UNORDERED_LIST,
} from './page';
