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

// TODO: Make this normal class properties
let log;
let HST;

export class HstCommentsProcessorService {

  constructor($log, HstConstants) {
    'ngInject';
    log = $log;
    HST = HstConstants;
  }

  run(document, callback) {
    if (!document) {
      throw new Error('DOM document is empty');
    }
    // IE doesn't support 'evaluate', see
    // https://developer.mozilla.org/en/docs/Web/API/Document/evaluate#Browser_compatibility
    if (!!document.evaluate) {
      this.processCommentsWithXPath(document, callback);
    } else {
      this.processCommentsWithDomWalking(document, callback);
    }
  }

  processFragment(jQueryNodeCollection, callback) {
    for (let i = 0; i < jQueryNodeCollection.length; i++) {
      this.processCommentsWithDomWalking(jQueryNodeCollection[i], callback);
    }
  }

  processCommentsWithXPath(document, callback) {
    const query = document.evaluate('//comment()', document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);
    for (let i = 0; i < query.snapshotLength; i++) {
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
      for (let i = 0; i < node.childNodes.length; i++) {
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
          log.warn('Error invoking callback on HST comment', e, json);
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
    return trimmedData.startsWith('{') && trimmedData.endsWith('}') && trimmedData.includes(HST.TYPE);
  }

  _isHstEndMarker(data) {
    return data !== null && data.startsWith(' {') && data.endsWith('} ') && data.includes(HST.END_MARKER);
  }

  _parseJson(data) {
    try {
      return JSON.parse(data);
    } catch (e) {
      log.warn('Error parsing HST comment as JSON', e, data);
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

    const exception = `No component end marker found for '${id}'.`;
    throw exception;
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
