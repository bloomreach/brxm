/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
  constructor($http, $q) {
    'ngInject';

    this.$http = $http;
    this.$q = $q;

    this.cache = {};
  }

  _loadSprite(src) {
    if (!this.cache[src]) {
      this.cache[src] = this.$http.get(src).then(({ data }) => data);
    }

    return this.cache[src];
  }

  _getSprite(window, src) {
    return angular.element(`body > svg[data-src="${src}"]`, window.document)[0];
  }

  _injectSprite(window, src, contents) {
    const sprite = this._getSprite(window, src);

    return sprite || angular.element(contents)
      .width(0)
      .height(0)
      .hide()
      .attr('data-src', src)
      .prependTo(angular.element('body', window.document))[0];
  }

  _resolveSprite(window, src) {
    return this._loadSprite(src)
      .then(this._injectSprite.bind(this, window, src));
  }

  getSvg(window, { url, viewBox }) {
    const [src, id] = url.split('#');
    const [x, y, width, height] = viewBox.split(' ');
    const reference = angular.element('<svg>');

    this._resolveSprite(window, src)
      .then(() => reference.replaceWith(
        angular.element(`<svg viewBox="${viewBox}"><use xlink:href="#${id}"/></svg>`)
          .attr('width', width - x)
          .attr('height', height - y),
      ));

    return reference;
  }
}
