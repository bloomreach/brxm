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
   * Initializes the URL Builder with options.
   * @param options The URL Builder options.
   */
  initialize(options: UrlBuilderOptions): void;

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

function parseUrl(url: string) {
  // URL constructor requires either a valid URL or a base URL.
  // Since this function returns a pathname, we can safely pass a fake host to be able to resolve relative URLs.
  const parsedUrl = url ? new URL(url, 'http://example.com') : {} as URL;
  const { hash = '', search = '', searchParams = new URLSearchParams() } = parsedUrl;

  // For links like `//example.com?query#hash` pathname will be `/` so we need to strip query and hash parameters first.
  let origin = url.substring(0, url.length - search.length - hash.length);
  let { pathname = '' } = parsedUrl;
  if (!origin.endsWith(pathname)) {
    pathname = pathname.substring(1);
  }

  origin = origin.substring(0, origin.length - pathname.length);

  return { hash, origin, pathname, search, searchParams, path: `${pathname}${search}${hash}` };
}

function isMatchedOrigin(origin: string, baseOrigin: string) {
  return !baseOrigin || !origin || baseOrigin === origin;
}

function isMatchedPathname(pathname: string, basePathname: string) {
  return !basePathname || pathname.startsWith(basePathname);
}

function isMatchedQuery(search: URLSearchParams, baseSearch: URLSearchParams) {
  let match = true;
  baseSearch.forEach((value, key) => {
    match = match && (!value && search.has(key) || search.getAll(key).includes(value));
  });

  return match;
}

export function isMatched(link: string, base = DEFAULT_SPA_BASE_URL) {
  const linkUrl = parseUrl(link);
  const baseUrl = parseUrl(base);

  return isMatchedOrigin(linkUrl.origin, baseUrl.origin)
    && isMatchedPathname(linkUrl.pathname, baseUrl.pathname)
    && isMatchedQuery(linkUrl.searchParams, baseUrl.searchParams);
}

function mergeSearchParams(params1: URLSearchParams, params2: URLSearchParams) {
  const result = new URLSearchParams(params1);
  params2.forEach((value, key) => result.set(key, value));

  return result;
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

    if (this.spaBaseUrl.pathname && !pathname.startsWith(this.spaBaseUrl.pathname)) {
      throw new Error(`The path "${pathname}" does not start with the base path "${this.spaBaseUrl.pathname}".`);
    }

    const route = pathname.substring(this.spaBaseUrl.pathname.length);
    const query = mergeSearchParams(searchParams, this.apiBaseUrl.searchParams).toString();

    return `${this.apiBaseUrl.origin}${this.apiBaseUrl.pathname}${route}${query && `?${query}`}`;
  }

  getSpaUrl(link: string) {
    const { hash, pathname, searchParams } = parseUrl(link);
    const query = mergeSearchParams(searchParams, this.spaBaseUrl.searchParams).toString();
    let route = pathname.startsWith(this.cmsBaseUrl.pathname)
      ? pathname.substring(this.cmsBaseUrl.pathname.length)
      : pathname;

    if (!route.startsWith('/') && !this.spaBaseUrl.pathname) {
      route = `/${route}`;
    }

    return `${this.spaBaseUrl.origin}${this.spaBaseUrl.pathname}${route}${query && `?${query}`}${hash || this.spaBaseUrl.hash}`;
  }
}
