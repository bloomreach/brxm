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

import * as HstConstants from '../../model/constants';

class HstCommentsProcessorService {
  constructor($document, $log) {
    'ngInject';

    this.$document = $document;
    this.$log = $log;
  }

  run(document) {
    if (!document) {
      throw new Error('DOM document is empty');
    }

    return this._processCommentsWithXPath(document);
  }

  * processFragment(jQueryNodeCollection) {
    // eslint-disable-next-line no-plusplus
    for (let i = 0; i < jQueryNodeCollection.length; i++) {
      yield* this._processCommentsWithXPath(jQueryNodeCollection[i]);
    }
  }

  * _processCommentsWithXPath(node) {
    if (node.nodeType === Node.COMMENT_NODE) {
      yield* this._processComment(node);

      return;
    }

    const document = node.nodeType === Node.DOCUMENT_NODE
      ? node
      : this.$document[0];

    const query = document.evaluate('.//comment()', node, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);
    // eslint-disable-next-line no-plusplus
    for (let i = 0; i < query.snapshotLength; i++) {
      yield* this._processComment(query.snapshotItem(i));
    }
  }

  * _processComment(element) {
    const data = this._getCommentData(element);

    if (!this._isHstComment(data) && !this._isHstEndMarker(data)) {
      return;
    }

    const json = this._parseJson(data);

    if (json) {
      yield { element, json };
    }
  }

  _getCommentData(element) {
    if (!element.data || element.data.length === 0) {
      // no data available
      return null;
    }

    return element.data;
  }

  _isHstComment(data) {
    return this._isJsonString(data) && data.includes(HstConstants.TYPE);
  }

  _isHstEndMarker(data) {
    return this._isJsonString(data) && data.includes(HstConstants.END_MARKER);
  }

  _isJsonString(data) {
    if (data === null) {
      return false;
    }
    const trimmedData = data.trim();
    return trimmedData.startsWith('{') && trimmedData.endsWith('}');
  }

  _parseJson(data) {
    try {
      return JSON.parse(data);
    } catch (e) {
      this.$log.warn('Error parsing HST comment as JSON', e, data);
    }
    return null;
  }

  locateComponent(id, startingDomElement) {
    let boxDomElement;

    for (let { nextSibling } = startingDomElement; nextSibling; { nextSibling } = nextSibling) {
      if (nextSibling.nodeType === Node.ELEMENT_NODE && !boxDomElement) {
        boxDomElement = nextSibling; // use the first element of type 1 to draw the overlay box
      }

      if (this._isEndMarker(nextSibling, id)) {
        return [boxDomElement, nextSibling];
      }
    }

    throw new Error(`No component end marker found for '${id}'.`);
  }

  _isEndMarker(domElement, id) {
    if (domElement.nodeType === Node.COMMENT_NODE) {
      const data = this._getCommentData(domElement);
      if (this._isHstEndMarker(data)) {
        const json = this._parseJson(data);
        if (json !== null) {
          return json.uuid === id;
        }
      }
    }
    return false;
  }
}

export default HstCommentsProcessorService;
