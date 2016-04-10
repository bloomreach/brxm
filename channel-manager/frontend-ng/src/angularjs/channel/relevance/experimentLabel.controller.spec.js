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

/* eslint-disable prefer-const */

describe('ExperimentLabelCtrl', () => {
  'use strict';

  let $rootScope;
  let $compile;
  let $translate;
  let $element;
  let mockStructureElement;

  beforeEach(() => {
    module('hippo-cm');

    inject((_$rootScope_, _$compile_, _$translate_) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $translate = _$translate_;
    });

    spyOn($translate, 'instant');

    mockStructureElement = {
      type: 'component',
      metaData: {
        'Targeting-experiment-id': '1234',
        'Targeting-experiment-state': 'STARTED',
      },
      getLabel: () => 'label',
    };
  });

  function createController() {
    const $scope = $rootScope.$new();
    $scope.structureElement = mockStructureElement;

    $element = angular.element('<div experiment-label="structureElement"></div>');
    $compile($element)($scope);
    $scope.$digest();
//    spyOn($element, 'addClass');

    return $element.controller('experiment-label');
  }

  it('displays the label in case of a container component', () => {
    mockStructureElement.type = 'container';
    const ExperimentLabelCtrl = createController();

    expect(ExperimentLabelCtrl.text).toBe('label');
//    expect($element.addClass).not.toHaveBeenCalled();
    expect($translate.instant).not.toHaveBeenCalled();
  });

  it('displays the label in case of a component without experiment', () => {
    delete mockStructureElement.metaData['Targeting-experiment-id'];
    const ExperimentLabelCtrl = createController();

    expect(ExperimentLabelCtrl.text).toBe('label');
//    expect($element.addClass).not.toHaveBeenCalled();
    expect($translate.instant).not.toHaveBeenCalled();

    expect(ExperimentLabelCtrl.hasExperiment()).toBe(false);
  });

  it('displays the experiment label and icon if the component has an experiment', () => {
    $translate.instant.and.returnValue('experiment-label');
    const ExperimentLabelCtrl = createController();

    expect(ExperimentLabelCtrl.text).toBe('experiment-label');
//    expect($element.addClass).toHaveBeenCalledWith('has-icon');
    expect($translate.instant).toHaveBeenCalledWith('EXPERIMENT_LABEL_STARTED');

    expect(ExperimentLabelCtrl.hasExperiment()).toBe(true);
  });
});
