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

import { PageStructureElement } from './pageStructureElement';

/**
 * Components inside a 'transparent' container may be represented by nothing but
 * a set of HTML comments rendered by HST. HST renders a start and end comment
 * so we know how far to go looking for the component's potentially empty DOM
 * tree. This function tries to extract the component's root DOM element. If it
 * determines that there is none, i.e. it finds HST's end-marker before it found
 * a valid root DOM element, it marks the page structure element and uses HST's
 * initial comment DOM element instead. The controller of the overlay element
 * will take this into account and inject a placeholder <div> into the iframe
 * when in edit mode, so the user can interact with the component.
 *
 * @param commentDomElement DOM element representing HST's first comment
 * @param metaData          meta-data provided by HST
 * @param commentProcessor  service to find HST's end marker comment
 * @returns {*}             the DOM element to be registered with the structure element.
 */
function extractDomRoot(commentDomElement, metaData, commentProcessor) {
  let nextSibling = commentDomElement.nextSibling;
  let componentDomRoot;

  while (nextSibling !== null) {
    if (nextSibling.nodeType === 1 && !componentDomRoot) {
      componentDomRoot = nextSibling; // use the first element of type 1 as component DOM root.
    }
    if (commentProcessor.isEndMarker(nextSibling, metaData.uuid)) {
      const commentEndMarker = nextSibling;
      if (!componentDomRoot) {
        // this component currently renders no DOM root element.
        // We put a mark on the component and register the comment DOM element instead.
        metaData.hasNoDom = true;
        componentDomRoot = commentDomElement;
      }

      return {
        componentDomRoot,
        commentEndMarker,
      };
    }
    nextSibling = nextSibling.nextSibling;
  }

  const exception = `No component end marker found for '${metaData.uuid}'.`;
  throw exception;
}

export class ComponentElement extends PageStructureElement {
  constructor(commentDomElement, metaData, container, commentProcessor) {
    let jQueryElement;
    let commentEndMarker;

    if (PageStructureElement.isTransparentXType(container.metaData)) {
      const domRoot = extractDomRoot(commentDomElement, metaData, commentProcessor);
      jQueryElement = $(domRoot.componentDomRoot);
      commentEndMarker = $(domRoot.commentEndMarker);
    } else {
      jQueryElement = $(commentDomElement).parent();
    }

    super('component', jQueryElement, metaData);

    this.container = container;
    this.commentStartMarker = $(commentDomElement);
    this.commentEndMarker = commentEndMarker;
  }

  /**
   * Remove both the component's rendering element and its HST meta-data comment element
   */
  removeFromDOM() {
    this._removeJQueryElement('iframe');
    this._removeCommentElements();
  }

  _removeJQueryElement(type) {
    const jQueryElement = this.getJQueryElement(type);
    if (jQueryElement) {
      jQueryElement.remove();
    }
  }

  _removeCommentElements() {
    this._removeElement(this.commentStartMarker);
    this._removeElement(this.commentEndMarker);
  }

  _removeElement(e) {
    if (e) {
      e.remove();
    }
  }

  getContainer() {
    return this.container;
  }

  setContainer(container) {
    this.container = container;
  }

}
