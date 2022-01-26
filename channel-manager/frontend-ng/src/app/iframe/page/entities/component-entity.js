/*
 * Copyright 2016-2021 Hippo B.V. (http://www.onehippo.com)
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

import { Container } from '../../../model/entities';
import { EntityMixin } from './entity';
import { waitForImagesToLoad } from '../../utils/dom.utils';

export function ComponentEntityMixin(BaseClass) {
  return class ComponentEntityMixed extends EntityMixin(BaseClass) {
    /**
     * Replace container DOM elements with the given markup
     */
    replaceDOM($newMarkup, onLoadCallback, onLazyLoadCallback) {
      const startComment = this.getStartComment();
      const endComment = this.getEndComment();

      const node = this._removeSiblingsUntil(startComment[0], endComment[0]);

      if (!node) {
        throw new Error('Inconsistent PageStructureElement: startComment and endComment elements should be siblings');
      }

      // For containers of type NoMarkup, D&D may have placed a moved component after the end comment.
      // This would lead to lingering, duplicate components in the DOM. To get rid of these "misplaced"
      // elements, we also remove all subsequent elements. See CHANNELMGR-1030.
      if (this instanceof Container && this.isXTypeNoMarkup() && endComment[0].nextSibling) {
        this._removeSiblingsUntil(endComment[0].nextSibling); // Don't remove the end marker
      }

      // find all images in the new markup
      const images = $newMarkup.find('img, [type="image"]');

      endComment.replaceWith($newMarkup);

      waitForImagesToLoad(images, onLoadCallback, onLazyLoadCallback);
    }

    _removeSiblingsUntil(startNode, endNode) {
      let node = startNode;
      while (node && node !== endNode) {
        const toBeRemoved = node;
        node = node.nextSibling;
        toBeRemoved.remove();
      }

      return node;
    }

    generateBoxElement() {
      return $('<div class="hippo-overlay-box-empty"></div>');
    }

    containsDomElement(domElement) {
      const startCommentNode = this.getStartComment()[0];
      const endCommentNode = this.getEndComment()[0];
      for (let node = startCommentNode.nextSibling; node && node !== endCommentNode; node = node.nextSibling) {
        if (node.contains(domElement)) {
          return true;
        }
      }

      return false;
    }
  };
}
