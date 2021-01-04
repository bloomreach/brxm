/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import { ApiOptions } from './spa';
import { CmsOptions, PostMessageOptions } from './cms';
import { UrlBuilderOptions09, UrlBuilderOptions } from './url';

/**
 * Configuration options for generating the page model URL.
 */
export interface UrlOptions {
  /**
   * URL mapping for the live page model.
   */
  live: UrlBuilderOptions09;

  /**
   * URL mapping for the preview page model.
   */
  preview: UrlBuilderOptions09;
}

/**
 * Configuration of the SPA SDK using reverse proxy-based setup.
 */
export interface ConfigurationWithProxy extends ApiOptions, CmsOptions {
  /**
   * Options for generating the page model API URL.
   */
  options: UrlOptions;

  /**
   * The path part of the URL, including a query string if present.
   * For example: '/path/to/page?foo=1'. The path always starts with '/'.
   */
  path?: string;

  /**
   * The option enabling debug mode.
   */
  debug?: boolean;
}

/**
 * Configuration of the SPA SDK using the JWT token-based setup.
 */
export interface ConfigurationWithJwt extends ApiOptions, CmsOptions, PostMessageOptions {
  /**
   * The query string parameter used to pass authorization header value.
   * By default, `token` parameter is used.
   */
  authorizationQueryParameter?: string;

  /**
   * The query string parameter used to pass a cluster node identifier.
   * By default, `server-id` parameter is used.
   */
  serverIdQueryParameter?: string;

  /**
   * The path part of the URL, including a query string if present.
   * For example: '/path/to/page?foo=1'. The path always starts with '/'.
   */
  path?: string;

  /**
   * The option enabling debug mode.
   */
  debug?: boolean;
}

/**
 * Configuration of the SPA SDK using the JWT token-based setup and the Page Model API v0.9.
 */
export interface ConfigurationWithJwt09 extends ConfigurationWithJwt, UrlBuilderOptions09 {}

/**
 * Configuration of the SPA SDK using the JWT token-based setup and the Page Model API v1.0.
 */
export interface ConfigurationWithJwt10 extends ConfigurationWithJwt, UrlBuilderOptions {
  /**
   * The query string parameter used as the brXM endpoint (`cmsBaseUrl`).
   * The option will be ignored if the `cmsBaseUrl` option is not empty.
   * In case when this option is used, the `apiBaseUrl` will be prepended with the value from the query parameter.
   * This option should be used only for testing or debugging.
   * By default, the option is disabled.
   */
  endpointQueryParameter?: string;
}

/**
 * Configuration of the SPA SDK.
 */
export type Configuration = ConfigurationWithProxy | ConfigurationWithJwt09 | ConfigurationWithJwt10;

export function isConfigurationWithProxy(value: any): value is ConfigurationWithProxy {
  return !!(value?.options?.live && value?.options?.preview);
}

export function isConfigurationWithJwt09(value: any): value is ConfigurationWithJwt09 {
  return !!(value?.cmsBaseUrl);
}
