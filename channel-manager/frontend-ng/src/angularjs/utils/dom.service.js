/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export class DomService {

  constructor($q, $rootScope, $document) {
    'ngInject';

    this.$q = $q;
    this.$rootScope = $rootScope;
    this.$document = $document;
  }

  getAppRootUrl() {
    const location = this.$document[0].location;
    const appPath = location.pathname.substring(0, location.pathname.lastIndexOf('/') + 1);
    return `//${location.host}${appPath}`;
  }

  addCss(window, url) {
    return $.get(url, (response) => {
      const link = $(`<style>${response}</style>`);
      $(window.document).find('head').append(link);
    });
  }

  addScript(window, url) {
    return this.$q((resolve, reject) => {
      const script = window.document.createElement('script');
      script.type = 'text/javascript';
      script.src = url;
      script.addEventListener('load', () => this.$rootScope.$apply(resolve));
      script.addEventListener('error', () => this.$rootScope.$apply(reject));
      window.document.body.appendChild(script);
    });
  }

  copyComputedStyleExcept(fromElement, toElement, excludedProperties) {
    const excludeRegExp = this._createExcludeRegexp(excludedProperties);
    this._doCopyComputedStyleExcept(fromElement, toElement, excludeRegExp);
  }

  _createExcludeRegexp(excludedProperties) {
    if (!excludedProperties) {
      return null;
    }
    let excludeRegexText = '';
    excludedProperties.forEach((prop) => {
      if (excludeRegexText.length > 0) {
        excludeRegexText += '|';
      }
      excludeRegexText += prop;
    });
    return new RegExp(excludeRegexText);
  }

  _doCopyComputedStyleExcept(fromElement, toElement, excludeRegExp) {
    const fromWindow = fromElement.ownerDocument.defaultView;
    const fromComputedStyle = fromWindow.getComputedStyle(fromElement, null);
    const toWindow = toElement.ownerDocument.defaultView;
    const toComputedStyle = toWindow.getComputedStyle(toElement, null);

    let cssTextDiff = '';

    for (let i = 0; i < fromComputedStyle.length; i++) {
      const cssPropertyName = fromComputedStyle.item(i);

      if (!excludeRegExp || !excludeRegExp.test(cssPropertyName)) {
        const fromValue = fromComputedStyle.getPropertyValue(cssPropertyName);
        const toValue = toComputedStyle.getPropertyValue(cssPropertyName);
        if (fromValue !== toValue) {
          cssTextDiff += `${cssPropertyName}:${fromValue};`;
        }
      }
    }

    if (cssTextDiff.length > 0) {
      toElement.style.cssText = cssTextDiff;
    }
  }

  copyComputedStyleOfDescendantsExcept(fromElement, toElement, excludedProperties) {
    const excludeRegExp = this._createExcludeRegexp(excludedProperties);
    this._doCopyComputedStyleOfDescendantsExcept(fromElement, toElement, excludeRegExp);
  }

  _doCopyComputedStyleOfDescendantsExcept(fromRootElement, toRootElement, excludeRegExp) {
    const fromChildren = $(fromRootElement).children();
    const toChildren = $(toRootElement).children();

    fromChildren.each((index, fromChild) => {
      const toChild = toChildren[index];
      this._doCopyComputedStyleExcept(fromChild, toChild, excludeRegExp);
      this._doCopyComputedStyleOfDescendantsExcept(fromChild, toChild, excludeRegExp);
    });
  }
}
