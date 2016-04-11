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
  let $scope;
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
    $scope = $rootScope.$new();
    $scope.overlayElement = {
      structureElement: mockStructureElement,
      text: 'label',
      icon: '',
    };
    $scope.structureElement = mockStructureElement;

    $element = angular.element('<div experiment-label></div>');
    $compile($element)($scope);
    $scope.$digest();

    return $element.controller('experiment-label');
  }

  it('displays the label in case of a container component', () => {
    mockStructureElement.type = 'container';
    createController();

    expect($scope.overlayElement.text).toBe('label');
    expect($scope.overlayElement.icon).toBe('');
    expect($translate.instant).not.toHaveBeenCalled();
  });

  it('displays the label in case of a component without experiment', () => {
    delete mockStructureElement.metaData['Targeting-experiment-id'];
    createController();

    expect($scope.overlayElement.text).toBe('label');
    expect($scope.overlayElement.icon).toBe('');
    expect($translate.instant).not.toHaveBeenCalled();
  });

  it('displays the experiment label and icon if the component has an experiment', () => {
    $translate.instant.and.returnValue('experiment-label');
    createController();

    expect($scope.overlayElement.text).toBe('experiment-label');
    expect($scope.overlayElement.icon).not.toBe('');
    expect($translate.instant).toHaveBeenCalledWith('EXPERIMENT_LABEL_STARTED');
  });
});
