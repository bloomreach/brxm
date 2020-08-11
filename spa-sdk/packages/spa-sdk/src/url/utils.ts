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

interface Url {
  hash: string;
  origin: string;
  pathname: string;
  search: string;
  searchParams: URLSearchParams;
  path: string;
}

export function appendSearchParams(url: string, params: URLSearchParams) {
  const { hash, origin, pathname, searchParams } = parseUrl(url);

  return buildUrl({ hash, origin, pathname, searchParams: mergeSearchParams(searchParams, params) });
}

export function buildUrl(url: Partial<Url>) {
  const searchParams = url.searchParams?.toString() ?? '';
  const search = url.search ?? `${searchParams && `?${searchParams}`}`;
  const path = url.path ?? `${url.pathname ?? ''}${search}${url.hash ?? ''}`;

  return `${url.origin ?? ''}${path}`;
}

export function extractSearchParams(url: string, params: string[]) {
  const { hash, origin, pathname, searchParams } = parseUrl(url);
  const extracted = new URLSearchParams();

  params.forEach((param) => {
    if (searchParams.has(param)) {
      extracted.set(param, searchParams.get(param)!);
      searchParams.delete(param);
    }
  });

  return {
    searchParams: extracted,
    url: buildUrl({ hash, origin, pathname, searchParams }),
  };
}

export function isAbsoluteUrl(url: string): boolean {
  const { origin, pathname } = parseUrl(url);

  return !!origin || pathname.startsWith('/');
}

function isMatchedOrigin(origin: string, baseOrigin: string) {
  const [schema, host = ''] = origin.split('//', 2);
  const [baseSchema, baseHost = ''] = baseOrigin.split('//', 2);

  return !baseOrigin
    || !origin
    || (!schema || !baseSchema || schema === baseSchema) && baseHost === host;
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

export function isMatched(link: string, base = '') {
  const linkUrl = parseUrl(link);
  const baseUrl = parseUrl(base);

  return isMatchedOrigin(linkUrl.origin, baseUrl.origin)
    && isMatchedPathname(linkUrl.pathname, baseUrl.pathname)
    && isMatchedQuery(linkUrl.searchParams, baseUrl.searchParams);
}

export function mergeSearchParams(params: URLSearchParams, ...rest: URLSearchParams[]) {
  const result = new URLSearchParams(params);
  rest.forEach(params => params.forEach((value, key) => result.set(key, value)));

  return result;
}

export function parseUrl(url: string): Url {
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

export function resolveUrl(url: string, base: string) {
  const baseUrl = parseUrl(base);
  const sourceUrl = parseUrl(url);
  const pathname = sourceUrl.pathname.startsWith('/')
    ? sourceUrl.pathname
    : `${baseUrl.pathname}${baseUrl.pathname.endsWith('/') || !sourceUrl.pathname ? '' : '/'}${sourceUrl.pathname}`;

  return buildUrl({
    pathname,
    hash: sourceUrl.hash || baseUrl.hash,
    origin: sourceUrl.origin || baseUrl.origin,
    searchParams: mergeSearchParams(baseUrl.searchParams, sourceUrl.searchParams),
  });
}
