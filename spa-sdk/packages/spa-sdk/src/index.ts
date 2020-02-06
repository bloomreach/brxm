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

import { DOMParser, XMLSerializer } from 'xmldom';
import { Typed } from 'emittery';
import { ApiImpl, Spa } from './spa';
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
} from './page';
import { Configuration, isConfigurationWithProxy } from './configuration';
import { Events } from './events';
import { UrlBuilderImpl, appendSearchParams, extractSearchParams, isMatched, parseUrl } from './url';

const DEFAULT_AUTHORIZATION_PARAMETER = 'token';
const DEFAULT_SERVER_ID_PARAMETER = 'serverid';

const eventBus = new Typed<Events>();
const postMessage = new PostMessage();
const cms14 = new Cms14Impl(eventBus);
const cms = new CmsImpl(eventBus, postMessage, postMessage);
const domParser = new DOMParser();
const pages = new WeakMap<Page, Spa>();
const xmlSerializer = new XMLSerializer();

/**
 * Initializes the page model.
 *
 * @param config Configuration of the SPA integration with brXM.
 * @param model Preloaded page model.
 */
export async function initialize(config: Configuration, model?: PageModel): Promise<Page> {
  const urlBuilder =  new UrlBuilderImpl();
  const api = new ApiImpl(urlBuilder);
  const linkFactory = new LinkFactory()
    .register(TYPE_LINK_INTERNAL, urlBuilder.getSpaUrl.bind(urlBuilder));
  const linkRewriter = new LinkRewriterImpl(linkFactory, domParser, xmlSerializer);
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
    linkRewriter,
    metaFactory,
  ));
  const spa = new Spa(eventBus, api, pageFactory);

  if (isConfigurationWithProxy(config)) {
    const options = isMatched(config.request.path, config.options.preview.spaBaseUrl)
      ? config.options.preview
      : config.options.live;
    urlBuilder.initialize(options);
    api.initialize(config);
    cms14.initialize(config);

    const page = await spa.initialize(config.request.path, model);
    pages.set(page, spa);

    return page;
  }

  const authorizationParameter = config.authorizationQueryParameter || DEFAULT_AUTHORIZATION_PARAMETER;
  const serverIdParameter = config.serverIdQueryParameter || DEFAULT_SERVER_ID_PARAMETER;
  const { url: path, searchParams } = extractSearchParams(
    config.request.path,
    [authorizationParameter, serverIdParameter],
  );
  const authorizationToken = searchParams.get(authorizationParameter) || undefined;
  const serverId = searchParams.get(serverIdParameter) || undefined;

  urlBuilder.initialize({
    ...config,
    spaBaseUrl: appendSearchParams(config.spaBaseUrl || '', searchParams),
  });
  api.initialize({ authorizationToken, serverId, ...config });

  const page = await spa.initialize(path, model);
  if (page.isPreview()) {
    const { origin } = parseUrl(config.cmsBaseUrl);
    postMessage.initialize({ ...config, origin });
    cms.initialize();
  }

  pages.set(page, spa);

  return page;
}

/**
 * Destroys the integration with the SPA page.
 * @param page Page instance to destroy.
 */
export function destroy(page: Page): void {
  const spa = pages.get(page);

  return spa && spa.destroy();
}

export { Configuration } from './configuration';
export {
  Component,
  ContainerItem,
  Container,
  Content,
  Link,
  Menu,
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
