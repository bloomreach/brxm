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

import * as HstConstants from '../constants';
import { ComponentEntity } from './component-entity';

export class Component extends ComponentEntity {
  hasExperiment() {
    return !!this.getExperimentId();
  }

  getExperimentId() {
    return this._meta[HstConstants.EXPERIMENT_ID];
  }

  getExperimentStateLabel() {
    if (!this.hasExperiment()) {
      return null;
    }

    return `EXPERIMENT_LABEL_${this._meta[HstConstants.EXPERIMENT_STATE]}`;
  }

  getContainer() {
    return this.container;
  }

  setContainer(container) {
    this.container = container;
  }

  getType() {
    return 'component';
  }

  getRenderVariant() {
    return this._meta[HstConstants.RENDER_VARIANT] || HstConstants.DEFAULT_RENDER_VARIANT;
  }

  getReferenceNamespace() {
    return this._meta[HstConstants.REFERENCE_NAMESPACE];
  }
}
