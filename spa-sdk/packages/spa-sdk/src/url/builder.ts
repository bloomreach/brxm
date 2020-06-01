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

import { buildUrl, mergeSearchParams, parseUrl } from './utils';

const DEFAULT_API_BASE_URL = '/resourceapi';
const DEFAULT_SPA_BASE_URL = '';

/**
 * Mapping of the incoming HTTP request path to the URL of the page model API.
 */
export interface UrlBuilderOptions {
  /**
   * Base URL to fetch the page model from.
   * The default URL is `cmsBaseUrl` + `/resourceapi`.
   */
  apiBaseUrl?: string;

  /**
   * Base URL of the CMS.
   */
  cmsBaseUrl: string;

  /**
   * Base URL of the SPA. Everything after it will be interpreted as a route into the page model.
   * The default base url is an empty string.
   */
  spaBaseUrl?: string;
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

export class UrlBuilderImpl {
  private apiBaseUrl!: ReturnType<typeof parseUrl>;
  private cmsBaseUrl!: ReturnType<typeof parseUrl>;
  private spaBaseUrl!: ReturnType<typeof parseUrl>;

  constructor(private options: UrlBuilderOptions = { cmsBaseUrl: '' }) {
    this.initialize(options);
  }

  initialize(options: UrlBuilderOptions) {
    this.options = options;
    this.apiBaseUrl = parseUrl(options.apiBaseUrl || `${options.cmsBaseUrl}${DEFAULT_API_BASE_URL}`);
    this.cmsBaseUrl = parseUrl(options.cmsBaseUrl);
    this.spaBaseUrl = parseUrl(options.spaBaseUrl || DEFAULT_SPA_BASE_URL);
  }

  getApiUrl(link: string) {
    const { pathname, searchParams } = parseUrl(link);

    // TODO: Remove when HSTTWO-4728 is resolved
    if (this.apiBaseUrl.pathname && pathname.startsWith(this.apiBaseUrl.pathname)) {
      return buildUrl({
        pathname,
        origin: this.apiBaseUrl.origin,
        searchParams: mergeSearchParams(this.apiBaseUrl.searchParams, searchParams),
      });
    }

    if (this.spaBaseUrl.pathname && !pathname.startsWith(this.spaBaseUrl.pathname)) {
      throw new Error(`The path "${pathname}" does not start with the base path "${this.spaBaseUrl.pathname}".`);
    }

    const route = pathname.substring(this.spaBaseUrl.pathname.length);

    return buildUrl({
      origin: this.apiBaseUrl.origin,
      pathname: `${this.apiBaseUrl.pathname}${route}`,
      searchParams: mergeSearchParams(searchParams, this.apiBaseUrl.searchParams),
    });
  }

  getSpaUrl(link: string) {
    const { hash, pathname, searchParams } = parseUrl(link);
    let route = pathname.startsWith(this.cmsBaseUrl.pathname)
      ? pathname.substring(this.cmsBaseUrl.pathname.length)
      : pathname;

    if (!route.startsWith('/') && !this.spaBaseUrl.pathname) {
      route = `/${route}`;
    }

    return buildUrl({
      origin: this.spaBaseUrl.origin,
      pathname: `${this.spaBaseUrl.pathname}${route}`,
      searchParams: mergeSearchParams(searchParams, this.spaBaseUrl.searchParams),
      hash: hash || this.spaBaseUrl.hash,
    });
  }
}
