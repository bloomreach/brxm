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

import { Container as BaseContainer } from '../../../model/entities';
import { ComponentEntityMixin } from './component-entity';

export class Container extends ComponentEntityMixin(BaseContainer) {
  // For no-markup containers we can not depend on the built-in CSS class .hst-container-item to indicate non-emptiness,
  // instead we simply fallback to checking if there is *any* child element present
  isEmptyInDom() {
    const items = this.isXTypeNoMarkup()
      ? this.getComponents()
      : this.getBoxElement().find('.hst-container-item');

    return !items.length;
  }

  getComponentByIframeElement(iframeElement) {
    return this._items.find(item => item.getBoxElement().is(iframeElement));
  }
}
