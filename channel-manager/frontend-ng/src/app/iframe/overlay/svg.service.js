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

export default class SvgService {
  constructor($document, $http, CommunicationService) {
    'ngInject';

    this.$document = $document;
    this.$http = $http;
    this.CommunicationService = CommunicationService;

    this.cache = {};
  }

  _loadSprite(src) {
    if (!this.cache[src]) {
      this.cache[src] = this.CommunicationService.getAsset(src);
    }

    return this.cache[src];
  }

  _getSprite(src) {
    return this.$document.find(`body > svg[data-src="${src}"]`)[0];
  }

  _injectSprite(src, contents) {
    const sprite = this._getSprite(src);

    return sprite || angular.element(contents)
      .width(0)
      .height(0)
      .attr('data-src', src)
      .hide()
      .prependTo(this.$document.find('body'))[0];
  }

  async _resolveSprite(src) {
    const contents = await this._loadSprite(src);

    return this._injectSprite(src, contents);
  }

  getSvg({ url, viewBox }) {
    const [src, id] = url.split('#');
    const [x, y, width, height] = viewBox.split(' ');
    const reference = angular.element('<svg>');

    this._resolveSprite(src)
      .then(() => reference.replaceWith(
        angular.element(`<svg viewBox="${viewBox}"><use xlink:href="#${id}"/></svg>`)
          .attr('width', width - x)
          .attr('height', height - y),
      ));

    return reference;
  }
}
