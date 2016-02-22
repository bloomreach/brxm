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

import { HST_CONSTANT } from '../../../api/hst.constants';

export class PageStructureElement {
  constructor(type, jQueryElement, metaData) {
    this.type = type;
    this.metaData = metaData;
    this.jQueryElements = {};

    this.setJQueryElement('iframe', jQueryElement);
  }

  setJQueryElement(elementType, element) {
    this.jQueryElements[elementType] = element;
  }

  getJQueryElement(elementType) {
    return this.jQueryElements[elementType];
  }

  getLabel() {
    let label = this.metaData[HST_CONSTANT.LABEL];
    if (label === 'null') {
      label = this.type; // no label available, fallback to type.
    }
    return label;
  }
}
