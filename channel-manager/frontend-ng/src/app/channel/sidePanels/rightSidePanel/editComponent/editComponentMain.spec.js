/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

describe('EditComponentMainCtrl', () => {
  let $log;
  let $q;
  let $scope;
  let CmsService;
  let ComponentEditor;
  let EditComponentService;
  let HippoIframeService;
  let PageStructureService;

  let $ctrl;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel.editComponent');

    inject((
      $controller,
      $rootScope,
      _$log_,
      _$q_,
      _EditComponentService_,
    ) => {
      $log = _$log_;
      $q = _$q_;
      EditComponentService = _EditComponentService_;

      CmsService = jasmine.createSpyObj('CmsService', ['publish', 'reportUsageStatistic']);
      ComponentEditor = jasmine.createSpyObj('ComponentEditor', ['close', 'confirmSaveOrDiscardChanges']);
      HippoIframeService = jasmine.createSpyObj('HippoIframeService', ['reload']);
      PageStructureService = jasmine.createSpyObj('PageStructureService', ['renderComponent']);

      $scope = $rootScope.$new();
      $ctrl = $controller('editComponentMainCtrl as $ctrl', {
        $scope,
        CmsService,
        ComponentEditor,
        EditComponentService,
        HippoIframeService,
        PageStructureService,
      });

      $scope.$digest();
    });
  });

  describe('ui-router state exit', () => {
    describe('when save or discard changes is rejected', () => {
      beforeEach(() => {
        spyOn($log, 'error');
      });

      it('fails silently when save or discard changes is canceled', (done) => {
        ComponentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.reject());

        $ctrl.uiCanExit().catch(() => {
          expect(ComponentEditor.confirmSaveOrDiscardChanges).toHaveBeenCalled();
          expect($log.error).not.toHaveBeenCalled();
          expect(ComponentEditor.close).not.toHaveBeenCalled();
          done();
        });
        $scope.$digest();
      });

      it('logs an error when save or discard changes throws an error', (done) => {
        const error = new Error('test-error');
        ComponentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.reject(error));

        $ctrl.uiCanExit().catch(() => {
          expect($log.error).toHaveBeenCalledWith('An error occurred while closing the ComponentEditor ->', error);
          expect(ComponentEditor.close).not.toHaveBeenCalled();
          done();
        });
        $scope.$digest();
      });
    });

    describe('when save or discard changes is resolved', () => {
      beforeEach(() => {
        ComponentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.resolve());
        ComponentEditor.component = { id: 'componentId' };
      });

      it('gets a transition message key for the confirmation message', (done) => {
        spyOn(EditComponentService, 'getTransitionMessageKey').and.returnValue('translation-key');

        $ctrl.uiCanExit().then(() => {
          expect(EditComponentService.getTransitionMessageKey).toHaveBeenCalled();
          expect(ComponentEditor.confirmSaveOrDiscardChanges).toHaveBeenCalledWith('translation-key');
          done();
        });
        $scope.$digest();
      });

      it('closes the component editor ', (done) => {
        $ctrl.uiCanExit().then(() => {
          expect(ComponentEditor.close).toHaveBeenCalled();
          done();
        });
        $scope.$digest();
      });

      it('renders the component', (done) => {
        $ctrl.uiCanExit().then(() => {
          expect(PageStructureService.renderComponent).toHaveBeenCalledWith('componentId');
          done();
        });
        $scope.$digest();
      });
    });
  });
});
