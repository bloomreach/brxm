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

import 'reflect-metadata';
import { Container } from 'inversify';
import { Api, ApiImpl, Spa } from './spa';
import { CmsService, Cms, CmsModule, PostMessageService, PostMessage } from './cms';
import {
  PageFactory,
  PageModel,
  PageModule,
  Page,
  isPage,
} from './page';
import { Configuration, ConfigurationWithProxy, ConfigurationWithJwt, isConfigurationWithProxy } from './configuration';
import { EventBus, EventBusService, EventsModule } from './events';
import {
  UrlModule,
  UrlBuilderOptionsToken,
  UrlBuilderService,
  UrlBuilder,
  appendSearchParams,
  extractSearchParams,
  isMatched,
  parseUrl,
} from './url';

const DEFAULT_AUTHORIZATION_PARAMETER = 'token';
const DEFAULT_SERVER_ID_PARAMETER = 'server-id';

const container = new Container({ skipBaseClassChecks: true });
const pages = new WeakMap<Page, Spa>();

container.load(EventsModule(), CmsModule(), UrlModule());

function onReady<T, U>(value: T | Promise<T>, callback: (value: T) => U): U | Promise<U> {
  return value instanceof Promise
    ? value.then(callback)
    : callback(value);
}

function initializeSpa(api: Api, scope: Container) {
  const eventBus = scope.get<EventBus>(EventBusService);
  const pageFactory = scope.get<PageFactory>(PageFactory);

  return new Spa(eventBus, api, pageFactory);
}

function initializeWithProxy(configuration: ConfigurationWithProxy, model?: PageModel) {
  const options = isMatched(configuration.request.path, configuration.options.preview.spaBaseUrl)
    ? configuration.options.preview
    : configuration.options.live;
  const scope = container.createChild();

  scope.load(UrlModule(), PageModule());
  scope.bind(UrlBuilderOptionsToken).toConstantValue(options);

  const url = scope.get<UrlBuilder>(UrlBuilderService);
  const api = new ApiImpl(url, configuration);
  const spa = initializeSpa(api, scope);

  scope.getNamed<Cms>(CmsService, 'cms14').initialize(configuration);

  return spa.initialize(model ?? configuration.request.path);
}

function initializeWithJwt(configuration: ConfigurationWithJwt, model?: PageModel) {
  const authorizationParameter = configuration.authorizationQueryParameter ?? DEFAULT_AUTHORIZATION_PARAMETER;
  const endpointParameter = configuration.endpointQueryParameter ?? '';
  const serverIdParameter = configuration.serverIdQueryParameter ?? DEFAULT_SERVER_ID_PARAMETER;
  const { url: path, searchParams } = extractSearchParams(
    configuration.request.path,
    [authorizationParameter, serverIdParameter, endpointParameter].filter(Boolean),
  );
  const authorizationToken = searchParams.get(authorizationParameter) ?? undefined;
  const endpoint = searchParams.get(endpointParameter) ?? undefined;
  const serverId = searchParams.get(serverIdParameter) ?? undefined;
  const config = {
    ...configuration,
    apiBaseUrl: configuration.cmsBaseUrl || !configuration.apiBaseUrl || !endpoint
      ? configuration.apiBaseUrl
      : `${endpoint}${configuration.apiBaseUrl}`,
    cmsBaseUrl: configuration.cmsBaseUrl ?? endpoint,
    spaBaseUrl: appendSearchParams(configuration.spaBaseUrl ?? '', searchParams),
  };
  const scope = container.createChild();

  scope.load(PageModule(), UrlModule());
  scope.bind(UrlBuilderOptionsToken).toConstantValue(config);

  const url = scope.get<UrlBuilder>(UrlBuilderService);
  const api = new ApiImpl(url, { authorizationToken, serverId, ...config });
  const spa = initializeSpa(api, scope);

  return onReady(
    spa.initialize(model ?? path),
    (spa) => {
      if (spa.getPage()?.isPreview() && config.cmsBaseUrl) {
        const { origin } = parseUrl(config.cmsBaseUrl);
        scope.get<PostMessage>(PostMessageService).initialize({ ...config, origin });
        scope.get<Cms>(CmsService).initialize(config);
      }

      return spa;
    },
  );
}

/**
 * Initializes the page model.
 *
 * @param configuration Configuration of the SPA integration with brXM.
 * @param model Preloaded page model.
 */
export function initialize(configuration: Configuration, model: Page | PageModel): Page;

/**
 * Initializes the page model.
 *
 * @param configuration Configuration of the SPA integration with brXM.
 */
export async function initialize(configuration: Configuration): Promise<Page>;

export function initialize(configuration: Configuration, model?: Page | PageModel): Page | Promise<Page> {
  if (isPage(model)) {
    return model;
  }

  return onReady(
    isConfigurationWithProxy(configuration)
      ? initializeWithProxy(configuration, model)
      : initializeWithJwt(configuration, model),
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
