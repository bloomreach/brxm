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
 * Main entry point of the spa-sdk library.
 * @module index
 */

import { Typed } from 'emittery';
import { Events } from './events';
import { Cms } from './cms';
import {
  ComponentFactory,
  ComponentImpl,
  ContainerItemImpl,
  ContainerItemModel,
  ContainerItem,
  ContainerImpl,
  ContainerModel,
  ContentImpl,
  LinkFactory,
  MetaCommentImpl,
  MetaFactory,
  PageImpl,
  Page,
  SingleTypeFactory,
  TYPE_COMPONENT,
  TYPE_COMPONENT_CONTAINER_ITEM,
  TYPE_COMPONENT_CONTAINER,
  TYPE_META_COMMENT,
  TYPE_LINK_EXTERNAL,
  TYPE_LINK_INTERNAL,
  TYPE_LINK_RESOURCE,
} from './page';
import { Configuration, Spa } from './spa';
import { UrlBuilderImpl } from './url';

const eventBus = new Typed<Events>();
const cms = new Cms(eventBus);
const pages = new WeakMap<Page, Spa>();

/**
 * Initializes the page model.
 *
 * @param config Configuration of the SPA integration with brXM.
 */
export async function initialize(config: Configuration): Promise<Page> {
  const urlBuilder =  new UrlBuilderImpl();
  const linkFactory = new LinkFactory()
    .register(TYPE_LINK_EXTERNAL, urlBuilder.getSpaUrl.bind(urlBuilder))
    .register(TYPE_LINK_INTERNAL, urlBuilder.getSpaUrl.bind(urlBuilder))
    .register(TYPE_LINK_RESOURCE, urlBuilder.getCmsUrl.bind(urlBuilder));
  const metaFactory = new MetaFactory()
    .register(TYPE_META_COMMENT, (model, position) => new MetaCommentImpl(model, position));
  const componentFactory = new ComponentFactory()
    .register(TYPE_COMPONENT, (model, children) => new ComponentImpl(model, children, linkFactory, metaFactory))
    .register(TYPE_COMPONENT_CONTAINER, (model, children) => new ContainerImpl(
      model as ContainerModel,
      children as ContainerItem[],
      linkFactory,
      metaFactory,
    ))
    .register(TYPE_COMPONENT_CONTAINER_ITEM, model => new ContainerItemImpl(
      model as ContainerItemModel,
      eventBus,
      linkFactory,
      metaFactory,
    ));
  const contentFactory = new SingleTypeFactory(model => new ContentImpl(model, linkFactory, metaFactory));
  const pageFactory = new SingleTypeFactory(model => new PageImpl(
    model,
    componentFactory.create(model.page),
    contentFactory,
    eventBus,
    linkFactory,
    metaFactory,
  ));

  const spa = new Spa(config, cms, eventBus, pageFactory, urlBuilder);
  const page = await spa.initialize();

  pages.set(page, spa);

  return page;
}

/**
 * Destroys the integration with the SPA page.
 * @param page Page instance to destroy.
 */
export function destroy(page: Page) {
  const spa = pages.get(page);

  return spa && spa.destroy();
}

export { Configuration } from './spa';
export {
  Component,
  ContainerItem,
  Container,
  Content,
  Link,
  Menu,
  MetaComment,
  Meta,
  Page,
  isComponent,
  isContainerItem,
  isContainer,
  isLink,
  isMetaComment,
  isMeta,
  META_POSITION_BEGIN,
  META_POSITION_END,
  TYPE_CONTAINER_BOX,
  TYPE_CONTAINER_INLINE,
  TYPE_CONTAINER_NO_MARKUP,
  TYPE_CONTAINER_ORDERED_LIST,
  TYPE_CONTAINER_UNORDERED_LIST,
  TYPE_LINK_EXTERNAL,
  TYPE_LINK_INTERNAL,
  TYPE_LINK_RESOURCE,
} from './page';
