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

describe('hippoIframeCtrl', function () {
  'use strict';

  var PageStructureService;
  var $mdDialog;
  var hippoIframeCtrl;
  var $element;
  var scope;
  var $q;

  beforeEach(function () {
    module('hippo-cm');

    inject(function ($controller, _$rootScope_, _$q_, _$mdDialog_, _PageStructureService_) {
      scope = _$rootScope_.$new();
      $q = _$q_;

      $mdDialog = _$mdDialog_;
      PageStructureService = _PageStructureService_;

      spyOn(PageStructureService, 'removeComponent');
      spyOn(PageStructureService, 'showComponentProperties');

      console.log('initiating controller');
      hippoIframeCtrl = $controller('hippoIframeCtrl', {
        $scope: scope,
      });
    });
  });

  it('initializes mock services', function () {
    expect(PageStructureService.removeComponent).toBeDefined();
    expect(PageStructureService.showComponentProperties).toBeDefined();
    expect($mdDialog.show).toBeDefined();
    expect($mdDialog.confirm).toBeDefined();
    expect(hippoIframeCtrl).toBeDefined();
  });

  it('shows component properties dialog after rejecting the delete operation', function () {
    spyOn($mdDialog, 'show').and.returnValue($q.reject());

    hippoIframeCtrl.deleteComponent('1234');
    scope.$digest();

    expect($mdDialog.confirm).toHaveBeenCalled();
    expect($mdDialog.show).toHaveBeenCalled();
    expect(PageStructureService.showComponentProperties).toHaveBeenCalledWith('1234');
  });

  it('shows the confirmation dialog and deletes selected component', function () {
    spyOn($mdDialog, 'show').and.returnValue($q.when([]));

    hippoIframeCtrl.deleteComponent('1234');
    scope.$digest();

    expect($mdDialog.confirm).toHaveBeenCalled();
    expect($mdDialog.show).toHaveBeenCalled();
    expect(PageStructureService.removeComponent).toHaveBeenCalledWith('1234');
  });
});
