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

import { HttpRequest } from './http';

const DEFAULT_SPA_BASE_PATH = '/';
const DEFAULT_PAGE_MODEL_API_SUFFIX = '/resourceapi';
const PREVIEW_QUERY_PARAM = 'bloomreach-preview';

/**
 * Configuration options for generating the page model URL.
 */
export interface PageModelUrlOptions {

  /**
   * URL mapping for the live page model.
   */
  live: PageModelUrlMapping;

  /**
   * URL mapping for the preview page model.
   */
  preview: PageModelUrlMapping;

  /**
   * Optional custom suffix for requests to the page model API. Must start with a slash.
   * The default suffix is '/resourceapi'.
   */
  pageModelApiSuffix?: string;
}

/**
 * Mapping of the incoming HTTP request path to the URL of the page model API.
 */
interface PageModelUrlMapping {

  /**
   * Base path of the SPA. Everything after it will be interpreted as a route into the page model.
   * The default base path is '/'.
   */
  spaBasePath?: string;

  /**
   * Base URL to fetch the page model from.
   */
  pageModelBaseUrl: string;
}

export interface PageModelUrlBuilder {
  (request: HttpRequest, options: PageModelUrlOptions): string;
}

export function buildPageModelUrl(request: HttpRequest, options: PageModelUrlOptions): string {
  const [path, query = ''] = request.path.split('?', 2);
  const isPreview = determinePreview(query);

  const urlMapping = isPreview ? options.preview : options.live;
  const channelPath = getChannelPath(path, query, urlMapping);

  const apiBaseUrl = urlMapping.pageModelBaseUrl;
  const apiSuffix = options.pageModelApiSuffix || DEFAULT_PAGE_MODEL_API_SUFFIX;

  let url = removeTrailingSlash(`${apiBaseUrl}${channelPath}`) + apiSuffix;
  if (query) {
    url += `?${query}`;
  }
  return url;
}

function determinePreview(query: string) {
  const searchParams = new URLSearchParams(query);
  const previewParamValue = searchParams.get(PREVIEW_QUERY_PARAM);
  return previewParamValue === 'true';
}

function getChannelPath(path: string, query: string, mapping: PageModelUrlMapping) {
  const spaBasePath = mapping.spaBasePath || DEFAULT_SPA_BASE_PATH;

  if (!path.startsWith(spaBasePath)) {
    throw new Error(`Request path '${path}' does not start with SPA base path '${spaBasePath}'`);
  }
  return path.substring(spaBasePath.length);
}

function removeTrailingSlash(path: string) {
  return path.endsWith('/') ? path.slice(0, -1) : path;
}
