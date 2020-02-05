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
import { CmsOptions } from './cms';
import { UrlBuilderOptions } from './url';

/**
 * Configuration options for generating the page model URL.
 */
export interface UrlOptions {
  /**
   * URL mapping for the live page model.
   */
  live: UrlBuilderOptions;

  /**
   * URL mapping for the preview page model.
   */
  preview: UrlBuilderOptions;
}

/**
 * Configuration of the SPA SDK using reverse proxy-based setup.
 */
export interface ConfigurationWithProxy extends ApiOptions, CmsOptions {
  /**
   * Options for generating the page model API URL.
   */
  options: UrlOptions;
}

/**
 * Configuration of the SPA SDK using the JWT token-based setup.
 */
export interface ConfigurationWithJwt extends ApiOptions, CmsOptions, UrlBuilderOptions {
  /**
   * The query string parameter used to pass authorization header.
   * By default, `token` parameter is used.
   */
  authorizationQueryParameter?: string;

  /**
   * The query string parameter used to pass a cluster node identifier.
   * By default, `serverid` parameter is used.
   */
  serverIdQueryParameter?: string;
}

/**
 * Configuration of the SPA SDK.
 */
export type Configuration = ConfigurationWithProxy | ConfigurationWithJwt;

export function isConfigurationWithProxy(value: any): value is ConfigurationWithProxy {
  return !!(value?.options?.live && value?.options?.preview);
}
