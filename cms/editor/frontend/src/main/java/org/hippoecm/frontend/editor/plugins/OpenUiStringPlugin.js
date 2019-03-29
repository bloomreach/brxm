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

Hippo.OpenUi = Hippo.OpenUi || {};

Hippo.OpenUi.createStringField = function(parameters) {

  const MIN_HEIGHT_IN_PIXELS = 10;
  const MAX_HEIGHT_IN_PIXELS = 10000;
  const MAX_SIZE_IN_BYTES = 102400;

  const {
    cmsLocale,
    cmsTimeZone,
    cmsVersion,
    compareValue,
    documentDisplayName,
    documentEditorMode,
    documentId,
    documentLocale,
    documentUrlName,
    documentVariantId,
    extensionConfig,
    extensionUrl,
    hiddenValueId,
    iframeParentId,
    initialHeightInPixels,
    userId,
    userDisplayName,
    userFirstName,
    userLastName
  } = unescapeProperties(parameters);

  function unescapeProperties(parameters) {
    const unescaped = {};
    Object.keys(parameters).forEach(parameter => unescaped[parameter] = unescapeProperty(parameters[parameter]));
    return unescaped;
  }

  function unescapeProperty(value) {
    return typeof value === 'string' ? value.replace('\\"', '"') : value;
  }

  function getIframeUrl(cmsOrigin, antiCache) {
    const iframeUrl = new URL(extensionUrl, cmsOrigin);
    iframeUrl.searchParams.append('br.antiCache', antiCache);
    iframeUrl.searchParams.append('br.parentOrigin', cmsOrigin);
    return iframeUrl;
  }

  function getProperties(cmsBaseUrl) {
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

  function getDocumentProperties() {
    return {
      displayName: documentDisplayName,
      id: documentId,
      locale: documentLocale,
      mode: documentEditorMode,
      urlName: documentUrlName,
      variant: {
        id: documentVariantId
      }
    }
  }

  function setHeight(iframe, pixels) {
    const height = Math.max(MIN_HEIGHT_IN_PIXELS, Math.min(pixels, MAX_HEIGHT_IN_PIXELS));
    iframe.style.height = height + "px";
  }

  const cmsOrigin = window.location.origin;
  const antiCache = window.Hippo.antiCache;
  const iframeUrl = getIframeUrl(cmsOrigin, antiCache);
  const iframeParentElement = document.getElementById(iframeParentId);
  const hiddenValueElement = document.getElementById(hiddenValueId);

  const iframe = document.createElement('iframe');
  setHeight(iframe, initialHeightInPixels);

  // Don't allow an extension to change the URL of the top-level window: sandbox the iframe and DON'T include:
  // - allow-top-navigation
  // - allow-top-navigation-by-user-activation
  iframe.sandbox = 'allow-forms allow-popups allow-popups-to-escape-sandbox allow-same-origin allow-scripts';

  const connection = Penpal.connectToChild({
    url: iframeUrl,
    iframe: iframe,
    appendTo: iframeParentElement,
    methods: {
      getDocument: function() {
        return getDocumentProperties();
      },
      getProperties: function() {
        const cmsBaseUrl = cmsOrigin + window.location.pathname;
        return getProperties(cmsBaseUrl);
      },
      getFieldValue: function() {
        return hiddenValueElement.value;
      },
      setFieldValue: function(value) {
        if (value.length >= MAX_SIZE_IN_BYTES) {
          throw new Error('Max value length of ' + MAX_SIZE_IN_BYTES + ' is reached.');
        }
        hiddenValueElement.value = value;
      },
      getFieldCompareValue: function() {
        return compareValue;
      },
      setFieldHeight: function(pixels) {
        setHeight(connection.iframe, pixels);
      }
    }
  });

  HippoAjax.registerDestroyFunction(connection.iframe, function() {
    connection.destroy();
  });
};
