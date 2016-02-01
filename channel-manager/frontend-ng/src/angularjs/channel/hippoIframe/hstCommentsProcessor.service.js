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

let log;
let CONST_HST_CONSTANT;

function getCommentData(element) {
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

function isHstComment(data) {
  return data !== null && data.startsWith(' {') && data.endsWith('} ') && data.includes(CONST_HST_CONSTANT.TYPE);
}

function processComment(element, callback) {
  const data = getCommentData(element);

  if (isHstComment(data)) {
    try {
      const json = JSON.parse(data);
      return callback(element, json);
    } catch (e) {
      log.warn('Error parsing HST comment', e, data);
    }
  }
}

function isComment(element) {
  return element.nodeType === 8;
}

function processCommentsWithXPath(document, callback) {
  const query = document.evaluate('//comment()', document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);
  for (let i = 0; i < query.snapshotLength; i++) {
    processComment(query.snapshotItem(i), callback);
  }
}

function processCommentsWithDomWalking(node, callback) {
  if (!node || node.nodeType === undefined) {
    return;
  }

  if (isComment(node)) {
    processComment(node, callback);
  } else {
    for (let i = 0; i < node.childNodes.length; i++) {
      processCommentsWithDomWalking(node.childNodes[i], callback);
    }
  }
}

export class HstCommentsProcessorService {

  constructor($log, HST_CONSTANT) {
    'ngInject';
    log = $log;
    CONST_HST_CONSTANT = HST_CONSTANT;
  }

  run(document, callback) {
    // IE doesn't support 'evaluate', see https://developer.mozilla.org/en/docs/Web/API/Document/evaluate#Browser_compatibility
    if (!!document.evaluate) {
      processCommentsWithXPath(document, callback);
    } else {
      processCommentsWithDomWalking(document, callback);
    }
  }
}
