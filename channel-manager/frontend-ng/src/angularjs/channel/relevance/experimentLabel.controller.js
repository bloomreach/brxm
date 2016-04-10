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

const EXPERIMENT_ID = 'Targeting-experiment-id';
const EXPERIMENT_STATE = 'Targeting-experiment-state';

export class ExperimentLabelCtrl {
  constructor($translate, $element) {
    'ngInject';

    if (this.structureElement.type === 'component' && this.hasExperiment()) {
      $element.addClass('has-icon');
      this.text = $translate.instant(`EXPERIMENT_LABEL_${this._getExperimentState()}`);
    } else {
      this.text = this.structureElement.getLabel();
    }
  }

  hasExperiment() {
    return !!this.structureElement.metaData[EXPERIMENT_ID];
  }

  _getExperimentState() {
    return this.structureElement.metaData[EXPERIMENT_STATE];
  }
}
