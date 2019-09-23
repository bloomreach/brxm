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

import { Typed } from 'emittery';
import { Events } from './events';
import { Cms, Window } from './cms';
import {
  ComponentFactory,
  ComponentImpl,
  ContainerItemImpl,
  ContainerItemModel,
  ContainerItem,
  ContainerImpl,
  ContainerModel,
  ContentFactoryImpl,
  ContentImpl,
  MetaCommentImpl,
  MetaFactory,
  Page,
  TYPE_COMPONENT,
  TYPE_COMPONENT_CONTAINER_ITEM,
  TYPE_COMPONENT_CONTAINER,
  TYPE_META_COMMENT,
} from './page';
import { Configuration, Spa } from './spa';
import { buildPageModelUrl } from './url';

declare const window: Window | undefined;

const eventBus = new Typed<Events>();

const metaFactory = new MetaFactory()
  .register(TYPE_META_COMMENT, (model, position) => new MetaCommentImpl(model, position));
const contentFactory = new ContentFactoryImpl(model => new ContentImpl(model, metaFactory.create(model._meta)));
const componentFactory = new ComponentFactory()
  .register(
    TYPE_COMPONENT,
    (model, children) => new ComponentImpl(model, children, metaFactory.create(model._meta)),
  )
  .register<ContainerModel, ContainerItem>(
    TYPE_COMPONENT_CONTAINER,
    (model, children) => new ContainerImpl(model, children, metaFactory.create(model._meta)),
  )
  .register<ContainerItemModel>(
    TYPE_COMPONENT_CONTAINER_ITEM,
    model => new ContainerItemImpl(model, eventBus, metaFactory.create(model._meta)),
  );

const cms = new Cms(eventBus, window);
const spa = new Spa(buildPageModelUrl, componentFactory, contentFactory, eventBus, cms);

/**
 * Initializes the page model.
 *
 * @param config Configuration of the SPA integration with brXM.
 */
export function initialize(config: Configuration): Promise<Page> {
  return spa.initialize(config);
}

/**
 * Destroys the integration with the SPA page.
 * @param page Page instance to destroy.
 */
export function destroy(page: Page) {
  return spa.destroy(page);
}

export {
  Component,
  ContainerItem,
  Container,
  Content,
  MetaComment,
  Meta,
  Page,
  isComponent,
  isContainerItem,
  isContainer,
  isMetaComment,
  isMeta,
  META_POSITION_BEGIN,
  META_POSITION_END,
  TYPE_COMPONENT,
  TYPE_COMPONENT_CONTAINER_ITEM,
  TYPE_COMPONENT_CONTAINER,
  TYPE_CONTAINER_BOX,
  TYPE_CONTAINER_INLINE,
  TYPE_CONTAINER_NO_MARKUP,
  TYPE_CONTAINER_ORDERED_LIST,
  TYPE_CONTAINER_UNORDERED_LIST,
  TYPE_META_COMMENT,
} from './page';
