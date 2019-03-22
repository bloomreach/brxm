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

const extensionConfig = '${extensionConfig}';
const extensionUrl = '${extensionUrl}';
const iframeParentId = '${iframeParentId}';
const hiddenValueId = '${hiddenValueId}';

const cmsLocale = '${cmsLocale}';
const cmsTimeZone = '${cmsTimeZone}';
const cmsVersion = '${cmsVersion}';

const userId = '${userId}';
const userFirstName = '${userFirstName}';
const userLastName = '${userLastName}';
const userDisplayName = '${userDisplayName}';

const MAX_SIZE = 4096;

function getIframeUrl(extensionUrl, cmsOrigin, antiCache) {
  const iframeUrl = new URL(extensionUrl, cmsOrigin);
  iframeUrl.searchParams.append('br.antiCache', antiCache);
  iframeUrl.searchParams.append('br.parentOrigin', cmsOrigin);
  return iframeUrl;
}

function getUiProperties(cmsBaseUrl) {
  return {
    baseUrl: cmsBaseUrl,
    extension: {
      config: extensionConfig
    },
    locale: cmsLocale,
    timeZone: cmsTimeZone,
    user: {
      id: userId,
      firstName: userFirstName,
      lastName: userLastName,
      displayName: userDisplayName
    },
    version: cmsVersion
  }
}

(function (win, doc) {

  const cmsOrigin = win.location.origin;
  const antiCache = win.Hippo.antiCache;
  const iframeUrl = getIframeUrl(extensionUrl, cmsOrigin, antiCache);
  const iframeParentElement = doc.getElementById(iframeParentId);
  const hiddenValueElement = doc.getElementById(hiddenValueId);

  const connection = Penpal.connectToChild({
    url: iframeUrl,
    appendTo: iframeParentElement,
    methods: {
      getProperties: function() {
        const cmsBaseUrl = cmsOrigin + win.location.pathname;
        return getUiProperties(cmsBaseUrl);
      },
      getFieldValue: function() {
        return hiddenValueElement.value;
      },
      setFieldValue: function(value) {
        if (value.length >= MAX_SIZE) {
          throw new Error('Max value length of ' + MAX_SIZE + ' is reached.');
        }
        hiddenValueElement.value = value;
      }
    }
  });

  connection.iframe.setAttribute('sandbox', 'allow-forms allow-popups allow-popups-to-escape-sandbox allow-same-origin allow-scripts');

  win.HippoAjax.registerDestroyFunction(connection.iframe, function() {
    connection.destroy();
  });

})(window, document);
