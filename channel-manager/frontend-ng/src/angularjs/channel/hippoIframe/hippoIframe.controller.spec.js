/*
 *
 *  * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *  http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

describe('hippoIframeCtrl', () => {
  'use strict';

  let PageStructureService;
  let $mdDialog;
  let hippoIframeCtrl;
  let scope;
  let $q;
  let ScalingService;
  let DragDropService;
  let OverlaySyncService;

  beforeEach(() => {
    let $compile;
    module('hippo-cm');

    inject(($controller, $rootScope, _$compile_, _$mdDialog_, _$q_, _DragDropService_, _OverlaySyncService_, _PageStructureService_, _ScalingService_) => {
      $compile = _$compile_;
      $mdDialog = _$mdDialog_;
      $q = _$q_;
      DragDropService = _DragDropService_;
      OverlaySyncService = _OverlaySyncService_;
      PageStructureService = _PageStructureService_;
      ScalingService = _ScalingService_;
      scope = $rootScope.$new();
    });

    spyOn(ScalingService, 'init');
    spyOn(DragDropService, 'init');
    spyOn(OverlaySyncService, 'init');

    scope.testPath = '/';
    scope.testEditMode = false;

    const el = angular.element('<hippo-iframe path="testPath" edit-mode="testEditMode"></hippo-iframe>');
    $compile(el)(scope);
    scope.$digest();

    hippoIframeCtrl = el.controller('hippo-iframe');
    hippoIframeCtrl.selectedComponent = {
      getLabel: () => 'testLabel',
    };
  });

  it('shows the confirmation dialog and deletes selected component on confirmation', () => {
    spyOn(PageStructureService, 'removeComponent').and.returnValue($q.resolve());
    spyOn($mdDialog, 'show').and.returnValue($q.resolve());
    spyOn($mdDialog, 'confirm').and.callThrough();

    hippoIframeCtrl.deleteComponent('1234');

    scope.$digest();

    expect($mdDialog.confirm).toHaveBeenCalled();
    expect($mdDialog.show).toHaveBeenCalled();
    expect(PageStructureService.removeComponent).toHaveBeenCalledWith('1234');
  });

  it('shows component properties dialog after rejecting the delete operation', () => {
    spyOn(PageStructureService, 'showComponentProperties');
    spyOn($mdDialog, 'show').and.returnValue($q.reject());
    spyOn($mdDialog, 'confirm').and.callThrough();

    hippoIframeCtrl.deleteComponent('1234');

    scope.$digest();

    expect($mdDialog.confirm).toHaveBeenCalled();
    expect($mdDialog.show).toHaveBeenCalled();
    expect(PageStructureService.showComponentProperties).toHaveBeenCalledWith(hippoIframeCtrl.selectedComponent);
  });
});
