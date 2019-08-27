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
import { Request, Options } from './api';
import { concatPaths } from './path';

const DEFAULT_API_SUFFIX = '/resourceapi';
const PREVIEW_QUERY_PARAM = 'bloomreach-preview';

export function buildModelUrl(request: Request, options: Options): string {
  const path = getPath(request.path);
  const query = getQuery(request.path);

  const isPreview = determinePreview(path, query, options.previewPrefix);

  const prefix = isPreview ? options.previewPrefix : options.livePrefix;
  const channelPath = getChannelPath(path, query, prefix);
  const suffix = options.apiSuffix || DEFAULT_API_SUFFIX;

  const base = concatPaths(prefix, channelPath);
  let url = concatPaths(base, suffix);
  if (query) {
    url += `?${query}`;
  }
  return url;
}

function getPath(requestPath: string): string {
  const queryStart = requestPath.indexOf('?');
  return queryStart < 0 ? requestPath : requestPath.substring(0, queryStart);
}

function getQuery(requestPath: string): string {
  const queryStart = requestPath.indexOf('?');
  return queryStart < 0 ? '' : requestPath.substring(queryStart + 1);
}

function determinePreview(path: string, query: string, previewPrefix: string) {
  const searchParams = new URLSearchParams(query);
  const previewParamValue = searchParams.get(PREVIEW_QUERY_PARAM);

  return previewParamValue === 'true'
    || (previewParamValue === null && path.startsWith(new URL(previewPrefix).pathname));
}

function getChannelPath(path: string, query: string, apiUrl: string) {
  const apiPath = new URL(apiUrl).pathname;

  if (path.startsWith(apiPath)) {
    return path.substring(apiPath.length);
  }

  // assume the SPA lives at "/" (e.g. "http://localhost:3000/"), so any path below it is relative in the channel
  return path;
}
