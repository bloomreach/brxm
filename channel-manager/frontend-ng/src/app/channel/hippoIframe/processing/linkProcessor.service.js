/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

function startsWith(str, prefix) {
  return str !== undefined && str !== null
    && prefix !== undefined && prefix !== null
    && prefix === str.slice(0, prefix.length);
}

function isInternalLink(link, internalLinks) {
  if (!angular.isArray(internalLinks)) {
    return false;
  }
  return internalLinks.some(internalLink => startsWith(link, internalLink));
}

class LinkProcessorService {

  constructor($translate, $window) {
    'ngInject';

    this.$translate = $translate;
    this.$window = $window;
  }

  run(document, internalLinkPrefixes) {
    angular.element(document).find('a').each((index, el) => {
      const link = angular.element(el);
      let url = link.prop('href');

      // handle links within SVG elements
      if (url instanceof SVGAnimatedString) {
        url = url.baseVal;
      }

      // intercept all clicks on external links: open them in a new tab if confirmed by the user
      if (url && !isInternalLink(url, internalLinkPrefixes)) {
        link.attr('target', '_blank');
        link.click((event) => {
          // TODO: should use proper dialog!!
          if (!this.$window.confirm(this.$translate.instant('CONFIRM_OPEN_EXTERNAL_LINK'))) { // eslint-disable-line no-alert
            event.preventDefault();
          }
        });
      }
    });
  }
}

export default LinkProcessorService;
