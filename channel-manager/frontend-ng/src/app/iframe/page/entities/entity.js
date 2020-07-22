/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

export function EntityMixin(BaseClass) {
  return class EntityMixed extends BaseClass {
    getStartComment() {
      return this.iframeStartComment;
    }

    setStartComment(jQueryStartComment) {
      this.iframeStartComment = jQueryStartComment;
    }

    getEndComment() {
      return this.iframeEndComment;
    }

    setEndComment(jQueryEndComment) {
      this.iframeEndComment = jQueryEndComment;
    }

    getOverlayElement() {
      return this.overlay;
    }

    setOverlayElement(jQueryOverlayElement) {
      this.overlay = jQueryOverlayElement;
    }

    hasOverlayElement() {
      return !!this.overlay;
    }

    getBoxElement() {
      return this.iframeBoxElement;
    }

    setBoxElement(jQueryBoxElement) {
      this.iframeBoxElement = jQueryBoxElement;
    }

    prepareBoxElement() {
      let boxElement = this.getBoxElement();
      if (!boxElement || !boxElement.length) {
        boxElement = this._insertGeneratedBoxElement();
        this.setBoxElement(boxElement);
        this._hasGeneratedBoxElement = true;
      }
      return boxElement;
    }

    _insertGeneratedBoxElement() {
      // sub-classes can override this method to generate a custom placeholder box element
      const startComment = this.getStartComment();
      const generatedBox = this.generateBoxElement();

      generatedBox.insertAfter(startComment);

      return generatedBox;
    }

    hasGeneratedBoxElement() {
      return !!this._hasGeneratedBoxElement;
    }
  };
}
