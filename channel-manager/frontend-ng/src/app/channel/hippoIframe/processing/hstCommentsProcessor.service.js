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

class HstCommentsProcessorService {
  constructor($log, HstConstants) {
    'ngInject';

    this.$log = $log;
    this.HstConstants = HstConstants;
  }

  run(document, callback) {
    if (!document) {
      throw new Error('DOM document is empty');
    }
    this.processCommentsWithXPath(document, callback);
  }

  processFragment(jQueryNodeCollection, callback) {
    for (let i = 0; i < jQueryNodeCollection.length; i += 1) {
      this.processCommentsWithDomWalking(jQueryNodeCollection[i], callback);
    }
  }

  processCommentsWithXPath(document, callback) {
    const query = document.evaluate('//comment()', document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);
    for (let i = 0; i < query.snapshotLength; i += 1) {
      this._processComment(query.snapshotItem(i), callback);
    }
  }

  processCommentsWithDomWalking(node, callback) {
    if (!node || node.nodeType === undefined) {
      return;
    }

    if (node.nodeType === 8) {
      this._processComment(node, callback);
    } else {
      for (let i = 0; i < node.childNodes.length; i += 1) {
        this.processCommentsWithDomWalking(node.childNodes[i], callback);
      }
    }
  }

  _processComment(element, callback) {
    const data = this._getCommentData(element);

    if (this._isHstComment(data)) {
      const json = this._parseJson(data);
      if (json !== null) {
        try {
          callback(element, json);
        } catch (e) {
          this.$log.warn('Error invoking callback on HST comment', e, json);
        }
      }
    }
  }

  _getCommentData(element) {
    if (element.length < 0) {
      // Skip conditional comments in IE: reading their 'data' property throws an
      // Error "Not enough storage space is available to complete this operation."
      // Conditional comments can be recognized by a negative 'length' property.
      return null;
    }

    if (!element.data || element.data.length === 0) {
      // no data available
      return null;
    }

    return element.data;
  }

  _isHstComment(data) {
    if (data === null) {
      return false;
    }
    const trimmedData = data.trim();
    return trimmedData.startsWith('{') && trimmedData.endsWith('}') && trimmedData.includes(this.HstConstants.TYPE);
  }

  _isHstEndMarker(data) {
    return data !== null && data.startsWith(' {') && data.endsWith('} ') && data.includes(this.HstConstants.END_MARKER);
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
    let nextSibling = startingDomElement.nextSibling;
    let boxDomElement;

    while (nextSibling !== null) {
      if (nextSibling.nodeType === 1 && !boxDomElement) {
        boxDomElement = nextSibling; // use the first element of type 1 to draw the overlay box
      }
      if (this._isEndMarker(nextSibling, id)) {
        return [boxDomElement, nextSibling];
      }
      nextSibling = nextSibling.nextSibling;
    }

    throw new Error(`No component end marker found for '${id}'.`);
  }

  _isEndMarker(domElement, id) {
    if (domElement.nodeType === 8) {
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
