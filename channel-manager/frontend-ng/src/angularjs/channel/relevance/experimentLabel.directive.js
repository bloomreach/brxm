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

export function ExperimentLabelDirective() {
  'ngInject';

  return {
    restrict: 'A',
    scope: false,
    transclude: true,
    template: '<md-icon ng-if="experimentLabel.hasExperiment()" class="overlay-label-icon"' +
                'qa-experiment-id="{{experimentLabel.getExperimentId()}}">toys</md-icon><ng-transclude/>',
    controller: 'ExperimentLabelCtrl',
    controllerAs: 'experimentLabel',
    link(scope, element, attrs) {
      // override component label
      if (attrs.experimentState) {
        element.find('.overlay-label-text').text(attrs.experimentState);
        element.addClass('has-icon');
      }
    },
  };
}
