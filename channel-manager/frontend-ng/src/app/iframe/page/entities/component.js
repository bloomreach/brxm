/*
 * Copyright 2016-2023 Bloomreach
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

import { Component as BaseComponent } from '../../../model/entities';
import { ComponentEntityMixin } from './component-entity';

export class Component extends ComponentEntityMixin(BaseComponent) {
  setContainer(container) {
    super.setContainer(container);

    if (container && !container.isXTypeNoMarkup()) {
      const startComment = this.getStartComment();
      this.setBoxElement(startComment.parent());
    }
  }
}
