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

/**
 * Main entry point of the spa-sdk library.
 * @module index
 */

import xmldom from 'xmldom';
import emittery from 'emittery';
import { Api, ApiImpl, Spa } from './spa';
import { Cms14Impl, CmsImpl, PostMessage } from './cms';
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
  LinkRewriterImpl,
  MetaCollectionImpl,
  MetaCollectionModel,
  MetaCommentImpl,
  MetaFactory,
  PageImpl,
  PageModel,
  Page,
  SingleTypeFactory,
  TYPE_COMPONENT,
  TYPE_COMPONENT_CONTAINER_ITEM,
  TYPE_COMPONENT_CONTAINER,
  TYPE_META_COMMENT,
  TYPE_LINK_INTERNAL,
  isPage,
} from './page';
import { Configuration, ConfigurationWithProxy, ConfigurationWithJwt, isConfigurationWithProxy } from './configuration';
import { Events } from './events';
import { UrlBuilder, UrlBuilderImpl, appendSearchParams, extractSearchParams, isMatched, parseUrl } from './url';

const DEFAULT_AUTHORIZATION_PARAMETER = 'token';
const DEFAULT_SERVER_ID_PARAMETER = 'server-id';

const eventBus = new emittery.Typed<Events>();
const postMessage = new PostMessage();
const cms14 = new Cms14Impl(eventBus);
const cms = new CmsImpl(eventBus, postMessage, postMessage);
const domParser = new xmldom.DOMParser();
const pages = new WeakMap<Page, Spa>();
const xmlSerializer = new xmldom.XMLSerializer();

function onReady<T, U>(value: T | Promise<T>, callback: (value: T) => U): U | Promise<U> {
  return value instanceof Promise
    ? value.then(callback)
    : callback(value);
}

function initializeSpa(api: Api, url: UrlBuilder) {
  const linkFactory = new LinkFactory()
    .register(TYPE_LINK_INTERNAL, url.getSpaUrl.bind(url));
  const linkRewriter = new LinkRewriterImpl(linkFactory, domParser, xmlSerializer);
  const metaFactory = new MetaFactory()
    .register(TYPE_META_COMMENT, (model, position) => new MetaCommentImpl(model, position));
  const metaCollectionFactory = new SingleTypeFactory(
    (model: MetaCollectionModel) => new MetaCollectionImpl(model, metaFactory),
  );
  const componentFactory = new ComponentFactory()
    .register(TYPE_COMPONENT, (model, children) => new ComponentImpl(
      model,
      children,
      linkFactory,
      metaCollectionFactory,
    ))
    .register(TYPE_COMPONENT_CONTAINER, (model, children) => new ContainerImpl(
      model as ContainerModel,
      children as ContainerItem[],
      linkFactory,
      metaCollectionFactory,
    ))
    .register(TYPE_COMPONENT_CONTAINER_ITEM, model => new ContainerItemImpl(
      model as ContainerItemModel,
      eventBus,
      linkFactory,
      metaCollectionFactory,
    ));
  const contentFactory = new SingleTypeFactory(model => new ContentImpl(model, linkFactory, metaCollectionFactory));
  const pageFactory = new SingleTypeFactory(model => new PageImpl(
    model,
    componentFactory.create(model.page),
    contentFactory,
    eventBus,
    linkFactory,
    linkRewriter,
    metaCollectionFactory,
  ));

  return new Spa(eventBus, api, pageFactory);
}

function initializeWithProxy(config: ConfigurationWithProxy, model?: PageModel) {
  const url =  new UrlBuilderImpl();
  const api = new ApiImpl(url);
  const spa = initializeSpa(api, url);
  const options = isMatched(config.request.path, config.options.preview.spaBaseUrl)
    ? config.options.preview
    : config.options.live;
  url.initialize(options);
  api.initialize(config);
  cms14.initialize(config);

  return spa.initialize(model ?? config.request.path);
}

function initializeWithJwt(config: ConfigurationWithJwt, model?: PageModel) {
  const url =  new UrlBuilderImpl();
  const api = new ApiImpl(url);
  const spa = initializeSpa(api, url);
  const authorizationParameter = config.authorizationQueryParameter || DEFAULT_AUTHORIZATION_PARAMETER;
  const serverIdParameter = config.serverIdQueryParameter || DEFAULT_SERVER_ID_PARAMETER;
  const { url: path, searchParams } = extractSearchParams(
    config.request.path,
    [authorizationParameter, serverIdParameter],
  );
  const authorizationToken = searchParams.get(authorizationParameter) || undefined;
  const serverId = searchParams.get(serverIdParameter) || undefined;

  url.initialize({
    ...config,
    spaBaseUrl: appendSearchParams(config.spaBaseUrl ?? '', searchParams),
  });
  api.initialize({ authorizationToken, serverId, ...config });

  return onReady(
    spa.initialize(model ?? path),
    (spa) => {
      if (spa.getPage()?.isPreview()) {
        const { origin } = parseUrl(config.cmsBaseUrl);
        postMessage.initialize({ ...config, origin });
        cms.initialize(config);
      }

      return spa;
    },
  );
}

/**
 * Initializes the page model.
 *
 * @param config Configuration of the SPA integration with brXM.
 * @param model Preloaded page model.
 */
export function initialize(config: Configuration, model: Page | PageModel): Page;

/**
 * Initializes the page model.
 *
 * @param config Configuration of the SPA integration with brXM.
 */
export async function initialize(config: Configuration): Promise<Page>;

export function initialize(config: Configuration, model?: Page | PageModel): Page | Promise<Page> {
  if (isPage(model)) {
    return model;
  }

  return onReady(
    isConfigurationWithProxy(config)
      ? initializeWithProxy(config, model)
      : initializeWithJwt(config, model),
    (spa) => {
      const page = spa.getPage()!;
      pages.set(page, spa);

      return page;
    },
  );
}

/**
 * Destroys the integration with the SPA page.
 * @param page Page instance to destroy.
 */
export function destroy(page: Page): void {
  return pages.get(page)?.destroy();
}

export { Configuration } from './configuration';
export {
  Component,
  ContainerItem,
  Container,
  Content,
  Link,
  Menu,
  MetaCollection,
  MetaComment,
  Meta,
  PageModel,
  Page,
  Reference,
  isComponent,
  isContainerItem,
  isContainer,
  isLink,
  isMetaComment,
  isMeta,
  isPage,
  isReference,
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
