/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

function appendSearchParams(url, win) {
  url.searchParams.append('br.antiCache', Hippo.antiCache);
  url.searchParams.append('br.parentOrigin', win.location.origin);
}

function isAbsoluteUrl(url) {
  return url.startsWith('http://') || url.startsWith('https://');
}

function getAbsoluteUrl(urlString, win) {
  const url = new URL(urlString);
  appendSearchParams(url, win);
  return url;
}

function getRelativeUrl(urlString, win) {
  // TODO: fix this code
  const url = new URL('http://localhost:8080/' + urlString);
  appendSearchParams(url, win);
  return url;
}

function getIframeUrl(url, win) {
  return isAbsoluteUrl(url)
  ? getAbsoluteUrl(url, win)
  : getRelativeUrl(url, win);
}

(function (win, doc) {

  const extensionUrl = '${extensionUrl}';
  const iframeParentId = '${iframeParentId}';
  const userLocale = '${userLocale}';
  const userTimezone = '${userTimezone}';

  const url = getIframeUrl(extensionUrl, win);

  const connection = Penpal.connectToChild({
    url: url,
    appendTo: doc.getElementById(iframeParentId),
    methods: {
      getProperties: function() {
        return {baseUrl: 'testUrl', locale: userLocale, timezone: userTimezone}
      }
    }
  });
  
})(window, document);
