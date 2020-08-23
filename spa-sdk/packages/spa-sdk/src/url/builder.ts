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

import { inject, injectable } from 'inversify';
import { buildUrl, mergeSearchParams, parseUrl } from './utils';

export const UrlBuilderOptionsToken = Symbol.for('UrlBuilderOptionsToken');
export const UrlBuilderService = Symbol.for('UrlBuilderService');

/**
 * Mapping of the incoming HTTP request path to the URL of the page model API.
 */
export interface UrlBuilderOptions {
  /**
   * Base URL to fetch the page model from.
   */
  endpoint?: string;

  /**
   * Base URL of the SPA. Everything after it will be interpreted as a route into the page model.
   * The default base url is an empty string.
   */
  baseUrl?: string;
}

export interface UrlBuilder {
  /**
   * @param path Source path to generate the Page Model API URL.
   * @returns The Page Model API URL.
   */
  getApiUrl(path: string): string;

  /**
   * @param path Source path to generate an SPA URL.
   * @return The SPA URL for the channel path from the source path.
   */
  getSpaUrl(path: string): string;
}

@injectable()
export class UrlBuilderImpl {
  private endpoint: ReturnType<typeof parseUrl>;
  private baseUrl: ReturnType<typeof parseUrl>;

  constructor(@inject(UrlBuilderOptionsToken) options: UrlBuilderOptions) {
    this.endpoint = parseUrl(options.endpoint ?? '');
    this.baseUrl = parseUrl(options.baseUrl ?? '');
  }

  getApiUrl(link: string) {
    const { pathname, searchParams } = parseUrl(link);

    if (this.baseUrl.pathname && !pathname.startsWith(this.baseUrl.pathname)) {
      throw new Error(`The path "${pathname}" does not start with the base path "${this.baseUrl.pathname}".`);
    }

    const route = pathname.substring(this.baseUrl.pathname.length);

    return buildUrl({
      origin: this.endpoint.origin,
      pathname: `${this.endpoint.pathname}${route}`,
      searchParams: mergeSearchParams(searchParams, this.endpoint.searchParams),
    });
  }

  getSpaUrl(link: string) {
    const { hash, pathname, searchParams } = parseUrl(link);
    const route = !pathname.startsWith('/') && !this.baseUrl.pathname
      ? `/${pathname}`
      : pathname;

    return buildUrl({
      origin: this.baseUrl.origin,
      pathname: `${this.baseUrl.pathname}${route}`,
      searchParams: mergeSearchParams(searchParams, this.baseUrl.searchParams),
      hash: hash || this.baseUrl.hash,
    });
  }
}
