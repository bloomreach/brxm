/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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

class DomService {
  constructor($q, $rootScope, $document, $window) {
    'ngInject';

    this.$q = $q;
    this.$rootScope = $rootScope;
    this.$document = $document;
    this.$window = $window;
    this._scrollBarWidth = 0;
  }

  getAppRootUrl() {
    const { location } = this.$document[0];
    const appPath = location.pathname.substring(0, location.pathname.lastIndexOf('/') + 1);
    return `//${location.host}${appPath}`;
  }

  getAssetUrl(href) {
    const link = angular.element('<a>', { href });

    return link[0].href;
  }

  /**
   * Checks whether the iframe DOM is accessible from the Channel Manager.
   * At first, it tries to access the document from the content window,
   * and in case if it a cross-origin website, an exception will be thrown.
   * In case, when the document was not loaded due to some error, the document's body will be empty.
   *
   * @see https://stackoverflow.com/a/12381504
   */
  isFrameAccessible(frame) {
    let html;

    try {
      const { document } = frame.contentWindow || frame;
      html = document.body.innerHTML;
    } catch (error) {} // eslint-disable-line no-empty

    return html != null;
  }

  addCssLinks(window, files) {
    return this.$q.all(files.map(file => this.$q((resolve, reject) => {
      const link = angular.element('<link>', {
        rel: 'stylesheet',
        href: file,
      });

      // resolves relative path in order to use absolute one inside the iframe
      link.attr('href', link[0].href)
        .on('load', () => this.$rootScope.$apply(resolve))
        .on('error', () => this.$rootScope.$apply(reject))
        .appendTo(angular.element('head', window.document));
    })));
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
    return new RegExp(excludedProperties.join('|'));
  }

  _doCopyComputedStyleExcept(fromElement, toElement, excludeRegExp) {
    const fromComputedStyle = fromElement.ownerDocument.defaultView.getComputedStyle(fromElement, null);
    const toComputedStyle = toElement.ownerDocument.defaultView.getComputedStyle(toElement, null);
    const cssDiff = [];

    for (let i = 0, fromLength = fromComputedStyle.length; i < fromLength; i += 1) {
      const cssPropertyName = fromComputedStyle.item(i);
      if (!excludeRegExp || !excludeRegExp.test(cssPropertyName)) {
        const fromValue = fromComputedStyle.getPropertyValue(cssPropertyName);
        if (fromValue && fromValue.length) {
          const toValue = toComputedStyle.getPropertyValue(cssPropertyName);
          if (fromValue !== toValue) {
            cssDiff.push(`${cssPropertyName}:${fromValue}`);
          }
        }
      } else {
        const toStyleValue = toElement.style.getPropertyValue(cssPropertyName);
        if (toStyleValue && toStyleValue.length) {
          cssDiff.push(`${cssPropertyName}:${toStyleValue}`);
        }
      }
    }

    if (cssDiff.length) {
      toElement.style.cssText = cssDiff.join(';');
    }
  }

  copyComputedStyleOfDescendantsExcept(fromElement, toElement, excludedProperties) {
    const excludeRegExp = this._createExcludeRegexp(excludedProperties);

    const fromChildren = $(fromElement).children();
    const toChildren = $(toElement).children();

    // detach the elements that will get styles added to prevent DOM reflows
    toChildren.detach();
    this._doCopyComputedStylesExcept(fromChildren, toChildren, excludeRegExp);
    toChildren.appendTo(toElement);
  }

  _doCopyComputedStylesExcept(fromElements, toElements, excludeRegExp) {
    fromElements.each((index, fromElement) => {
      const toElement = toElements[index];
      this._doCopyComputedStyleExcept(fromElement, toElement, excludeRegExp);

      const fromChildren = $(fromElement).children();
      const toChildren = $(toElement).children();
      this._doCopyComputedStylesExcept(fromChildren, toChildren, excludeRegExp);
    });
  }

  createMouseDownEvent(view, clientX, clientY) {
    const bubbles = true;
    const cancelable = false;
    const detail = 0;
    const screenX = 0;
    const screenY = 0;
    const ctrlKey = false;
    const altKey = false;
    const shiftKey = false;
    const metaKey = false;
    const button = 0;
    const relatedTarget = null;

    const type = this.$window.navigator.pointerEnabled
      ? 'pointerdown'
      : 'mousedown';
    return new MouseEvent(type, {
      bubbles,
      cancelable,
      view,
      detail,
      screenX,
      screenY,
      clientX,
      clientY,
      ctrlKey,
      altKey,
      shiftKey,
      metaKey,
      button,
      relatedTarget,
    });
  }

  isVisible(jqueryElement) {
    let isVisible = jqueryElement.is(':visible');
    if (isVisible) {
      const style = window.getComputedStyle(jqueryElement[0]);
      isVisible = style.visibility !== 'hidden';
    }
    return isVisible;
  }

  escapeHtml(str) {
    // eslint-disable-next-line
    // escape all characters recommended by OWASP: https://www.owasp.org/index.php/XSS_(Cross_Site_Scripting)_Prevention_Cheat_Sheet#RULE_.231_-_HTML_Escape_Before_Inserting_Untrusted_Data_into_HTML_Element_Content
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#x27;')
      .replace(/\//g, '&#x2F;');
  }
}

export default DomService;
